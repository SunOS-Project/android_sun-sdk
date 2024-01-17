/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.settings.accessibility;

import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings;

import org.nameless.provider.SettingsExt;

public class FaceUnlockVibrationPreferenceController extends CustomVibrationTogglePreferenceController {

    private final PackageManager mPackageManager;

    public FaceUnlockVibrationPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey, new CustomVibrationPreferenceConfig(
                context, Settings.System.VIBRATE_ON, SettingsExt.System.CUSTOM_HAPTIC_ON_FACE));
        mPackageManager = context.getPackageManager();
    }

    @Override
    public int getAvailabilityStatus() {
        return mPackageManager.hasSystemFeature(PackageManager.FEATURE_FACE)
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }
}
