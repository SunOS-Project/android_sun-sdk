/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.settings.fuelgauge;

import static vendor.nameless.hardware.battery.Feature.WIRELESS_CHARGING_RX;

import android.content.Context;

import com.android.internal.util.nameless.BatteryFeatureSettingsHelper;

import com.android.settings.core.TogglePreferenceController;

import org.nameless.os.BatteryFeatureManager;

public class WirelessChargingEnablePreferenceController extends TogglePreferenceController {

    private final BatteryFeatureManager mBatteryFeatureManager;

    public WirelessChargingEnablePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mBatteryFeatureManager = BatteryFeatureManager.getInstance();
    }

    @Override
    public int getAvailabilityStatus() {
        return mBatteryFeatureManager.hasFeature(WIRELESS_CHARGING_RX)
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isChecked() {
        return BatteryFeatureSettingsHelper.getWirelessChargingEnabled(mContext);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        BatteryFeatureSettingsHelper.setWirelessChargingEnabled(mContext, isChecked);
        return true;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return 0;
    }
}
