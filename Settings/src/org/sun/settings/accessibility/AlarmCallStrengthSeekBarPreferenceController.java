/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.settings.accessibility;

import static vendor.sun.hardware.vibratorExt.Type.ALARM_CALL;

import android.content.Context;

public class AlarmCallStrengthSeekBarPreferenceController extends VibrationStrengthSeekBarPreferenceController {

    public AlarmCallStrengthSeekBarPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getVibrationType() {
        return ALARM_CALL;
    }
}
