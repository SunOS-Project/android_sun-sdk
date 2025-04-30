/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.settings.display;

import static org.sun.display.DisplayFeatureManager.DC_ALIAS_ONE_PULSE;
import static org.sun.provider.SettingsExt.System.DC_DIMMING_STATE;

import static vendor.sun.hardware.displayfeature.Feature.DC_DIMMING;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
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
import org.sun.display.DisplayFeatureManager;

public class DcDimmingPreferenceController extends TogglePreferenceController
        implements LifecycleObserver, OnStart, OnStop {

    private final DisplayFeatureManager mManager;

    private SwitchPreferenceCompat mPreference;
    private SettingObserver mSettingObserver;

    public DcDimmingPreferenceController(Context context, String key) {
        super(context, key);
        mManager = DisplayFeatureManager.getInstance();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        mSettingObserver = new SettingObserver(mPreference);
    }

    @Override
    public void updateState(Preference preference) {
        if (DC_ALIAS_ONE_PULSE) {
            preference.setTitle(mContext.getString(R.string.one_pulse_dimming_title));
            preference.setSummary(mContext.getString(R.string.one_pulse_dimming_summary));
        } else {
            preference.setTitle(mContext.getString(R.string.dc_dimming_title));
            preference.setSummary(mContext.getString(R.string.dc_dimming_summary));
        }
        super.updateState(preference);
    }

    @Override
    public int getAvailabilityStatus() {
        return mManager.hasFeature(DC_DIMMING) ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isChecked() {
         return Settings.System.getIntForUser(mContext.getContentResolver(),
                DC_DIMMING_STATE, 0, UserHandle.USER_SYSTEM) == 1;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.System.putIntForUser(mContext.getContentResolver(),
                DC_DIMMING_STATE, isChecked ? 1 : 0, UserHandle.USER_SYSTEM);
    }

    @Override
    public void onStart() {
        if (mSettingObserver != null) {
            mSettingObserver.register(mContext.getContentResolver());
            mSettingObserver.onChange(false);
        }
    }

    @Override
    public void onStop() {
        if (mSettingObserver != null) {
            mSettingObserver.unregister(mContext.getContentResolver());
        }
    }

    private class SettingObserver extends ContentObserver {
        private final Uri mUri = Settings.System.getUriFor(DC_DIMMING_STATE);

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
            updateState(mPreference);
        }
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return 0;
    }
}
