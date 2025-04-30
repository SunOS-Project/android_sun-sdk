/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.settings.accessibility;

import static vendor.sun.hardware.vibratorExt.Effect.KEYBOARD_PRESS;

import android.content.Context;

import com.android.settings.core.BasePreferenceController;

import org.sun.os.VibratorExtManager;

public class VibrationEffectCategoryPreferenceController extends BasePreferenceController {

    private final VibratorExtManager mVibratorExtManager;

    public VibrationEffectCategoryPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mVibratorExtManager = VibratorExtManager.getInstance();
    }

    @Override
    public int getAvailabilityStatus() {
        boolean available = false;

        available |= mVibratorExtManager.isEffectSupported(KEYBOARD_PRESS);
        available |= mVibratorExtManager.getValidHapticStyles().size() > 1;

        return available ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }
}
