/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.settings.accessibility;

import static vendor.sun.hardware.vibratorExt.Type.HAPTIC;

import android.content.Context;

public class HapticStrengthSeekBarPreferenceController extends VibrationStrengthSeekBarPreferenceController {

    public HapticStrengthSeekBarPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getVibrationType() {
        return HAPTIC;
    }
}
