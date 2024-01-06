/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package android.provider.settings.backup;

import android.compat.annotation.UnsupportedAppUsage;

import org.nameless.provider.SettingsExt.System;

public class SystemSettingsExt {

    @UnsupportedAppUsage
    public static final String[] SETTINGS_TO_BACKUP = {
        System.CUSTOM_HAPTIC_ON_BACK_GESTURE,
        System.CUSTOM_HAPTIC_ON_FACE,
        System.CUSTOM_HAPTIC_ON_FINGERPRINT,
        System.CUSTOM_HAPTIC_ON_GESTURE_OFF_SCREEN,
        System.CUSTOM_HAPTIC_ON_QS_TILE,
        System.CUSTOM_HAPTIC_ON_SLIDER,
        System.CUSTOM_HAPTIC_ON_SWITCH,
        System.CUSTOM_HAPTIC_ON_MISC_SCENES,
        System.VIBRATOR_EXT_ALARM_CALL_STRENGTH_LEVEL,
        System.VIBRATOR_EXT_HAPTIC_STRENGTH_LEVEL,
        System.VIBRATOR_EXT_NOTIFICAITON_STRENGTH_LEVEL,
        System.IME_KEYBOARD_PRESS_EFFECT,
        System.FORCE_ENABLE_IME_HAPTIC,
        System.LOW_POWER_DISABLE_VIBRATION,
        System.VIBRATION_PATTERN_NOTIFICATION,
        System.VIBRATION_PATTERN_RINGTONE,
        System.REFRESH_RATE_CONFIG_CUSTOM,
        System.EXTREME_REFRESH_RATE,
        System.VIBRATE_ON_CONNECT,
        System.VIBRATE_ON_DISCONNECT,
    };
}
