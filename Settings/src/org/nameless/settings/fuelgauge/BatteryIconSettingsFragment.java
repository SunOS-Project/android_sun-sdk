/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.settings.fuelgauge;

import static org.nameless.provider.SettingsExt.System.STATUS_BAR_BATTERY_STYLE;
import static org.nameless.provider.SettingsExt.System.STATUS_BAR_SHOW_BATTERY_PERCENT;

import android.app.settings.SettingsEnums;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import org.nameless.custom.preference.SystemSettingListPreference;

public class BatteryIconSettingsFragment extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final int BATTERY_STYLE_TEXT = 2;
    private static final int BATTERY_STYLE_HIDDEN = 3;

    private SystemSettingListPreference mStyle;
    private SystemSettingListPreference mPercent;

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        addPreferencesFromResource(R.xml.battery_icon);

        final int style = Settings.System.getIntForUser(getContentResolver(),
                STATUS_BAR_BATTERY_STYLE,
                0, UserHandle.USER_CURRENT);
        final boolean isText = style == BATTERY_STYLE_TEXT;
        final boolean isHidden = style == BATTERY_STYLE_HIDDEN;

        mStyle = (SystemSettingListPreference) findPreference(STATUS_BAR_BATTERY_STYLE);
        mStyle.setOnPreferenceChangeListener(this);

        mPercent = (SystemSettingListPreference) findPreference(STATUS_BAR_SHOW_BATTERY_PERCENT);
        mPercent.setEnabled(!isText && !isHidden);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mStyle) {
            final int style = Integer.valueOf((String) newValue);
            final boolean isText = style == BATTERY_STYLE_TEXT;
            final boolean isHidden = style == BATTERY_STYLE_HIDDEN;
            mPercent.setEnabled(!isText && !isHidden);
        }
        return true;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.PAGE_UNKNOWN;
    }
}
