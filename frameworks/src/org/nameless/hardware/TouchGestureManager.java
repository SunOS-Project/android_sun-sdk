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

    public static final int GESTURE_ALPHA_V = 2;
    public static final int GESTURE_LEFT_ARROW = 4;
    public static final int GESTURE_RIGHT_ARROW = 5;
    public static final int GESTURE_ALPHA_O = 6;
    public static final int GESTURE_TWO_FINGER_DOWN = 7;
    public static final int GESTURE_ALPHA_M = 12;
    public static final int GESTURE_ALPHA_W = 13;
    public static final int GESTURE_SINGLE_TAP = 16;
    public static final int GESTURE_ALPHA_S = 18;

    public static boolean isSingleTapSupported() {
        return (TOUCH_GESTURE_SUPPORT_BIT & (1 << GESTURE_SINGLE_TAP)) != 0;
    }

    public static boolean isMusicControlSupported() {
        return (TOUCH_GESTURE_SUPPORT_BIT & ((1 << GESTURE_LEFT_ARROW) |
                (1 << GESTURE_RIGHT_ARROW) | (1 << GESTURE_TWO_FINGER_DOWN))) != 0;
    }

    public static boolean isDrawMSupported() {
        return (TOUCH_GESTURE_SUPPORT_BIT & (1 << GESTURE_ALPHA_M)) != 0;
    }

    public static boolean isDrawOSupported() {
        return (TOUCH_GESTURE_SUPPORT_BIT & (1 << GESTURE_ALPHA_O)) != 0;
    }

    public static boolean isDrawSSupported() {
        return (TOUCH_GESTURE_SUPPORT_BIT & (1 << GESTURE_ALPHA_S)) != 0;
    }

    public static boolean isDrawVSupported() {
        return (TOUCH_GESTURE_SUPPORT_BIT & (1 << GESTURE_ALPHA_V)) != 0;
    }

    public static boolean isDrawWSupported() {
        return (TOUCH_GESTURE_SUPPORT_BIT & (1 << GESTURE_ALPHA_W)) != 0;
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
