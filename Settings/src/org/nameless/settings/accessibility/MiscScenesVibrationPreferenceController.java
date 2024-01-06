/*
 * Copyright (C) 2022 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.settings.accessibility;

import android.content.Context;
import android.provider.Settings;

import org.nameless.os.VibratorExtManager;
import org.nameless.provider.SettingsExt;

public class MiscScenesVibrationPreferenceController extends CustomVibrationTogglePreferenceController {

    public MiscScenesVibrationPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey, new CustomVibrationPreferenceConfig(
                context, Settings.System.HAPTIC_FEEDBACK_ENABLED, SettingsExt.System.CUSTOM_HAPTIC_ON_MISC_SCENES));
    }

    @Override
    public int getAvailabilityStatus() {
        return VibratorExtManager.getInstance().isSupported() ?
                AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }
}
