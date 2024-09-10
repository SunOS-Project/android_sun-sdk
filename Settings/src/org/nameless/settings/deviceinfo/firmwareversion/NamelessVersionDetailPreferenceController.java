/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.settings.deviceinfo.firmwareversion;

import android.content.Context;
import android.os.SystemProperties;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class NamelessVersionDetailPreferenceController extends BasePreferenceController {

    private static final String TAG = "NamelessVersionDetailCtrl";

    private static final String KEY_NAMELESS_VERSION_PROP = "ro.nameless.platform.display.version";

    public NamelessVersionDetailPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        return SystemProperties.get(KEY_NAMELESS_VERSION_PROP,
                mContext.getString(R.string.unknown));
    }
}
