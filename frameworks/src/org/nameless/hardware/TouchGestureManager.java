/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.hardware;

import android.os.SystemProperties;

/** @hide */
public class TouchGestureManager {

    private TouchGestureManager() {}

    public static final int TOUCH_GESTURE_SUPPORT_BIT =
            SystemProperties.getInt("ro.nameless.feature.touch_gesture.bit", 0);

    public static final int GESTURE_START_KEY_CUSTOM = 246;

    public static final int GESTURE_DOUBLE_TAP = 1;
    public static final int GESTURE_ALPHA_V = 2;
    public static final int GESTURE_RIGHT_ARROW = 4;
    public static final int GESTURE_LEFT_ARROW = 5;
    public static final int GESTURE_ALPHA_O = 6;
    public static final int GESTURE_TWO_FINGER_DOWN = 7;
    public static final int GESTURE_ALPHA_M = 12;
    public static final int GESTURE_ALPHA_W = 13;
    public static final int GESTURE_SINGLE_TAP = 16;
    public static final int GESTURE_ALPHA_S = 18;

    public static boolean isSingleTapSupported() {
        return isGestureIdSupported(GESTURE_SINGLE_TAP);
    }

    public static boolean isMusicControlSupported() {
        return isGestureIdSupported(GESTURE_LEFT_ARROW) &&
                isGestureIdSupported(GESTURE_RIGHT_ARROW) &&
                isGestureIdSupported(GESTURE_TWO_FINGER_DOWN);
    }

    public static boolean isDrawMSupported() {
        return isGestureIdSupported(GESTURE_ALPHA_M);
    }

    public static boolean isDrawOSupported() {
        return isGestureIdSupported(GESTURE_ALPHA_O);
    }

    public static boolean isDrawSSupported() {
        return isGestureIdSupported(GESTURE_ALPHA_S);
    }

    public static boolean isDrawVSupported() {
        return isGestureIdSupported(GESTURE_ALPHA_V);
    }

    public static boolean isDrawWSupported() {
        return isGestureIdSupported(GESTURE_ALPHA_W);
    }

    public static boolean isGestureIdSupported(int id) {
        return (TOUCH_GESTURE_SUPPORT_BIT & (1 << id)) != 0;
    }

    public static boolean hasGestureSupport() {
        return isSingleTapSupported() ||
                isMusicControlSupported() ||
                isDrawMSupported() ||
                isDrawOSupported() ||
                isDrawSSupported() ||
                isDrawVSupported() ||
                isDrawWSupported();
    }
}
