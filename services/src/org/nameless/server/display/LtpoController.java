/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.server.display;

import static org.nameless.provider.SettingsExt.System.LTPO_ENABLED;
import static org.nameless.server.display.DisplayFeatureController.logD;

import static vendor.nameless.hardware.displayfeature.Feature.LTPO;

import android.content.ContentResolver;
import android.os.UserHandle;
import android.provider.Settings;

import org.nameless.display.DisplayFeatureManager;
import org.nameless.server.app.GameModeController;

class LtpoController {

    private static final String TAG = "LtpoController";

    private final ContentResolver mResolver;
    private final DisplayFeatureManager mDisplayFeatureManager;

    private boolean mEnabled;

    LtpoController(ContentResolver resolver,
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
            setLtpoEnabled(false);
        } else {
            setLtpoEnabled(mEnabled);
        }
    }

    void onScreenOn() {
        logD(TAG, "onScreenOn, mEnabled: " + mEnabled);
        if (mEnabled && !GameModeController.getInstance().isInGame()) {
            setLtpoEnabled(true);
        }
    }

    void updateSettings() {
        mEnabled = Settings.System.getIntForUser(mResolver, LTPO_ENABLED,
                0, UserHandle.USER_SYSTEM) == 1;
        if (GameModeController.getInstance().isInGame()) {
            logD(TAG, "Interrupted settings update due to in game mode");
            return;
        }
        logD(TAG, "updateSettings, mEnabled: " + mEnabled);
        setLtpoEnabled(mEnabled);
    }

    private void setLtpoEnabled(boolean enabled) {
        logD(TAG, "setLtpoEnabled, enabled: " + enabled);
        mDisplayFeatureManager.setFeatureEnabled(LTPO, enabled);
    }
}
