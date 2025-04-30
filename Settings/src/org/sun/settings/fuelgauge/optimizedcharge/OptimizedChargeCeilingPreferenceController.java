/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.settings.fuelgauge.optimizedcharge;

import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.util.sun.BatteryFeatureSettingsHelper;

import com.android.settings.core.BasePreferenceController;

import org.sun.custom.preference.SystemSettingSeekBarPreference;

public class OptimizedChargeCeilingPreferenceController extends BasePreferenceController {

    private final Context mContext;

    private SystemSettingSeekBarPreference mPreference;

    public OptimizedChargeCeilingPreferenceController(Context context, String key) {
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
        mPreference.setVisible(BatteryFeatureSettingsHelper.getOptimizedChargingEnabled(mContext));
        mPreference.setValue(BatteryFeatureSettingsHelper.getOptimizedChargingCeiling(mContext));
    }
}
