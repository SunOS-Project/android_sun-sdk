/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.custom.preference;

import static org.nameless.os.CustomVibrationAttributes.VIBRATION_ATTRIBUTES_SWITCH;

import android.content.Context;
import android.os.VibrationExtInfo;
import android.util.AttributeSet;
import android.view.View;

public class SwitchPreferenceCompat extends androidx.preference.SwitchPreferenceCompat {

    public SwitchPreferenceCompat(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public SwitchPreferenceCompat(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public SwitchPreferenceCompat(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SwitchPreferenceCompat(Context context) {
        super(context);
    }

    @Override
    protected void performClick(View view) {
        super.performClick(view);
        view.performHapticFeedbackExt(new VibrationExtInfo.Builder()
                .setEffectId(VibrationExtInfo.SWITCH_TOGGLE)
                .setFallbackEffectId(VibrationExtInfo.CLICK)
                .setVibrationAttributes(VIBRATION_ATTRIBUTES_SWITCH)
                .build()
        );
    }
}
