/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.settings.accessibility;

import static vendor.nameless.hardware.vibratorExt.V1_0.Effect.KEYBOARD_PRESS;

import android.content.Context;

import com.android.settings.core.BasePreferenceController;

import org.nameless.os.VibratorExtManager;

public class VibrationEffectCategoryPreferenceController extends BasePreferenceController {

    private final VibratorExtManager mVibratorExtManager;

    public VibrationEffectCategoryPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mVibratorExtManager = VibratorExtManager.getInstance();
    }

    @Override
    public int getAvailabilityStatus() {
        return mVibratorExtManager.isEffectSupported(KEYBOARD_PRESS) ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }
}
