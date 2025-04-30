/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.settings.sound;

import android.content.Context;

import com.android.settings.core.BasePreferenceController;

import org.sun.audio.AlertSliderManager;

public class AlertSliderCategoryPreferenceController extends BasePreferenceController {

    private final boolean mSupported;

    public AlertSliderCategoryPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mSupported = AlertSliderManager.hasAlertSlider(context);
    }

    @Override
    public int getAvailabilityStatus() {
        return mSupported ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }
}
