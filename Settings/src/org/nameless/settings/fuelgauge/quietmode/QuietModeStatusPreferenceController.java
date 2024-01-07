/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.settings.fuelgauge.quietmode;

import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.util.nameless.BatteryFeatureSettingsHelper;

import com.android.settings.core.BasePreferenceController;

import org.nameless.custom.preference.SystemSettingListPreference;

public class QuietModeStatusPreferenceController extends BasePreferenceController {

    private final Context mContext;

    private SystemSettingListPreference mPreference;

    public QuietModeStatusPreferenceController(Context context, String key) {
        super(context, key);
        mContext = context;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public final void updateState(Preference preference) {
        mPreference.setVisible(BatteryFeatureSettingsHelper.getQuietModeEnabled(mContext));
        mPreference.setValue(String.valueOf(BatteryFeatureSettingsHelper.getQuietModeStatus(mContext)));
    }
}
