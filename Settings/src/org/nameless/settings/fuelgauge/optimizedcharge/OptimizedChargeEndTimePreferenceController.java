/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.settings.fuelgauge.optimizedcharge;

import static com.android.internal.util.nameless.BatteryFeatureSettingsHelper.OPTIMIZED_CHARGE_SCHEDULED;

import android.content.Context;
import android.text.format.DateFormat;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.util.nameless.BatteryFeatureSettingsHelper;

import com.android.settings.core.BasePreferenceController;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class OptimizedChargeEndTimePreferenceController extends BasePreferenceController {

    private final Context mContext;

    public OptimizedChargeEndTimePreferenceController(Context context, String key) {
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
        final Preference preference = screen.findPreference(getPreferenceKey());
        updateState(preference);
    }

    @Override
    public final void updateState(Preference preference) {
        preference.setVisible(BatteryFeatureSettingsHelper.getOptimizedChargingEnabled(mContext) &&
                BatteryFeatureSettingsHelper.getOptimizedChargingStatus(mContext) == OPTIMIZED_CHARGE_SCHEDULED);
        String[] times = BatteryFeatureSettingsHelper.getOptimizedChargingTime(mContext).split(",");
        String outputFormat = DateFormat.is24HourFormat(mContext) ? "HH:mm" : "hh:mm a";
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern(outputFormat);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime endTime = LocalTime.parse(times[1], formatter);
        preference.setSummary(endTime.format(outputFormatter));
    }
}
