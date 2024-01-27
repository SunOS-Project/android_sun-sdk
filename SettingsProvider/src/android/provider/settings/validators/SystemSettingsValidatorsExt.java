/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package android.provider.settings.validators;

import static android.provider.settings.validators.SettingsValidators.BOOLEAN_VALIDATOR;
import static android.provider.settings.validators.SettingsValidators.NON_NEGATIVE_INTEGER_VALIDATOR;

import android.text.TextUtils;
import android.util.ArrayMap;

import java.util.Map;

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
