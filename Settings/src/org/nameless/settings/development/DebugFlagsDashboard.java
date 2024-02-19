/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.settings.development;

import static org.nameless.os.DebugConstants.CONSTANTS_MAP;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.os.SystemProperties;

import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import org.nameless.content.OnlineConfigManager;
import org.nameless.custom.preference.SwitchPreference;

public class DebugFlagsDashboard extends SettingsPreferenceFragment {

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.debug_manager);
    }

    @Override
    public void onResume() {
        super.onResume();

        final PreferenceScreen screen = getPreferenceScreen();
        screen.removeAll();

        final Context prefContext = getPrefContext();

        final Preference onlineConfigPref = new Preference(prefContext);
        onlineConfigPref.setTitle(R.string.force_update_online_config);
        onlineConfigPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                OnlineConfigManager.sendUpdateBroadcast(prefContext);
                return true;
            }
        });
        screen.addPreference(onlineConfigPref);

        for (String key : CONSTANTS_MAP.keySet()) {
            final SwitchPreference pref = new SwitchPreference(prefContext);
            pref.setTitle(key);
            pref.setSummary(CONSTANTS_MAP.get(key));
            pref.setChecked(SystemProperties.getBoolean(CONSTANTS_MAP.get(key), false));
            pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (pref.getSummary() != null) {
                        SystemProperties.set(pref.getSummary().toString(), (Boolean) newValue ? "true" : "false");
                    }
                    return true;
                }
            });
            screen.addPreference(pref);
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.PAGE_UNKNOWN;
    }
}
