/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package android.provider.settings.validators;

import static android.provider.settings.validators.SettingsValidators.ANY_STRING_VALIDATOR;
import static android.provider.settings.validators.SettingsValidators.BOOLEAN_VALIDATOR;
import static android.provider.settings.validators.SettingsValidators.NON_NEGATIVE_INTEGER_VALIDATOR;

import android.util.ArrayMap;

import java.util.Map;

import org.sun.provider.SettingsExt.Secure;

public class SecureSettingsValidatorsExt {

    public static final Map<String, Validator> VALIDATORS = new ArrayMap<>();

    static {
        VALIDATORS.put(Secure.SCREEN_OFF_UDFPS_ENABLED, BOOLEAN_VALIDATOR);
        VALIDATORS.put(Secure.QSTILE_REQUIRES_UNLOCKING, BOOLEAN_VALIDATOR);
        VALIDATORS.put(Secure.ADVANCED_REBOOT, BOOLEAN_VALIDATOR);
        VALIDATORS.put(Secure.DOZE_FOR_NOTIFICATIONS, BOOLEAN_VALIDATOR);
        VALIDATORS.put(Secure.DOZE_ON_CHARGE, BOOLEAN_VALIDATOR);
        VALIDATORS.put(Secure.TETHERING_ALLOW_VPN_UPSTREAMS, BOOLEAN_VALIDATOR);
        VALIDATORS.put(Secure.DISPLAY_COLOR_BALANCE_RED, new InclusiveIntegerRangeValidator(0, 255));
        VALIDATORS.put(Secure.DISPLAY_COLOR_BALANCE_GREEN, new InclusiveIntegerRangeValidator(0, 255));
        VALIDATORS.put(Secure.DISPLAY_COLOR_BALANCE_BLUE, new InclusiveIntegerRangeValidator(0, 255));
        VALIDATORS.put(Secure.WINDOW_IGNORE_SECURE, BOOLEAN_VALIDATOR);
        VALIDATORS.put(Secure.KEYBOX_DATA, ANY_STRING_VALIDATOR);
    }
}
