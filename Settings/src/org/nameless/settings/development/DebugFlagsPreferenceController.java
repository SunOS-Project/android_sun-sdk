/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.settings.development;

import android.content.Context;

import com.android.settings.development.DevelopmentSettingsDashboardFragment;

import com.android.settingslib.development.DeveloperOptionsPreferenceController;

import org.nameless.os.DebugConstants;

public class DebugFlagsPreferenceController extends DeveloperOptionsPreferenceController {

    private static final String PREF_KEY = "debug_flags_dashboard";

    public DebugFlagsPreferenceController(Context context,
            DevelopmentSettingsDashboardFragment fragment) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return PREF_KEY;
    }

    @Override
    public boolean isAvailable() {
        return DebugConstants.shouldShowDebugManager();
    }
}
