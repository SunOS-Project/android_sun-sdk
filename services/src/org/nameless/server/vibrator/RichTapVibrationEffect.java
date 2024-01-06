/*
 * Copyright (C) 2023 Paranoid Android
 * Copyright (C) 2023-2024 Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.server.vibrator;

import static vendor.nameless.hardware.vibratorExt.V1_0.Effect.COMPAT_TICK;
import static vendor.nameless.hardware.vibratorExt.V1_0.Effect.DOUBLE_CLICK;
import static vendor.nameless.hardware.vibratorExt.V1_0.Effect.KEYBOARD_PRESS;

import android.os.VibrationEffect;
import android.util.Slog;

import java.util.HashMap;

import org.nameless.os.VibratorExtManager;

import vendor.nameless.hardware.vibratorExt.V1_0.LevelRange;
import vendor.nameless.hardware.vibratorExt.V1_0.RichTapEffectInfo;

public class RichTapVibrationEffect {

    private static final String TAG = "RichTapVibrationEffect";

    private static final int DEFAULT_STRENGTH = 255;

    private static final HashMap<Integer, RichTapEffectInfo> EFFECT_INFO_MAP = new HashMap<>();

    private static float sAmplitude = 1f;

    private static RichTapEffectInfo getRichTapEffectInfo(int id) {
        if (EFFECT_INFO_MAP.containsKey(id)) {
            return EFFECT_INFO_MAP.get(id);
        }
        final RichTapEffectInfo info = VibratorExtManager.getInstance().getRichTapEffectInfo(id);
        EFFECT_INFO_MAP.put(id, info);
        return info;
    }

    public static int[] getInnerEffect(int id) {
        final RichTapEffectInfo effectInfo = getRichTapEffectInfo(id);
        if (effectInfo == null || effectInfo.firstId <= 0) {
            Slog.w(TAG, "Unexpected effect id: " + id);
            return null;
        }
        switch (id) {
            case COMPAT_TICK:
                return new int[] {1, 4097, 0, 100, effectInfo.firstId, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        4097, effectInfo.sleepMs, 100, effectInfo.secondId, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        4097, effectInfo.sleepMs, 100, effectInfo.thirdId, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            case DOUBLE_CLICK:
                return new int[] {1, 4097, 0, 100, effectInfo.firstId, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        4097, effectInfo.sleepMs, 100, effectInfo.secondId, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            case KEYBOARD_PRESS:
                return new int[] {1, 4097, 0, 100, effectInfo.firstId, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        4097, effectInfo.sleepMs, 100, effectInfo.secondId, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            default:
                return new int[] {1, 4097, 0, 100, effectInfo.firstId, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        }
    }

    public static int getInnerEffectStrength(int id) {
        final RichTapEffectInfo effectInfo = getRichTapEffectInfo(id);
        if (effectInfo == null) {
            Slog.w(TAG, "Unexpected effect id: " + id + ", returning default strength " + DEFAULT_STRENGTH);
            return DEFAULT_STRENGTH;
        }
        return effectInfo.minStrength + (int) ((effectInfo.maxStrength - effectInfo.minStrength) * sAmplitude);
    }

    public static void setStrengthLevel(int level, LevelRange range) {
        if (level >= 1 && level <= range.maxLevel) {
            sAmplitude = 1f / range.maxLevel * level;
        } else {
            sAmplitude = 1f;
        }
    }
}
