/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.settings.gestures;

import android.content.Context;

import com.android.internal.util.nameless.CustomUtils;

import com.android.settings.core.BasePreferenceController;

public class ScreenOffUdfpsGesturePreferenceController extends BasePreferenceController {

    private final Context mContext;

    public ScreenOffUdfpsGesturePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mContext = context;
    }

    @Override
    public int getAvailabilityStatus() {
        return CustomUtils.isUdfpsAvailable(mContext) ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }
}
