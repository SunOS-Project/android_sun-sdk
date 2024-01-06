/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.server.wm;

import android.view.Surface;

import org.nameless.display.DisplayFeatureManager;

public class DisplayRotationExt {

    private static class InstanceHolder {
        private static DisplayRotationExt INSTANCE = new DisplayRotationExt();
    }

    public static DisplayRotationExt getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private final DisplayFeatureManager mDisplayFeatureManager =
            DisplayFeatureManager.getInstance();

    public void updateRotation(int rotation) {
        switch (rotation) {
            case Surface.ROTATION_0:
            case Surface.ROTATION_180:
                mDisplayFeatureManager.setDisplayOrientation(0);
                break;
            case Surface.ROTATION_90:
                mDisplayFeatureManager.setDisplayOrientation(90);
                break;
            case Surface.ROTATION_270:
                mDisplayFeatureManager.setDisplayOrientation(270);
                break;
        }
    }
}
