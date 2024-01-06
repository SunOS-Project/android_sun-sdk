/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.server.display;

import static org.nameless.provider.SettingsExt.System.UNLIMIT_EDGE_TOUCH_MODE;
import static org.nameless.server.display.DisplayFeatureController.logD;

import static vendor.nameless.hardware.displayfeature.V1_0.Feature.EDGE_TOUCH;

import android.content.ContentResolver;
import android.os.UserHandle;
import android.provider.Settings;

import org.nameless.display.DisplayFeatureManager;

public class EdgeTouchController {

    private static final String TAG = "EdgeTouchController";

    private final ContentResolver mResolver;
    private final DisplayFeatureManager mDisplayFeatureManager;

    private boolean mEnabled;

    public EdgeTouchController(ContentResolver resolver,
            DisplayFeatureManager displayFeatureManager) {
        mResolver = resolver;
        mDisplayFeatureManager = displayFeatureManager;
    }

    public void onBootCompleted() {
        logD(TAG, "onBootCompleted");
        updateSettings();
    }

    public void updateSettings() {
        mEnabled = Settings.System.getIntForUser(mResolver, UNLIMIT_EDGE_TOUCH_MODE,
                0, UserHandle.USER_SYSTEM) == 1;
        logD(TAG, "updateSettings, mEnabled: " + mEnabled);
        setEdgeTouchEnabled(mEnabled);
    }

    private void setEdgeTouchEnabled(boolean enabled) {
        logD(TAG, "setEdgeTouchEnabled, enabled: " + enabled);
        mDisplayFeatureManager.setFeatureEnabled(EDGE_TOUCH, enabled);
    }
}
