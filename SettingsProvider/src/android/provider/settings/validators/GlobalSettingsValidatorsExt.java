/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package android.provider.settings.validators;

import static android.provider.settings.validators.SettingsValidators.BOOLEAN_VALIDATOR;
import static android.provider.settings.validators.SettingsValidators.NON_NEGATIVE_INTEGER_VALIDATOR;

import android.util.ArrayMap;

import java.util.Map;

import org.nameless.provider.SettingsExt.Global;
import org.nameless.view.DisplayResolutionManager;

public class GlobalSettingsValidatorsExt {

    public static final Map<String, Validator> VALIDATORS = new ArrayMap<>();

    static {
        VALIDATORS.put(Global.ALERT_SLIDER_STATE, new InclusiveIntegerRangeValidator(0, 2));
        VALIDATORS.put(Global.ALERT_SLIDER_MUTE_MEDIA, BOOLEAN_VALIDATOR);
        VALIDATORS.put(Global.ALERT_SLIDER_APPLY_FOR_HEADSET, BOOLEAN_VALIDATOR);
        VALIDATORS.put(Global.ALERT_SLIDER_VIBRATE_ON_BLUETOOTH, BOOLEAN_VALIDATOR);
        VALIDATORS.put(Global.DISPLAY_WIDTH_CUSTOM, new Validator() {
            @Override
            public boolean validate(String value) {
                return DisplayResolutionManager.isDisplayWidthStrValid(value);
            }
        });
        VALIDATORS.put(Global.LOW_POWER_REFRESH_RATE, BOOLEAN_VALIDATOR);
    }
}
