/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.settings.notification;

import static android.provider.Settings.Global.HEADS_UP_NOTIFICATIONS_ENABLED;

import static org.sun.provider.SettingsExt.System.DISABLE_LANDSCAPE_HEADS_UP;
import static org.sun.provider.SettingsExt.System.HEADS_UP_BLACKLIST;
import static org.sun.provider.SettingsExt.System.HEADS_UP_NOTIFICATION_SNOOZE;
import static org.sun.provider.SettingsExt.System.HEADS_UP_STOPLIST;
import static org.sun.provider.SettingsExt.System.HEADS_UP_TIMEOUT;
import static org.sun.provider.SettingsExt.System.LESS_BORING_HEADS_UP;

import android.app.settings.SettingsEnums;
import android.os.Bundle;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import org.sun.custom.preference.SwitchPreferenceCompat;
import org.sun.settings.preference.GlobalSettingMainSwitchPreference;

public class HeadsUpSettingsFragment extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private GlobalSettingMainSwitchPreference mHeadsUpEnabled;
    private SwitchPreferenceCompat mLessBoring;
    private Preference mDisableInLandscape;
    private Preference mTimeout;
    private Preference mSnoozeTime;
    private Preference mBlacklist;
    private Preference mStoplist;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.heads_up_settings);

        mHeadsUpEnabled = (GlobalSettingMainSwitchPreference) findPreference(HEADS_UP_NOTIFICATIONS_ENABLED);
        mLessBoring = (SwitchPreferenceCompat) findPreference(LESS_BORING_HEADS_UP);
        mDisableInLandscape = findPreference(DISABLE_LANDSCAPE_HEADS_UP);
        mTimeout = findPreference(HEADS_UP_TIMEOUT);
        mSnoozeTime = findPreference(HEADS_UP_NOTIFICATION_SNOOZE);
        mBlacklist = findPreference(HEADS_UP_BLACKLIST);
        mStoplist = findPreference(HEADS_UP_STOPLIST);

        mHeadsUpEnabled.setOnPreferenceChangeListener(this);
        mLessBoring.setOnPreferenceChangeListener(this);

        setupPreferences(mHeadsUpEnabled.isChecked(), mLessBoring.isChecked());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mHeadsUpEnabled) {
            setupPreferences((Boolean) newValue, mLessBoring.isChecked());
            return true;
        }
        if (preference == mLessBoring) {
            setupPreferences(mHeadsUpEnabled.isChecked(), (Boolean) newValue);
            return true;
        }
        return false;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.PAGE_UNKNOWN;
    }

    private void setupPreferences(boolean headsUpEnabled, boolean lessBoringEnabled) {
        mLessBoring.setEnabled(headsUpEnabled);
        mDisableInLandscape.setEnabled(headsUpEnabled && !lessBoringEnabled);
        mTimeout.setEnabled(headsUpEnabled && !lessBoringEnabled);
        mSnoozeTime.setEnabled(headsUpEnabled && !lessBoringEnabled);
        mBlacklist.setEnabled(headsUpEnabled && !lessBoringEnabled);
        mStoplist.setEnabled(headsUpEnabled && !lessBoringEnabled);
    }
}
