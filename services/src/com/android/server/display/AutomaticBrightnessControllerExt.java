/*
 * Copyright (C) 2025 The Nameless-CLO Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.server.display;

public class AutomaticBrightnessControllerExt {

    private static class InstanceHolder {
        private static final AutomaticBrightnessControllerExt INSTANCE = new AutomaticBrightnessControllerExt();
    }

    public static AutomaticBrightnessControllerExt getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private Runnable mUpdateBrightnessCallback = null;

    void setUpdateBrightnessCallback(Runnable runnable) {
        mUpdateBrightnessCallback = runnable;
    }

    public void updateBrightness() {
        if (mUpdateBrightnessCallback != null) {
            mUpdateBrightnessCallback.run();
        }
    }
}
