/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package android.provider.settings.validators;

import static android.provider.settings.validators.SettingsValidators.BOOLEAN_VALIDATOR;
import static android.provider.settings.validators.SettingsValidators.NON_NEGATIVE_INTEGER_VALIDATOR;

import android.text.TextUtils;
import android.util.ArrayMap;

import com.android.internal.util.nameless.DozeHelper;

import java.util.Map;
import java.util.regex.Pattern;

import org.nameless.provider.SettingsExt.System;

public class SystemSettingsValidatorsExt {

    public static final Map<String, Validator> VALIDATORS = new ArrayMap<>();

    static {
        VALIDATORS.put(System.CUSTOM_HAPTIC_ON_BACK_GESTURE, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.CUSTOM_HAPTIC_ON_FACE, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.CUSTOM_HAPTIC_ON_FINGERPRINT, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.CUSTOM_HAPTIC_ON_GESTURE_OFF_SCREEN, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.CUSTOM_HAPTIC_ON_QS_TILE, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.CUSTOM_HAPTIC_ON_SLIDER, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.CUSTOM_HAPTIC_ON_SWITCH, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.CUSTOM_HAPTIC_ON_MISC_SCENES, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.IME_KEYBOARD_PRESS_EFFECT, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.FORCE_ENABLE_IME_HAPTIC, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.LOW_POWER_DISABLE_VIBRATION, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.VIBRATION_PATTERN_NOTIFICATION, NON_NEGATIVE_INTEGER_VALIDATOR);
        VALIDATORS.put(System.VIBRATION_PATTERN_RINGTONE, NON_NEGATIVE_INTEGER_VALIDATOR);
        VALIDATORS.put(System.REFRESH_RATE_CONFIG_CUSTOM, new PerAppConfigValidator());
        VALIDATORS.put(System.EXTREME_REFRESH_RATE, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.VIBRATE_ON_CONNECT, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.VIBRATE_ON_DISCONNECT, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.IRIS_VIDEO_COLOR_BOOST, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.IRIS_MEMC_ENABLED, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.OPTIMIZED_CHARGE_ENABLED, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.OPTIMIZED_CHARGE_CEILING, new InclusiveIntegerRangeValidator(30, 99));
        VALIDATORS.put(System.OPTIMIZED_CHARGE_FLOOR, new InclusiveIntegerRangeValidator(10, 99));
        VALIDATORS.put(System.OPTIMIZED_CHARGE_STATUS, new InclusiveIntegerRangeValidator(0, 1));
        VALIDATORS.put(System.OPTIMIZED_CHARGE_TIME, new ScheduledTimeValidator());
        VALIDATORS.put(System.WIRELESS_CHARGING_ENABLED, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.WIRELESS_REVERSE_CHARGING_ENABLED, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.WIRELESS_REVERSE_CHARGING_SUSPENDED_STATUS, new InclusiveIntegerRangeValidator(0, 3));
        VALIDATORS.put(System.WIRELESS_REVERSE_CHARGING_MIN_LEVEL, new InclusiveIntegerRangeValidator(0, 80));
        VALIDATORS.put(System.WIRELESS_CHARGING_QUIET_MODE_ENABLED, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.WIRELESS_CHARGING_QUIET_MODE_STATUS, new InclusiveIntegerRangeValidator(0, 1));
        VALIDATORS.put(System.WIRELESS_CHARGING_QUIET_MODE_TIME, new ScheduledTimeValidator());
        VALIDATORS.put(System.CLICK_PARTIAL_SCREENSHOT, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.NETWORK_TRAFFIC_STATE, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.NETWORK_TRAFFIC_MODE, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.NETWORK_TRAFFIC_AUTOHIDE_THRESHOLD, NON_NEGATIVE_INTEGER_VALIDATOR);
        VALIDATORS.put(System.NETWORK_TRAFFIC_REFRESH_INTERVAL, NON_NEGATIVE_INTEGER_VALIDATOR);
        VALIDATORS.put(System.POCKET_JUDGE, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.SCREENSHOT_SOUND, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.THREE_FINGER_HOLD_SCREENSHOT, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.THREE_FINGER_SWIPE_SCREENSHOT, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.DOUBLE_TAP_SLEEP_LOCKSCREEN, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.DOUBLE_TAP_SLEEP_STATUSBAR, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.STATUSBAR_BRIGHTNESS_CONTROL, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.STATUSBAR_GESTURE_PORTRAIT_ONLY, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.TORCH_POWER_BUTTON_GESTURE, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.AUTO_ROTATE_CONFIG_CUSTOM, new PerAppConfigValidator());
        VALIDATORS.put(System.DOZE_PICK_UP_ACTION, new DozeActionValidator());
        VALIDATORS.put(System.EDGE_LIGHT_ENABLED, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.EDGE_LIGHT_ALWAYS_TRIGGER_ON_PULSE, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.EDGE_LIGHT_REPEAT_ANIMATION, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.EDGE_LIGHT_COLOR_MODE, new InclusiveIntegerRangeValidator(0, 3));
        VALIDATORS.put(System.EDGE_LIGHT_CUSTOM_COLOR, new NonEmptyHexColorValidator());
        VALIDATORS.put(System.STATUS_BAR_BATTERY_STYLE, new InclusiveIntegerRangeValidator(0, 3));
        VALIDATORS.put(System.STATUS_BAR_SHOW_BATTERY_PERCENT, new InclusiveIntegerRangeValidator(0, 2));
        VALIDATORS.put(System.STATUSBAR_CLOCK, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.STATUSBAR_CLOCK_STYLE, new InclusiveIntegerRangeValidator(0, 2));
        VALIDATORS.put(System.STATUSBAR_CLOCK_SECONDS, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.STATUSBAR_CLOCK_AM_PM_STYLE, new InclusiveIntegerRangeValidator(0, 2));
        VALIDATORS.put(System.STATUSBAR_CLOCK_DATE_DISPLAY, new InclusiveIntegerRangeValidator(0, 2));
        VALIDATORS.put(System.STATUSBAR_CLOCK_DATE_STYLE, new InclusiveIntegerRangeValidator(0, 2));
        VALIDATORS.put(System.STATUSBAR_CLOCK_DATE_POSITION, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.STATUSBAR_CLOCK_AUTO_HIDE, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.STATUSBAR_CLOCK_AUTO_HIDE_HDURATION, NON_NEGATIVE_INTEGER_VALIDATOR);
        VALIDATORS.put(System.STATUSBAR_CLOCK_AUTO_HIDE_SDURATION, NON_NEGATIVE_INTEGER_VALIDATOR);
        VALIDATORS.put(System.HEADS_UP_NOTIFICATION_SNOOZE, NON_NEGATIVE_INTEGER_VALIDATOR);
        VALIDATORS.put(System.HEADS_UP_TIMEOUT, NON_NEGATIVE_INTEGER_VALIDATOR);
        VALIDATORS.put(System.LESS_BORING_HEADS_UP, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.DISABLE_LANDSCAPE_HEADS_UP, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.ADAPTIVE_PLAYBACK_ENABLED, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.ADAPTIVE_PLAYBACK_TIMEOUT, NON_NEGATIVE_INTEGER_VALIDATOR);
        VALIDATORS.put(System.VOLBTN_MUSIC_CONTROLS, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.POP_UP_KEEP_MUTE_IN_MINI, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.POP_UP_SINGLE_TAP_ACTION, new InclusiveIntegerRangeValidator(0, 2));
        VALIDATORS.put(System.POP_UP_DOUBLE_TAP_ACTION, new InclusiveIntegerRangeValidator(0, 2));
        VALIDATORS.put(System.POP_UP_HOOK_MI_FREEFORM, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.POP_UP_NOTIFICATION_JUMP_PORTRAIT, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.POP_UP_NOTIFICATION_JUMP_LANDSCAPE, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.POP_UP_SETTINGS_JUMP, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.POP_UP_SHARE_JUMP, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.SILENT_NOTIFICATION_SCREEN_ON, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.GESTURE_NAVBAR_LENGTH_MODE, new InclusiveIntegerRangeValidator(0, 3));
        VALIDATORS.put(System.GESTURE_NAVBAR_RADIUS_MODE, new InclusiveIntegerRangeValidator(0, 2));
        VALIDATORS.put(System.GESTURE_NAVBAR_IME_SPACE, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.GESTURE_NAVBAR_IMMERSIVE, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.NAVBAR_INVERSE_LAYOUT, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.LOCKSCREEN_BATTERY_INFO, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.VOLUME_PANEL_POSITION_PORT, new InclusiveIntegerRangeValidator(0, 1));
        VALIDATORS.put(System.VOLUME_PANEL_POSITION_LAND, new InclusiveIntegerRangeValidator(0, 1));
        VALIDATORS.put(System.VOLUME_PANEL_SHOW_APP_VOLUME, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.PERSISTED_APP_VOLUME_DATA, new AppVolumeDataValidator());
        VALIDATORS.put(System.BACK_GESTURE_ARROW, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.BACK_GESTURE_HEIGHT, new InclusiveIntegerRangeValidator(0, 3));
        VALIDATORS.put(System.LONG_BACK_SWIPE_THRESHOLD_PORT, new InclusiveFloatRangeValidator(0.1f, 0.9f));
        VALIDATORS.put(System.LONG_BACK_SWIPE_THRESHOLD_LAND, new InclusiveFloatRangeValidator(0.1f, 0.9f));
        VALIDATORS.put(System.QS_SHOW_BRIGHTNESS_SLIDER, new InclusiveIntegerRangeValidator(0, 2));
        VALIDATORS.put(System.QS_BRIGHTNESS_SLIDER_POSITION, new InclusiveIntegerRangeValidator(0, 1));
        VALIDATORS.put(System.QS_SHOW_AUTO_BRIGHTNESS, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.QS_TILE_LABEL_HIDE, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.QS_TILE_VERTICAL_LAYOUT, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.QS_LAYOUT_CUSTOM, NON_NEGATIVE_INTEGER_VALIDATOR);
        VALIDATORS.put(System.QQS_LAYOUT_CUSTOM, NON_NEGATIVE_INTEGER_VALIDATOR);
        VALIDATORS.put(System.TOUCH_GESTURE_SINGLE_TAP_SHOW_AMBIENT, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.TOUCH_GESTURE_MUSIC_CONTROL, BOOLEAN_VALIDATOR);
        VALIDATORS.put(System.TOUCH_GESTURE_M, NON_NEGATIVE_INTEGER_VALIDATOR);
        VALIDATORS.put(System.TOUCH_GESTURE_O, NON_NEGATIVE_INTEGER_VALIDATOR);
        VALIDATORS.put(System.TOUCH_GESTURE_S, NON_NEGATIVE_INTEGER_VALIDATOR);
        VALIDATORS.put(System.TOUCH_GESTURE_V, NON_NEGATIVE_INTEGER_VALIDATOR);
        VALIDATORS.put(System.TOUCH_GESTURE_W, NON_NEGATIVE_INTEGER_VALIDATOR);
        VALIDATORS.put(System.SHAKE_SENSORS_BLACKLIST_CONFIG, new PerAppConfigValidator());
    }

