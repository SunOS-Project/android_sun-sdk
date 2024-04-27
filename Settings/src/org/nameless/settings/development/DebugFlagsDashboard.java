/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.settings.development;

import static org.nameless.os.DebugConstants.CONSTANTS_MAP;
import static org.nameless.provider.SettingsExt.System.LTPO_ENABLED;

import static vendor.nameless.hardware.displayfeature.V1_0.Feature.LTPO;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import org.nameless.content.OnlineConfigManager;
import org.nameless.custom.preference.SwitchPreferenceCompat;
import org.nameless.display.DisplayFeatureManager;

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

        if (DisplayFeatureManager.getInstance().hasFeature(LTPO)) {
            final SwitchPreferenceCompat ltpoSwitch = new SwitchPreferenceCompat(prefContext);
            ltpoSwitch.setTitle("LTPO");
            ltpoSwitch.setChecked(Settings.System.getIntForUser(prefContext.getContentResolver(),
                    LTPO_ENABLED, 0, UserHandle.USER_SYSTEM) == 1);
            ltpoSwitch.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Settings.System.putIntForUser(prefContext.getContentResolver(),
                            LTPO_ENABLED, (Boolean) newValue ? 1 : 0, UserHandle.USER_SYSTEM);
                    return true;
                }
            });
            screen.addPreference(ltpoSwitch);
        }

        for (String key : CONSTANTS_MAP.keySet()) {
            final SwitchPreferenceCompat pref = new SwitchPreferenceCompat(prefContext);
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
