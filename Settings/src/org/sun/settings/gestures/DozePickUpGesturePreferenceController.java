/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.settings.gestures;

import android.content.Context;

import com.android.internal.util.sun.DozeHelper;

import com.android.settings.core.BasePreferenceController;

public class DozePickUpGesturePreferenceController extends BasePreferenceController {

    private final Context mContext;

    public DozePickUpGesturePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mContext = context;
    }

    @Override
    public int getAvailabilityStatus() {
        return DozeHelper.isPickUpSupported(mContext) ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }
}
