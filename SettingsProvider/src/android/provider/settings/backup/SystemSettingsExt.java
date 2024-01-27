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
        System.IRIS_VIDEO_COLOR_BOOST,
        System.IRIS_MEMC_ENABLED,
        System.CLICK_PARTIAL_SCREENSHOT,
        System.NETWORK_TRAFFIC_STATE,
        System.NETWORK_TRAFFIC_MODE,
        System.NETWORK_TRAFFIC_AUTOHIDE_THRESHOLD,
        System.NETWORK_TRAFFIC_REFRESH_INTERVAL,
        System.POCKET_JUDGE,
        System.SCREENSHOT_SOUND,
        System.THREE_FINGER_HOLD_SCREENSHOT,
        System.THREE_FINGER_SWIPE_SCREENSHOT,
        System.DOUBLE_TAP_SLEEP_LOCKSCREEN,
        System.DOUBLE_TAP_SLEEP_STATUSBAR,
        System.STATUSBAR_BRIGHTNESS_CONTROL,
        System.STATUSBAR_GESTURE_PORTRAIT_ONLY,
    };
}
