/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.settings.accessibility;

import static vendor.sun.hardware.vibratorExt.Effect.KEYBOARD_PRESS;

import android.content.Context;

import com.android.settings.core.BasePreferenceController;

import org.sun.os.VibratorExtManager;

public class KeyboardEffectWarningPreferenceController extends BasePreferenceController {

    public KeyboardEffectWarningPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return VibratorExtManager.getInstance().isEffectSupported(KEYBOARD_PRESS)
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }
}
