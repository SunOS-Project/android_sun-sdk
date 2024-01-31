/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.server.display.color;

import static org.nameless.display.DisplayFeatureManager.CUSTOM_DISPLAY_COLOR_MODE_START;

import org.nameless.display.DisplayFeatureManager;

class ColorDisplayServiceExt {

    private static class InstanceHolder {
        private static ColorDisplayServiceExt INSTANCE = new ColorDisplayServiceExt();
    }

    static ColorDisplayServiceExt getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private final DisplayFeatureManager mDisplayFeatureManager =
            DisplayFeatureManager.getInstance();

    private ColorDisplayService mColorDisplayService;

    void init(ColorDisplayService cds) {
        mColorDisplayService = cds;
    }

    boolean interceptDisplayColorModeChange(int mode) {
        if (mode >= CUSTOM_DISPLAY_COLOR_MODE_START) {
            setColorModeFeature(mode);
            return true;
        }
        return false;
    }

    void setColorModeFeature(int mode) {
        mDisplayFeatureManager.setColorMode(mode);
    }

    int getFirstCustomColorMode() {
        final int[] availableColorModes =
                mColorDisplayService.getContext().getResources().getIntArray(
                com.android.internal.R.array.config_availableColorModes);
        if (availableColorModes == null) {
            return -1;
        }
        for (int mode : availableColorModes) {
            if (mode >= CUSTOM_DISPLAY_COLOR_MODE_START) {
                return mode;
            }
        }
        return -1;
    }
}
