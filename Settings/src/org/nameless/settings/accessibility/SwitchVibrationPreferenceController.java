/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.settings.accessibility;

import android.content.Context;
import android.provider.Settings;

import org.nameless.provider.SettingsExt;

public class SwitchVibrationPreferenceController extends CustomVibrationTogglePreferenceController {

    public SwitchVibrationPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey, new CustomVibrationPreferenceConfig(
                context, Settings.System.HAPTIC_FEEDBACK_ENABLED, SettingsExt.System.CUSTOM_HAPTIC_ON_SWITCH));
    }
}
