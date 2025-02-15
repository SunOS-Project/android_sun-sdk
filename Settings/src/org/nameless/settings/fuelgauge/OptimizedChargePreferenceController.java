/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.settings.fuelgauge;

import static vendor.nameless.hardware.battery.Feature.SUSPEND_CHARGING;

import android.content.Context;

import com.android.settings.core.BasePreferenceController;

import org.nameless.os.BatteryFeatureManager;

public class OptimizedChargePreferenceController extends BasePreferenceController {

    private final BatteryFeatureManager mBatteryFeatureManager;

    public OptimizedChargePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mBatteryFeatureManager = BatteryFeatureManager.getInstance();
    }

    @Override
    public int getAvailabilityStatus() {
        return mBatteryFeatureManager.hasFeature(SUSPEND_CHARGING)
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }
}
