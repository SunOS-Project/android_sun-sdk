/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.settings.system;

import android.content.Context;

import com.android.settings.core.BasePreferenceController;

import org.sun.view.PopUpViewManager;

public class PopUpViewSettingsController extends BasePreferenceController {

    public PopUpViewSettingsController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return PopUpViewManager.FEATURE_SUPPORTED ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }
}
