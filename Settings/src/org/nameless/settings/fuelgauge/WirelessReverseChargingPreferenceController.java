/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.settings.fuelgauge;

import static vendor.nameless.hardware.battery.Feature.WIRELESS_CHARGING_TX;

import android.content.Context;

import com.android.settings.core.BasePreferenceController;

import org.nameless.os.BatteryFeatureManager;

public class WirelessReverseChargingPreferenceController extends BasePreferenceController {

    private final BatteryFeatureManager mBatteryFeatureManager;

    public WirelessReverseChargingPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mBatteryFeatureManager = BatteryFeatureManager.getInstance();
    }

    @Override
    public int getAvailabilityStatus() {
        return mBatteryFeatureManager.hasFeature(WIRELESS_CHARGING_TX)
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }
}
