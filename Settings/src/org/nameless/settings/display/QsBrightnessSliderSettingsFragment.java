/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.settings.display;

import static org.nameless.provider.SettingsExt.System.QS_BRIGHTNESS_SLIDER_POSITION;
import static org.nameless.provider.SettingsExt.System.QS_SHOW_AUTO_BRIGHTNESS;
import static org.nameless.provider.SettingsExt.System.QS_SHOW_BRIGHTNESS_SLIDER;

import android.app.settings.SettingsEnums;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import org.nameless.custom.preference.SystemSettingListPreference;
import org.nameless.custom.preference.SystemSettingSwitchPreference;

public class QsBrightnessSliderSettingsFragment extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private SystemSettingListPreference mWhereToShow;
    private SystemSettingSwitchPreference mAutoBrightnessIcon;
    private SystemSettingSwitchPreference mShowInBottom;

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        addPreferencesFromResource(R.xml.qs_brightness_slider);

        mWhereToShow = (SystemSettingListPreference) findPreference(QS_SHOW_BRIGHTNESS_SLIDER);
        mWhereToShow.setOnPreferenceChangeListener(this);

        final boolean show = mWhereToShow.findIndexOfValue(mWhereToShow.getValue()) > 0;

        mAutoBrightnessIcon = (SystemSettingSwitchPreference) findPreference(QS_SHOW_AUTO_BRIGHTNESS);
        mAutoBrightnessIcon.setEnabled(show);

        mShowInBottom = (SystemSettingSwitchPreference) findPreference(QS_BRIGHTNESS_SLIDER_POSITION);
        mShowInBottom.setEnabled(show);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mWhereToShow) {
            final boolean show = Integer.parseInt(newValue.toString()) > 0;
            mAutoBrightnessIcon.setEnabled(show);
            mShowInBottom.setEnabled(show);
            return true;
        }
        return false;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.PAGE_UNKNOWN;
    }
}
