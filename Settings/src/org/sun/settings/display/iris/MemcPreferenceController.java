/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.settings.display.iris;

import static android.os.PowerManager.ACTION_POWER_SAVE_MODE_CHANGED;

import static org.sun.provider.SettingsExt.System.IRIS_MEMC_ENABLED;
import static org.sun.settings.display.iris.FeaturesHolder.MEMC_FHD_SUPPORTED;
import static org.sun.settings.display.iris.FeaturesHolder.MEMC_QHD_SUPPORTED;
import static org.sun.view.DisplayResolutionManager.FHD_WIDTH;
import static org.sun.view.DisplayResolutionManager.QHD_WIDTH;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.PowerManager;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import org.sun.custom.preference.SwitchPreferenceCompat;
import org.sun.view.DisplayResolutionManager;
import org.sun.view.IDisplayResolutionListener;

public class MemcPreferenceController extends TogglePreferenceController
        implements LifecycleObserver, OnStart, OnStop {

    private final IDisplayResolutionListener.Stub mDisplayResolutionListener =
            new IDisplayResolutionListener.Stub() {
        @Override
        public void onDisplayResolutionChanged(int width, int height) {
            updateEnabledAndSummary();
        }
    };

    private final BroadcastReceiver mPowerSaveReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == ACTION_POWER_SAVE_MODE_CHANGED) {
                mPowerSaveOn = mPowerManager.isPowerSaveMode();
                updateEnabledAndSummary();
            }
        }
    };

    private final DisplayResolutionManager mDisplayResolutionManager;
    private final PowerManager mPowerManager;
    private boolean mPowerSaveOn;

    private SwitchPreferenceCompat mPreference;
    private SettingObserver mSettingObserver;

    public MemcPreferenceController(Context context, String key) {
        super(context, key);
        mDisplayResolutionManager = context.getSystemService(DisplayResolutionManager.class);
        mPowerManager = context.getSystemService(PowerManager.class);
        mPowerSaveOn = mPowerManager.isPowerSaveMode();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        mSettingObserver = new SettingObserver(mPreference);
    }

    @Override
    public int getAvailabilityStatus() {
        return MEMC_FHD_SUPPORTED || MEMC_QHD_SUPPORTED
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isChecked() {
        return Settings.System.getIntForUser(mContext.getContentResolver(), IRIS_MEMC_ENABLED,
                0, UserHandle.USER_CURRENT) == 1;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.System.putIntForUser(mContext.getContentResolver(), IRIS_MEMC_ENABLED,
                isChecked ? 1 : 0, UserHandle.USER_CURRENT);
    }

    @Override
    public void onStart() {
        if (mSettingObserver != null) {
            mSettingObserver.register(mContext.getContentResolver());
            mSettingObserver.onChange(false);
        }
        mDisplayResolutionManager.registerDisplayResolutionListener(mDisplayResolutionListener);
        mContext.registerReceiver(mPowerSaveReceiver,
                new IntentFilter(ACTION_POWER_SAVE_MODE_CHANGED));
    }

    @Override
    public void onStop() {
        mContext.unregisterReceiver(mPowerSaveReceiver);
        mDisplayResolutionManager.unregisterDisplayResolutionListener(mDisplayResolutionListener);
        if (mSettingObserver != null) {
            mSettingObserver.unregister(mContext.getContentResolver());
        }
    }

    private void updateEnabledAndSummary() {
        final int displayWidth = mDisplayResolutionManager.getDisplayResolution().x;
        final boolean available = ((displayWidth == FHD_WIDTH && MEMC_FHD_SUPPORTED) ||
                (displayWidth == QHD_WIDTH && MEMC_QHD_SUPPORTED)) &&
                !mPowerSaveOn;
        mPreference.setEnabled(available);
        mPreference.setSummary(available ?
                R.string.video_motion_enhancement_summary :
                mPowerSaveOn ? R.string.video_enhancement_battery_saver_on_summary :
                R.string.video_motion_enhancement_summary_unavailable);
    }

    private class SettingObserver extends ContentObserver {
        private final Uri mMemcEnabledUri = Settings.System.getUriFor(IRIS_MEMC_ENABLED);

        private final Preference mPreference;

        SettingObserver(Preference preference) {
            super(new Handler());
            mPreference = preference;
        }

        public void register(ContentResolver cr) {
            cr.registerContentObserver(mMemcEnabledUri, false, this);
        }

        public void unregister(ContentResolver cr) {
            cr.unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange) {
            updateEnabledAndSummary();
            updateState(mPreference);
        }
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return 0;
    }
}
