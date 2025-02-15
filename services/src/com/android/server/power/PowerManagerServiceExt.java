/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.server.power;

import static org.nameless.os.CustomVibrationAttributes.VIBRATION_ATTRIBUTES_MISC_SCENES;

import static vendor.nameless.hardware.vibratorExt.Effect.SCREEN_OFF;
import static vendor.nameless.hardware.vibratorExt.Effect.SCREEN_ON;

import android.content.Context;
import android.os.PowerManager;
import android.os.VibrationExtInfo;
import android.os.Vibrator;

class PowerManagerServiceExt {

    private static class InstanceHolder {
        private static final PowerManagerServiceExt INSTANCE = new PowerManagerServiceExt();
    }

    static PowerManagerServiceExt getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private static final String DETAIL_BOUNCER_VISIBLE = "com.android.systemui:BOUNCER_VISIBLE";

    private Context mContext;
    private Vibrator mVibrator;

    void init(Context context) {
        mContext = context;
    }

    void systemReady() {
        mVibrator = mContext.getSystemService(Vibrator.class);
    }

    void onDoze(int reason) {
        if (mVibrator == null) {
            return;
        }
        if (reason == PowerManager.GO_TO_SLEEP_REASON_TIMEOUT) {
            return;
        }
        mVibrator.vibrateExt(new VibrationExtInfo.Builder()
            .setEffectId(SCREEN_OFF)
            .setVibrationAttributes(VIBRATION_ATTRIBUTES_MISC_SCENES)
            .build());
    }

    void onWakeUp(int reason, String details) {
        if (mVibrator == null) {
            return;
        }
        if (reason != PowerManager.WAKE_REASON_WAKE_KEY &&
                reason != PowerManager.WAKE_REASON_TAP &&
                reason != PowerManager.WAKE_REASON_GESTURE) {
            return;
        }
        if (DETAIL_BOUNCER_VISIBLE.equals(details)) {
            return;
        }
        mVibrator.vibrateExt(new VibrationExtInfo.Builder()
            .setEffectId(SCREEN_ON)
            .setVibrationAttributes(VIBRATION_ATTRIBUTES_MISC_SCENES)
            .build());
    }
}
