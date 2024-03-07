/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.settings.display.iris;

import static android.os.PowerManager.ACTION_POWER_SAVE_MODE_CHANGED;

import static org.nameless.provider.SettingsExt.System.IRIS_VIDEO_COLOR_BOOST;
import static org.nameless.settings.display.iris.FeaturesHolder.SDR2HDR_SUPPORTED;

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

import org.nameless.custom.preference.SwitchPreferenceCompat;

public class VideoColorBoostPreferenceController extends TogglePreferenceController
        implements LifecycleObserver, OnStart, OnStop {

    private final BroadcastReceiver mPowerSaveReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == ACTION_POWER_SAVE_MODE_CHANGED) {
                mPowerSaveOn = mPowerManager.isPowerSaveMode();
                updateEnabledAndSummary();
            }
        }
    };

    private final PowerManager mPowerManager;
    private boolean mPowerSaveOn;

    private SwitchPreferenceCompat mPreference;
    private SettingObserver mSettingObserver;

    public VideoColorBoostPreferenceController(Context context, String key) {
        super(context, key);
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
        return SDR2HDR_SUPPORTED ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isChecked() {
        return Settings.System.getIntForUser(mContext.getContentResolver(), IRIS_VIDEO_COLOR_BOOST,
                0, UserHandle.USER_CURRENT) == 1;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.System.putIntForUser(mContext.getContentResolver(), IRIS_VIDEO_COLOR_BOOST,
                isChecked ? 1 : 0, UserHandle.USER_CURRENT);
    }

    @Override
    public void onStart() {
        if (mSettingObserver != null) {
            mSettingObserver.register(mContext.getContentResolver());
            mSettingObserver.onChange(false);
        }
        mContext.registerReceiver(mPowerSaveReceiver,
                new IntentFilter(ACTION_POWER_SAVE_MODE_CHANGED));
    }

    @Override
    public void onStop() {
        mContext.unregisterReceiver(mPowerSaveReceiver);
        if (mSettingObserver != null) {
            mSettingObserver.unregister(mContext.getContentResolver());
        }
    }

    public void updateEnabledAndSummary() {
        mPreference.setEnabled(!mPowerSaveOn);
        mPreference.setSummary(!mPowerSaveOn ?
                R.string.video_color_boost_summary :
                R.string.video_enhancement_battery_saver_on_summary);
    }

    private class SettingObserver extends ContentObserver {
        private final Uri mUri = Settings.System.getUriFor(IRIS_VIDEO_COLOR_BOOST);

        private final Preference mPreference;

        SettingObserver(Preference preference) {
            super(new Handler());
            mPreference = preference;
        }

        public void register(ContentResolver cr) {
            cr.registerContentObserver(mUri, false, this);
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
