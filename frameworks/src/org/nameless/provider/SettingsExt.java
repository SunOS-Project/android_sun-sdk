/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.provider;

/** @hide */
public class SettingsExt {

    public static class System {

        /**
         * Whether to do haptic feedback on back gesture
         * @hide
         */
        public static final String CUSTOM_HAPTIC_ON_BACK_GESTURE = "custom_haptic_on_back_gesture";

        /**
         * Whether to do haptic feedback on face unlocked
         * @hide
         */
        public static final String CUSTOM_HAPTIC_ON_FACE = "custom_haptic_on_face";

        /**
         * Whether to do haptic feedback on fingerprint unlocked
         * @hide
         */
        public static final String CUSTOM_HAPTIC_ON_FINGERPRINT = "custom_haptic_on_fingerprint";

        /**
         * Whether to do haptic feedback on off-screen gesture
         * @hide
         */
        public static final String CUSTOM_HAPTIC_ON_GESTURE_OFF_SCREEN = "custom_haptic_on_gesture_off_screen";

        /**
         * Whether to do haptic feedback on qs tile clicked
         * @hide
         */
        public static final String CUSTOM_HAPTIC_ON_QS_TILE = "custom_haptic_on_qs_tile";

        /**
         * Whether to do haptic feedback on slider progress changed
         * @hide
         */
        public static final String CUSTOM_HAPTIC_ON_SLIDER = "custom_haptic_on_slider";

        /**
         * Whether to do haptic feedback on switches clicked
         * @hide
         */
        public static final String CUSTOM_HAPTIC_ON_SWITCH = "custom_haptic_on_switch";

        /**
         * Whether to do feedback on more scenes
         * @hide
         */
        public static final String CUSTOM_HAPTIC_ON_MISC_SCENES = "custom_haptic_on_misc_scenes";

        /**
         * VibratorExt HAL alarm/call strength level
         * @hide
         */
        public static final String VIBRATOR_EXT_ALARM_CALL_STRENGTH_LEVEL = "vibrator_ext_alarm_call_strength_level";

        /**
         * VibratorExt HAL haptic strength level
         * @hide
         */
        public static final String VIBRATOR_EXT_HAPTIC_STRENGTH_LEVEL = "vibrator_ext_haptic_strength_level";

        /**
         * VibratorExt HAL notification strength level
         * @hide
         */
        public static final String VIBRATOR_EXT_NOTIFICAITON_STRENGTH_LEVEL = "vibrator_ext_notification_strength_level";

        /**
         * Whether to enable mechanical keyboard effect for inputmethod apps haptic feedback
         * @hide
         */
        public static final String IME_KEYBOARD_PRESS_EFFECT = "ime_keyboard_press_effect";

        /**
         * Whether to force enable inputmethod apps haptic feedback even with touch feedback disabled
         * @hide
         */
        public static final String FORCE_ENABLE_IME_HAPTIC = "force_enable_ime_haptic";

        /**
         * Whether to disable vibration in battery saver mode
         * @hide
         */
        public static final String LOW_POWER_DISABLE_VIBRATION = "low_power_disable_vibration";

        /**
         * Vibration pattern id used for notification
         * @hide
         */
        public static final String VIBRATION_PATTERN_NOTIFICATION = "vibration_pattern_notification";

        /**
         * Vibration pattern id used for ringtone
         * @hide
         */
        public static final String VIBRATION_PATTERN_RINGTONE = "vibration_pattern_ringtone";

        /**
         * Per-app refresh rate config
         * @hide
         */
        public static final String REFRESH_RATE_CONFIG_CUSTOM = "refresh_rate_config_custom";

        /**
         * Force highest refresh rate in all apps
         * @hide
         */
        public static final String EXTREME_REFRESH_RATE = "extreme_refresh_rate";
    }

    public static class Secure {

        /**
         * Whether UDFPS is active while the screen is off.
         *
         * <p>1 if true, 0 or unset otherwise.
         *
         * @hide
         */
        public static final String SCREEN_OFF_UDFPS_ENABLED = "screen_off_udfps_enabled";
    }

    public static class Global {
    }
}
