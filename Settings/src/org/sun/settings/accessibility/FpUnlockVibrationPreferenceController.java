/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.settings.accessibility;

import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings;

import org.sun.provider.SettingsExt;

public class FpUnlockVibrationPreferenceController extends CustomVibrationTogglePreferenceController {

    private final PackageManager mPackageManager;

    public FpUnlockVibrationPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey, new CustomVibrationPreferenceConfig(
                context, Settings.System.VIBRATE_ON, SettingsExt.System.CUSTOM_HAPTIC_ON_FINGERPRINT));
        mPackageManager = context.getPackageManager();
    }

    @Override
    public int getAvailabilityStatus() {
        return mPackageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }
}
