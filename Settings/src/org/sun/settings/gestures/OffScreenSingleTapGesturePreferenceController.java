/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.settings.gestures;

import android.content.Context;

import com.android.settings.core.BasePreferenceController;

import org.sun.hardware.TouchGestureManager;

public class OffScreenSingleTapGesturePreferenceController extends BasePreferenceController {

    public OffScreenSingleTapGesturePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return TouchGestureManager.isSingleTapSupported() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }
}
