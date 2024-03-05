/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.settings.display;

import static org.nameless.provider.SettingsExt.System.EDGE_LIGHT_ALWAYS_TRIGGER_ON_PULSE;
import static org.nameless.provider.SettingsExt.System.EDGE_LIGHT_COLOR_MODE;
import static org.nameless.provider.SettingsExt.System.EDGE_LIGHT_CUSTOM_COLOR;
import static org.nameless.provider.SettingsExt.System.EDGE_LIGHT_ENABLED;
import static org.nameless.provider.SettingsExt.System.EDGE_LIGHT_REPEAT_ANIMATION;

import android.app.settings.SettingsEnums;
import android.content.ContentResolver;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Switch;

import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import com.android.settingslib.widget.OnMainSwitchChangeListener;

import org.nameless.custom.colorpicker.ColorPickerPreference;
import org.nameless.custom.preference.SystemSettingListPreference;
import org.nameless.custom.preference.SystemSettingSwitchPreference;
import org.nameless.settings.preference.SystemSettingMainSwitchPreference;

public class EdgeLightSettingsFragment extends SettingsPreferenceFragment implements
        OnMainSwitchChangeListener, OnPreferenceChangeListener {

    private static final int COLOR_MODE_CUSTOM = 2;

    private ContentResolver mResolver;

    private SystemSettingMainSwitchPreference mEnabled;
    private SystemSettingSwitchPreference mAlwaysTrigger;
    private SystemSettingSwitchPreference mRepeat;
    private SystemSettingListPreference mColorMode;
    private ColorPickerPreference mColor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.edge_light_settings);

        mResolver = getContentResolver();

        final boolean enabled = Settings.System.getIntForUser(mResolver,
                EDGE_LIGHT_ENABLED,
                0, UserHandle.USER_CURRENT) == 1;
        final boolean customColor = Settings.System.getIntForUser(mResolver,
                EDGE_LIGHT_COLOR_MODE,
                0, UserHandle.USER_CURRENT) == COLOR_MODE_CUSTOM;
        final String color = Settings.System.getStringForUser(mResolver,
                EDGE_LIGHT_CUSTOM_COLOR,
                UserHandle.USER_CURRENT);

        mEnabled = (SystemSettingMainSwitchPreference) findPreference(EDGE_LIGHT_ENABLED);
        mEnabled.addOnSwitchChangeListener(this);

        mAlwaysTrigger = (SystemSettingSwitchPreference) findPreference(EDGE_LIGHT_ALWAYS_TRIGGER_ON_PULSE);
        mAlwaysTrigger.setEnabled(enabled);

        mRepeat = (SystemSettingSwitchPreference) findPreference(EDGE_LIGHT_REPEAT_ANIMATION);
        mRepeat.setEnabled(enabled);

        mColorMode = (SystemSettingListPreference) findPreference(EDGE_LIGHT_COLOR_MODE);
        mColorMode.setEnabled(enabled);
        mColorMode.setOnPreferenceChangeListener(this);

        final String colorStr = TextUtils.isEmpty(color) ? "#ffffff" : color;
        mColor = (ColorPickerPreference) findPreference(EDGE_LIGHT_CUSTOM_COLOR);
        mColor.setDefaultValue(ColorPickerPreference.convertToColorInt("#ffffff"));
        mColor.setEnabled(enabled && customColor);
        mColor.setSummary(colorStr);
        mColor.setNewPreviewColor(ColorPickerPreference.convertToColorInt(colorStr));
        mColor.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mColorMode) {
            final int value = Integer.valueOf((String) newValue);
            final boolean enabled = Settings.System.getIntForUser(mResolver,
                    EDGE_LIGHT_ENABLED,
                    0, UserHandle.USER_CURRENT) == 1;
            mColor.setEnabled(enabled && value == COLOR_MODE_CUSTOM);
        } else if (preference == mColor) {
            final String color = ColorPickerPreference.convertToRGB(
                    Integer.valueOf(String.valueOf(newValue)));
            mColor.setSummary(color);
            Settings.System.putStringForUser(mResolver,
                    EDGE_LIGHT_CUSTOM_COLOR,
                    color, UserHandle.USER_CURRENT);
        }
        return true;
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        final boolean customColor = Settings.System.getIntForUser(mResolver,
                EDGE_LIGHT_COLOR_MODE,
                0, UserHandle.USER_CURRENT) == COLOR_MODE_CUSTOM;
        mAlwaysTrigger.setEnabled(isChecked);
        mRepeat.setEnabled(isChecked);
        mColorMode.setEnabled(isChecked);
        mColor.setEnabled(isChecked && customColor);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.PAGE_UNKNOWN;
    }
}
