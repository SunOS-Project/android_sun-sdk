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

        /**
         * Whether to take partial screenshot with volume down + power click.
         * @hide
         */
        public static final String CLICK_PARTIAL_SCREENSHOT = "click_partial_screenshot";

        /**
         * Wheter to show network traffic indicator in statusbar
         * @hide
         */
        public static final String NETWORK_TRAFFIC_STATE = "network_traffic_state";

        /**
         * What to show in the network traffic indicator
         * @hide
         */
        public static final String NETWORK_TRAFFIC_MODE = "network_traffic_mode";

        /**
         * Network traffic inactivity threshold
         * @hide
         */
        public static final String NETWORK_TRAFFIC_AUTOHIDE_THRESHOLD = "network_traffic_autohide_threshold";

        /**
         * Specify refresh duration for network traffic
         * @hide
         */
        public static final String NETWORK_TRAFFIC_REFRESH_INTERVAL = "network_traffic_refresh_interval";

        /**
         * Whether allowing pocket service to register sensors and dispatch informations.
         * @hide
         */
        public static final String POCKET_JUDGE = "pocket_judge";

        /**
         * Whether to play sound on screenshot
         * @hide
         */
        public static final String SCREENSHOT_SOUND = "screenshot_sound";

        /**
         * Whether to enable three-finger hold to partial screenshot
         * @hide
         */
        public static final String THREE_FINGER_HOLD_SCREENSHOT = "three_finger_hold_screenshot";

        /**
         * Whether to enable three-finger swipe to full screenshot
         * @hide
         */
        public static final String THREE_FINGER_SWIPE_SCREENSHOT = "three_finger_swipe_screenshot";

        /**
         * Whether to enable double tap to sleep for lockscreen
         * @hide
         */
        public static final String DOUBLE_TAP_SLEEP_LOCKSCREEN = "double_tap_sleep_lockscreen";

        /**
         * Whether to enable double tap to sleep for status bar
         * @hide
         */
        public static final String DOUBLE_TAP_SLEEP_STATUSBAR = "double_tap_sleep_statusbar";

        /**
         * Whether to enable status bar brightness control
         * @hide
         */
        public static final String STATUSBAR_BRIGHTNESS_CONTROL = "statusbar_brightness_control";

        /**
         * Whether to enable status bar gestures only in portrait mode
         * @hide
         */
        public static final String STATUSBAR_GESTURE_PORTRAIT_ONLY = "statusbar_gesture_portrait_only";

        /**
         * Whether to enable long press power button gesture to toggle torch when screen off.
         * @hide
         */
        public static final String TORCH_POWER_BUTTON_GESTURE = "torch_power_button_gesture";

        /**
         * Per-app auto-rotate config
         * @hide
         */
        public static final String AUTO_ROTATE_CONFIG_CUSTOM = "auto_rotate_config_custom";

        /**
         * What to do when device is picked up
         * 0: Do nothing  1: Show ambient display  2: Show lockscreen
         * @hide
         */
        public static final String DOZE_PICK_UP_ACTION = "doze_pick_up_action";

        /**
         * Whether edge light is enabled.
         * Default 0
         * @hide
         */
        public static final String EDGE_LIGHT_ENABLED = "edge_light_enabled";

        /**
         * Whether to show edge light for all pulse events and not just for notifications.
         * Default 0
         * @hide
         */
        public static final String EDGE_LIGHT_ALWAYS_TRIGGER_ON_PULSE = "edge_light_always_trigger_on_pulse";

        /**
         * Whether to repeat edge light animation until pulse timeout.
         * Default 0
         * @hide
         */
        public static final String EDGE_LIGHT_REPEAT_ANIMATION = "edge_light_repeat_animation";

        /**
         * Color mode of edge light.
         * 0: Accent
         * 1: Notification
         * 2: Wallpaper
         * 3: Custom
         * Default 0
         * @hide
         */
        public static final String EDGE_LIGHT_COLOR_MODE = "edge_light_color_mode";

        /**
         * Custom color (hex value) for edge light.
         * Default #ffffff
         * @hide
         */
        public static final String EDGE_LIGHT_CUSTOM_COLOR = "edge_light_custom_color";

        /**
         * Whether to show media album art on keyguard
         * @hide
         */
        public static final String KEYGAURD_MEDIA_ART = "keygaurd_media_art";

        /**
         * Display style of the status bar battery information
         * 0: Display the battery an icon in portrait mode
         * 1: Display the battery as a circle
         * 2: Display the battery as plain text
         * 3: Hide battery icon
         * default: 0
         * @hide
         */
        public static final String STATUS_BAR_BATTERY_STYLE = "status_bar_battery_style";

        /**
         * Status bar battery %
         * 0: Hide the battery percentage
         * 1: Display the battery percentage inside the icon
         * 2: Display the battery percentage next to the icon
         * @hide
         */
        public static final String STATUS_BAR_SHOW_BATTERY_PERCENT = "status_bar_show_battery_percent";

        /**
         * Show or hide clock
         * 0 - hide
         * 1 - show (default)
         * @hide
         */
        public static final String STATUSBAR_CLOCK = "statusbar_clock";

        /**
         * Style of clock
         * 0 - Left Clock (default)
         * 1 - Center Clock
         * 2 - Right Clock
         * @hide
         */
        public static final String STATUSBAR_CLOCK_STYLE = "statusbar_clock_style";

        /**
         * Whether to show seconds next to clock in status bar
         * 0 - hide (default)
         * 1 - show
         * @hide
         */
        public static final String STATUSBAR_CLOCK_SECONDS = "statusbar_clock_seconds";

        /**
         * AM/PM Style for clock options
         * 0 - Normal AM/PM
         * 1 - Small AM/PM
         * 2 - No AM/PM (default)
         * @hide
         */
        public static final String STATUSBAR_CLOCK_AM_PM_STYLE = "statusbar_clock_am_pm_style";

        /**
         * Shows custom date before clock time
         * 0 - No Date
         * 1 - Small Date
         * 2 - Normal Date
         * @hide
         */
        public static final String STATUSBAR_CLOCK_DATE_DISPLAY = "statusbar_clock_date_display";

        /**
         * Sets the date string style
         * 0 - Regular style
         * 1 - Lowercase
         * 2 - Uppercase
         * @hide
         */
        public static final String STATUSBAR_CLOCK_DATE_STYLE = "statusbar_clock_date_style";

        /**
         * Stores the java DateFormat string for the date
         * @hide
         */
        public static final String STATUSBAR_CLOCK_DATE_FORMAT = "statusbar_clock_date_format";

        /**
         * Position of date
         * 0 - Left of clock
         * 1 - Right of clock
         * @hide
         */
        public static final String STATUSBAR_CLOCK_DATE_POSITION = "statusbar_clock_date_position";

        /**
         * Whether to auto hide clock
         * @hide
         */
        public static final String STATUSBAR_CLOCK_AUTO_HIDE = "statusbar_clock_auto_hide";

        /**
         * Auto hide clock hours duration
         * @hide
         */
        public static final String STATUSBAR_CLOCK_AUTO_HIDE_HDURATION = "statusbar_clock_auto_hide_hduration";

        /**
         * Auto hide clock seconds duration
         * @hide
         */
        public static final String STATUSBAR_CLOCK_AUTO_HIDE_SDURATION = "statusbar_clock_auto_hide_sduration";

        /**
         * Which applications to disable heads up notifications for
         *
         * @hide
         */
        public static final String HEADS_UP_BLACKLIST = "heads_up_blacklist";

        /**
         * Applications list where heasdup should't show
         *
         * @hide
         */
        public static final String HEADS_UP_STOPLIST = "heads_up_stoplist";

        /**
         * Defines the global heads up notification snooze
         * @hide
         */
        public static final String HEADS_UP_NOTIFICATION_SNOOZE = "heads_up_notification_snooze";

        /**
         * Whether to enable status and navigation bar color in battery saver mode.
         * Heads up timeout configuration
         * @hide
         */
        public static final String HEADS_UP_TIMEOUT = "heads_up_timeout";

        /**
         * Whether to show heads up only for dialer and sms apps
         * @hide
         */
        public static final String LESS_BORING_HEADS_UP = "less_boring_heads_up";

        /**
         * Whether to disable heads up in landscape mode
         * @hide
         */
        public static final String DISABLE_LANDSCAPE_HEADS_UP = "disable_landscape_heads_up";

        /**
         * Automatically pause media when the volume is muted and resume automatically when volume is restored.
         * 0 = disabled
         * 1 = enabled
         * @hide
         */
        public static final String ADAPTIVE_PLAYBACK_ENABLED = "adaptive_playback_enabled";

        /**
         * Adaptive playback's timeout in ms
         * @hide
         */
        public static final String ADAPTIVE_PLAYBACK_TIMEOUT = "adaptive_playback_timeout";

        /**
         * Whether or not volume button music controls should be enabled to seek media tracks
         * @hide
         */
        public static final String VOLBTN_MUSIC_CONTROLS = "volbtn_music_controls";

        /**
         * Whether to keep mute in mini-window
         * @hide
         */
        public static final String POP_UP_KEEP_MUTE_IN_MINI = "pop_up_keep_mute_in_mini";

        /**
         * Pop-Up Window dimmer view single tap action
         * 0: Enter pinned-window mode   1: Exit Pop-Up View   2: No action
         * @hide
         */
        public static final String POP_UP_SINGLE_TAP_ACTION = "pop_up_single_tap_action";

        /**
         * Pop-Up Window dimmer view double tap action
         * 0: Enter pinned-window mode   1: Exit Pop-Up View   2: No action
         * @hide
         */
        public static final String POP_UP_DOUBLE_TAP_ACTION = "pop_up_double_tap_action";

        /**
         * Whether to hook Mi-Freeform broadcast
         * @hide
         */
        public static final String POP_UP_HOOK_MI_FREEFORM = "pop_up_hook_mi_freeform";

        /**
         * Whether to use Pop-Up Window for notification app jump in portrait mode
         * @hide
         */
        public static final String POP_UP_NOTIFICATION_JUMP_PORTRAIT = "pop_up_notification_jump_portrait";

        /**
         * Whether to use Pop-Up Window for notification app jump in landscape mode
         * @hide
         */
        public static final String POP_UP_NOTIFICATION_JUMP_LANDSCAPE = "pop_up_notification_jump_landscape";

        /**
         * Blacklist apps that disallow from opening notification in Pop-Up View
         * @hide
         */
        public static final String POP_UP_NOTIFICATION_BLACKLIST = "pop_up_notification_blacklist";

        /**
         * Whether to use Pop-Up Window for Settings jump
         * @hide
         */
        public static final String POP_UP_SETTINGS_JUMP = "pop_up_settings_jump";

        /**
         * Whether to use Pop-Up Window for share activity jump
         * @hide
         */
        public static final String POP_UP_SHARE_JUMP = "pop_up_share_jump";

        /**
         * Whether to disable notification sound & vibration while the screen is on
         * @hide
         */
        public static final String SILENT_NOTIFICATION_SCREEN_ON = "silent_notification_screen_on";

        /**
         * Gesture navbar length mode.
         * 0: hidden  1: normal  2: medium  3: long
         * Default 1.
         * @hide
         */
        public static final String GESTURE_NAVBAR_LENGTH_MODE = "gesture_navbar_length_mode";

        /**
         * Size of gesture bar radius.
         * @hide
         */
        public static final String GESTURE_NAVBAR_RADIUS_MODE = "gesture_navbar_radius_mode";

        /**
         * Whether to show IME bottom space.
         * @hide
         */
        public static final String GESTURE_NAVBAR_IME_SPACE = "gesture_navbar_ime_space";

        /**
         * Whether to enable immersive mode for navbar.
         * @hide
         */
        public static final String GESTURE_NAVBAR_IMMERSIVE = "gesture_navbar_immersive";

        /**
         * Whether to inverse navbar layout on button navigation mode
         * @hide
         */
        public static final String NAVBAR_INVERSE_LAYOUT = "navbar_inverse_layout";

        /**
         * Whether to enable our pure black theme
         * @hide
         */
        public static final String PURE_BLACK_THEME = "pure_black_theme";

        /**
         * Whether to show the battery info on the lockscreen while charging
         * @hide
         */
        public static final String LOCKSCREEN_BATTERY_INFO = "lockscreen_battery_info";

        /**
         * Volume panel position in portrait mode
         * 0: Left  1: Right  Default: 0
         * @hide
         */
        public static final String VOLUME_PANEL_POSITION_PORT = "volume_panel_position_port";

        /**
         * Volume dialog position in landscape mode
         * 0: Left  1: Right  Default: 1
         * @hide
         */
        public static final String VOLUME_PANEL_POSITION_LAND = "volume_panel_position_land";

        /**
         * Whether to show app volume rows in volume panel
         * @hide
         */
        public static final String VOLUME_PANEL_SHOW_APP_VOLUME = "volume_panel_show_app_volume";

        /**
         * Persisted app volume data
         * @hide
         */
        public static final String PERSISTED_APP_VOLUME_DATA = "persisted_app_volume_data";
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

        /**
         * Whether to enable advanced reboot for power menu
         * 
         * @hide
         */
        public static final String ADVANCED_REBOOT = "advanced_reboot";

        /**
         * Whether to trigger doze for new notifications
         * @hide
         */
        public static final String DOZE_FOR_NOTIFICATIONS = "doze_for_notifications";

        /**
         * Whether to enable doze only when charging
         * @hide
         */
        public static final String DOZE_ON_CHARGE = "doze_on_charge";

        /**
         * Whether tethering is allowed to use VPN upstreams
         * @hide
         */
        public static final String TETHERING_ALLOW_VPN_UPSTREAMS = "tethering_allow_vpn_upstreams";

        /**
         * Display color balance for the red channel, from 0 to 255.
         * @hide
         */
        public static final String DISPLAY_COLOR_BALANCE_RED = "display_color_balance_red";

        /**
         * Display color balance for the green channel, from 0 to 255.
         * @hide
         */
        public static final String DISPLAY_COLOR_BALANCE_GREEN = "display_color_balance_green";

        /**
         * Display color balance for the blue channel, from 0 to 255.
         * @hide
         */
        public static final String DISPLAY_COLOR_BALANCE_BLUE = "display_color_balance_blue";

        /**
         * Control whether FLAG_SECURE is ignored for all windows.
         * @hide
         */
        public static final String WINDOW_IGNORE_SECURE = "window_ignore_secure";
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

        /**
         * Whether to wake the display when plugging or unplugging the charger
         *
         * @hide
         */
        public static final String WAKE_WHEN_PLUGGED_OR_UNPLUGGED = "wake_when_plugged_or_unplugged";
    }
}
