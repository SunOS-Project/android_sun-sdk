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
import org.nameless.server.app.GameModeController;

class EdgeTouchController {

    private static final String TAG = "EdgeTouchController";

    private final ContentResolver mResolver;
    private final DisplayFeatureManager mDisplayFeatureManager;

    private boolean mEnabled;

    EdgeTouchController(ContentResolver resolver,
            DisplayFeatureManager displayFeatureManager) {
        mResolver = resolver;
        mDisplayFeatureManager = displayFeatureManager;
    }

    void onBootCompleted() {
        logD(TAG, "onBootCompleted");
        updateSettings();
    }

    void onGameStateChanged(boolean inGame) {
        logD(TAG, "onGameStateChanged, inGame: " + inGame);
        if (inGame) {
            setEdgeTouchEnabled(true);
        } else {
            setEdgeTouchEnabled(mEnabled);
        }
    }

    void updateSettings() {
        mEnabled = Settings.System.getIntForUser(mResolver, UNLIMIT_EDGE_TOUCH_MODE,
                0, UserHandle.USER_SYSTEM) == 1;
        if (GameModeController.getInstance().isInGame()) {
            logD(TAG, "Interrupted settings update due to in game mode");
            return;
        }
        logD(TAG, "updateSettings, mEnabled: " + mEnabled);
        setEdgeTouchEnabled(mEnabled);
    }

    private void setEdgeTouchEnabled(boolean enabled) {
        logD(TAG, "setEdgeTouchEnabled, enabled: " + enabled);
        mDisplayFeatureManager.setFeatureEnabled(EDGE_TOUCH, enabled);
    }
}
