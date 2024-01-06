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
}
