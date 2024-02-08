/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.settings.gestures;

import android.content.Context;

import com.android.settings.core.BasePreferenceController;

import org.nameless.hardware.TouchGestureManager;

public class OffScreenDrawVGesturePreferenceController extends BasePreferenceController {

    public OffScreenDrawVGesturePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return TouchGestureManager.isDrawVSupported() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }
}
