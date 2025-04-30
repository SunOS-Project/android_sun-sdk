/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.settings.fuelgauge;

import static vendor.sun.hardware.battery.Feature.WIRELESS_CHARGING_QUIET_MODE;

import android.content.Context;

import com.android.settings.core.BasePreferenceController;

import org.sun.os.BatteryFeatureManager;

public class WirelessChargingQuietModePreferenceController extends BasePreferenceController {

    private final BatteryFeatureManager mBatteryFeatureManager;

    public WirelessChargingQuietModePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mBatteryFeatureManager = BatteryFeatureManager.getInstance();
    }

    @Override
    public int getAvailabilityStatus() {
        return mBatteryFeatureManager.hasFeature(WIRELESS_CHARGING_QUIET_MODE)
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }
}
