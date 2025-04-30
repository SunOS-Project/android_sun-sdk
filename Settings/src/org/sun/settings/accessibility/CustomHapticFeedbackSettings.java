/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.settings.accessibility;

import android.app.settings.SettingsEnums;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;

public class CustomHapticFeedbackSettings extends DashboardFragment {

    private static final String TAG = "CustomHapticFeedbackSettings";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ACCESSIBILITY_VIBRATION;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.custom_haptic_feedback;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }
}
