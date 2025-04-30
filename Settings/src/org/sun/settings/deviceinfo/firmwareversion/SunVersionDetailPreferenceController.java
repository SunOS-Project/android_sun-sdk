/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.settings.deviceinfo.firmwareversion;

import android.content.Context;
import android.os.SystemProperties;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class SunVersionDetailPreferenceController extends BasePreferenceController {

    private static final String TAG = "SunVersionDetailCtrl";

    private static final String KEY_SUN_VERSION_PROP = "ro.sun.platform.display.version";

    public SunVersionDetailPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        return SystemProperties.get(KEY_SUN_VERSION_PROP,
                mContext.getString(R.string.unknown));
    }
}
