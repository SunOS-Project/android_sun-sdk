/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.server.display;

import static org.nameless.provider.SettingsExt.System.DC_DIMMING_STATE;
import static org.nameless.server.display.DisplayFeatureController.logD;

import static vendor.nameless.hardware.displayfeature.V1_0.Feature.DC_DIMMING;

import android.content.ContentResolver;
import android.os.UserHandle;
import android.provider.Settings;

import org.nameless.display.DisplayFeatureManager;

class DcDimmingController {

    private static final String TAG = "DcDimmingController";

    private final ContentResolver mResolver;
    private final DisplayFeatureManager mDisplayFeatureManager;

    private boolean mEnabled;

    DcDimmingController(ContentResolver resolver,
            DisplayFeatureManager displayFeatureManager) {
        mResolver = resolver;
        mDisplayFeatureManager = displayFeatureManager;
    }

    void onBootCompleted() {
        logD(TAG, "onBootCompleted");
        updateSettings();
    }

    void updateSettings() {
        mEnabled = Settings.System.getIntForUser(mResolver, DC_DIMMING_STATE,
                0, UserHandle.USER_SYSTEM) == 1;
        logD(TAG, "updateSettings, mEnabled: " + mEnabled);
        setDcDimmingEnabled(mEnabled);
    }

    private void setDcDimmingEnabled(boolean enabled) {
        logD(TAG, "setDcDimmingEnabled, enabled: " + enabled);
        mDisplayFeatureManager.setFeatureEnabled(DC_DIMMING, enabled);
    }
}
