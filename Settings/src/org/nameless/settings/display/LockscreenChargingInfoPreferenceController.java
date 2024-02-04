/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.settings.display;

import android.content.Context;

import com.android.settings.core.BasePreferenceController;

public class LockscreenChargingInfoPreferenceController extends BasePreferenceController {

    private final Context mContext;

    public LockscreenChargingInfoPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mContext = context;
    }

    @Override
    public int getAvailabilityStatus() {
        return mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_enable_lockscreen_charging_info)
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }
}