    private static class AppVolumeDataValidator implements Validator {
        @Override
        public boolean validate(String value) {
            if (TextUtils.isEmpty(value)) return true;
            if (!value.contains(";")) return false;
            final String[] datas = value.split(";");
            for (String data : datas) {
                final String[] info = data.split(",");
                if (info.length != 3) {
                    return false;
                }
                try {
                    final int uid = Integer.parseInt(info[1]);
                    final int volume = Integer.parseInt(info[2]);
                    if (uid < 0 || volume < 0 || volume > 100) {
                        return false;
                    }
                } catch (NumberFormatException e) {
                    return false;
                }
            }
            return true;
        }
    }

    private static class DozeActionValidator implements Validator {
        @Override
        public boolean validate(String value) {
            try {
                return DozeHelper.isDozeActionValid(Integer.parseInt(value));
            } catch (NumberFormatException e) {
                return false;
            }
        }
    }

    private static class NonEmptyHexColorValidator implements Validator {
        @Override
        public boolean validate(String value) {
            return value == null || Pattern.matches("^[#][0-9A-Fa-f]{6}|[0-9A-Fa-f]{8}", value);
        }
    }

    private static class PerAppConfigValidator implements Validator {
        @Override
        public boolean validate(String value) {
            if (TextUtils.isEmpty(value)) return true;
            if (!value.contains(";")) return false;
            final String[] configs = value.split(";");
            for (String config : configs) {
                final String[] split = config.split(",");
                if (split.length != 2) {
                    return false;
                }
            }
            return true;
        }
    }

    private static class ScheduledTimeValidator implements Validator {
        @Override
        public boolean validate(String value) {
            String[] values = value.split(",", 0);
            if (values.length != 2) return false;
            for (String str : values) {
                String[] time = str.split(":", 0);
                if (time.length != 2) return false;
                int hour, minute;
                try {
                    hour = Integer.valueOf(time[0]);
                    minute = Integer.valueOf(time[1]);
                } catch (NumberFormatException e) {
                    return false;
                }
                if (hour < 0 || hour > 23 || minute < 0 || minute > 59)
                    return false;
            }
            return true;
        }
    }
}
