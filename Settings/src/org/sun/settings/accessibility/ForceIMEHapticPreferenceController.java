/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.settings.accessibility;

import android.content.Context;
import android.provider.Settings;

import org.sun.provider.SettingsExt;

public class ForceIMEHapticPreferenceController extends CustomVibrationTogglePreferenceController {

    public ForceIMEHapticPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey, new CustomVibrationPreferenceConfig(
                context, Settings.System.VIBRATE_ON, SettingsExt.System.FORCE_ENABLE_IME_HAPTIC), 0);
    }
}
