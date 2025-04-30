/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.settings.network;

import android.content.Context;

import com.android.internal.util.sun.CustomUtils;

import com.android.settings.core.BasePreferenceController;

public class PreferredNrModePreferenceController extends BasePreferenceController {

    private static final String PKG_NRMODE = "org.sun.nrmode";

    private final Context mContext;

    public PreferredNrModePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mContext = context;
    }

    @Override
    public int getAvailabilityStatus() {
        return CustomUtils.isPackageInstalled(mContext, PKG_NRMODE) ?
                AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }
}
