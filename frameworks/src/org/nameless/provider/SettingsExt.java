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

        /**
         * Whether the phone vibrates on call connect
         * @hide
         */
        public static final String VIBRATE_ON_CONNECT = "vibrate_on_connect";

        /**
         * Whether the phone vibrates on disconnect
         * @hide
         */
        public static final String VIBRATE_ON_DISCONNECT = "vibrate_on_disconnect";

        /**
         * Indicates the state of DC dimming:
         *   0 - Off
         *   1 - On
         * @hide
         */
        public static final String DC_DIMMING_STATE = "dc_dimming_state";

        /**
         * Whether to enable high touch sample mode
         * @hide
         */
        public static final String HIGH_TOUCH_SAMPLE_MODE = "high_touch_sample_mode";

        /**
         * Whether to unlimit screen edge touch
         * @hide
         */
        public static final String UNLIMIT_EDGE_TOUCH_MODE = "unlimit_edge_touch_mode";

        /**
         * Iris - video color boost
         * @hide
         */
        public static final String IRIS_VIDEO_COLOR_BOOST = "iris_video_color_boost";

        /**
         * Iris - video motion enhancement
         * @hide
         */
        public static final String IRIS_MEMC_ENABLED = "iris_memc_enabled";

        /**
         * Optimized charging enabled
         * @hide
         */
        public static final String OPTIMIZED_CHARGE_ENABLED = "optimized_charge_enabled";

        /**
         * Optimized charging ceiling
         * @hide
         */
        public static final String OPTIMIZED_CHARGE_CEILING = "optimized_charge_ceiling";

        /**
         * Optimized charging floor
         * @hide
         */
        public static final String OPTIMIZED_CHARGE_FLOOR = "optimized_charge_floor";

        /**
         * Optimized charging status
         * 0: Always  1: Scheduled
         * @hide
         */
        public static final String OPTIMIZED_CHARGE_STATUS = "optimized_charge_status";

        /**
         * Optimized charging scheduled time
         * @hide
         */
        public static final String OPTIMIZED_CHARGE_TIME = "optimized_charge_time";

        /**
         * Whether to enable wireless charging support
         * @hide
         */
        public static final String WIRELESS_CHARGING_ENABLED = "wireless_charging_enabled";

        /**
         * Whether to enable wireless reverse charging support
         * @hide
         */
        public static final String WIRELESS_REVERSE_CHARGING_ENABLED = "wireless_reverse_charging_enabled";

        /**
         * The status of wireless reverse charging suspended
         * 0: Unsuspended  1: Suspended for charging  2: Suspended for low power  3: Suspended for power save mode
         * @hide
         */
        public static final String WIRELESS_REVERSE_CHARGING_SUSPENDED_STATUS = "wireless_reverse_charging_suspended_status";

        /**
         * Minimum allowed level that wireless reverse charging can be enabled
         * @hide
         */
        public static final String WIRELESS_REVERSE_CHARGING_MIN_LEVEL = "wireless_reverse_charging_min_level";

        /**
         * Whether to enable wireless charging quiet mode
         * @hide
         */
        public static final String WIRELESS_CHARGING_QUIET_MODE_ENABLED = "wireless_charging_quiet_mode_enabled";

        /**
         * The status of wireless charging quiet mode
         * 0: Always  1: Scheduled
         * @hide
         */
        public static final String WIRELESS_CHARGING_QUIET_MODE_STATUS = "wireless_charging_quiet_mode_status";

        /**
         * Wireless charging scheduled quiet mode time
         * @hide
         */
        public static final String WIRELESS_CHARGING_QUIET_MODE_TIME = "wireless_charging_quiet_mode_time";
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

        /**
         * Whether to require unlocking while accessing sensitive tiles
         * 
         * @hide
         */
        public static final String QSTILE_REQUIRES_UNLOCKING = "qstile_requires_unlocking";
    }

    public static class Global {

        /**
         * Indicates the state of alert slider:
         *  0 - Top
         *  1 - Middle
         *  2 - Bottom
         *
         * @hide
         */
        public static final String ALERT_SLIDER_STATE = "alert_slider_state";

        /**
         * Whether to mute media on switched to top position
         *
         * @hide
         */
        public static final String ALERT_SLIDER_MUTE_MEDIA = "alert_slider_mute_media";

        /**
         * Whether to apply mute media action for headset devices
         *
         * @hide
         */
        public static final String ALERT_SLIDER_APPLY_FOR_HEADSET = "alert_slider_apply_for_headset";

        /**
         * Whether to switch to vibrate mode when bluetooth devcies connected
         *
         * @hide
         */
        public static final String ALERT_SLIDER_VIBRATE_ON_BLUETOOTH = "alert_slider_vibrate_on_bluetooth";

        /**
         * Display width set by custom display resolution controller
         *
         * @hide
         */
        public static final String DISPLAY_WIDTH_CUSTOM = "display_width_custom";

        /**
         * Whether refresh rate should be switched to 60Hz on power save mode.
         * @hide
         */
        public static final String LOW_POWER_REFRESH_RATE = "low_power_rr_switch";
    }
}
