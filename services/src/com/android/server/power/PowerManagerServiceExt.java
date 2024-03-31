/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.server.power;

import static org.nameless.os.CustomVibrationAttributes.VIBRATION_ATTRIBUTES_MISC_SCENES;

import android.content.Context;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;

class PowerManagerServiceExt {

    private static class InstanceHolder {
        private static final PowerManagerServiceExt INSTANCE = new PowerManagerServiceExt();
    }

    static PowerManagerServiceExt getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private static final VibrationEffect EFFECT_CLICK =
            VibrationEffect.get(VibrationEffect.EFFECT_CLICK);

    private Context mContext;
    private Vibrator mVibrator;

    void init(Context context) {
        mContext = context;
    }

    void systemReady() {
        mVibrator = mContext.getSystemService(Vibrator.class);
    }

    void onWakefulnessAwake(int reason) {
        if (mVibrator != null &&
                (reason == PowerManager.WAKE_REASON_WAKE_KEY ||
                reason == PowerManager.WAKE_REASON_TAP ||
                reason == PowerManager.WAKE_REASON_GESTURE)) {
            mVibrator.vibrate(EFFECT_CLICK, VIBRATION_ATTRIBUTES_MISC_SCENES);
        }
    }
}
