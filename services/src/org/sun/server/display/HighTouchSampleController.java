/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.server.display;

import static org.sun.provider.SettingsExt.System.HIGH_TOUCH_SAMPLE_MODE;
import static org.sun.server.display.DisplayFeatureController.logD;

import static vendor.sun.hardware.displayfeature.Feature.HIGH_SAMPLE_TOUCH;

import android.content.ContentResolver;
import android.os.UserHandle;
import android.provider.Settings;

import org.sun.display.DisplayFeatureManager;
import org.sun.server.app.GameModeController;

class HighTouchSampleController {

    private static final String TAG = "HighTouchSampleController";

    private final ContentResolver mResolver;
    private final DisplayFeatureManager mDisplayFeatureManager;

    private boolean mEnabled;

    HighTouchSampleController(ContentResolver resolver,
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
            setHighTouchSampleEnabled(true);
        } else {
            setHighTouchSampleEnabled(mEnabled);
        }
    }

    void updateSettings() {
        mEnabled = Settings.System.getIntForUser(mResolver, HIGH_TOUCH_SAMPLE_MODE,
                0, UserHandle.USER_SYSTEM) == 1;
        logD(TAG, "updateSettings, mEnabled: " + mEnabled);
        if (GameModeController.getInstance().isInGame()) {
            logD(TAG, "Interrupted settings update due to in game mode");
            return;
        }
        setHighTouchSampleEnabled(mEnabled);
    }

    private void setHighTouchSampleEnabled(boolean enabled) {
        logD(TAG, "setHighTouchSampleEnabled, enabled: " + enabled);
        mDisplayFeatureManager.setFeatureEnabled(HIGH_SAMPLE_TOUCH, enabled);
    }
}
