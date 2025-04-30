/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.server.display;

import static org.sun.provider.SettingsExt.System.DC_DIMMING_STATE;
import static org.sun.server.display.DisplayFeatureController.logD;

import static vendor.sun.hardware.displayfeature.Feature.DC_DIMMING;

import android.content.ContentResolver;
import android.os.UserHandle;
import android.provider.Settings;

import com.android.server.display.AutomaticBrightnessControllerExt;

import org.sun.display.DisplayFeatureManager;

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
        AutomaticBrightnessControllerExt.getInstance().updateBrightness();
    }
}
