/*
 * Copyright (C) 2022 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.custom.preference;

import static org.nameless.os.CustomVibrationAttributes.VIBRATION_ATTRIBUTES_SWITCH;

import android.content.Context;
import android.os.VibrationExtInfo;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.View;

public class Switch extends android.widget.Switch {

    private final Vibrator mVibrator;

    public Switch(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mVibrator = context.getSystemService(Vibrator.class);
    }

    public Switch(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public Switch(Context context, AttributeSet attrs) {
        this(context, attrs, com.android.internal.R.attr.switchStyle);
    }

    public Switch(Context context) {
        this(context, null);
    }

    @Override
    public void toggle() {
        super.toggle();
        mVibrator.vibrateExt(new VibrationExtInfo.Builder()
                .setEffectId(VibrationExtInfo.SWITCH_TOGGLE)
                .setFallbackEffectId(VibrationExtInfo.CLICK)
                .setVibrationAttributes(VIBRATION_ATTRIBUTES_SWITCH)
                .build()
        );
    }
}
