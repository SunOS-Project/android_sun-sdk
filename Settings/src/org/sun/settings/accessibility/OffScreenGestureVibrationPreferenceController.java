/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.settings.accessibility;

import android.content.Context;
import android.provider.Settings;

import org.sun.provider.SettingsExt;

public class OffScreenGestureVibrationPreferenceController extends CustomVibrationTogglePreferenceController {

    public OffScreenGestureVibrationPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey, new CustomVibrationPreferenceConfig(
                context, Settings.System.HAPTIC_FEEDBACK_ENABLED, SettingsExt.System.CUSTOM_HAPTIC_ON_GESTURE_OFF_SCREEN));
    }
}
