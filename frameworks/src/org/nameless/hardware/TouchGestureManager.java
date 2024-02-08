/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.hardware;

import android.os.SystemProperties;

/** @hide */
public class TouchGestureManager {

    private TouchGestureManager() {}

    public static final int KEY_CODE_SINGLE_TAP =
            SystemProperties.getInt("persist.sys.nameless.gesture.single_tap", -1);
    public static final int KEY_CODE_LEFT_ARROW =
            SystemProperties.getInt("persist.sys.nameless.gesture.left_arrow", -1);
    public static final int KEY_CODE_RIGHT_ARROW =
            SystemProperties.getInt("persist.sys.nameless.gesture.right_arrow", -1);
    public static final int KEY_CODE_TWO_FINGERS_DOWN =
            SystemProperties.getInt("persist.sys.nameless.gesture.two_fingers_down", -1);
    public static final int KEY_CODE_M =
            SystemProperties.getInt("persist.sys.nameless.gesture.draw_m", -1);
    public static final int KEY_CODE_O =
            SystemProperties.getInt("persist.sys.nameless.gesture.draw_o", -1);
    public static final int KEY_CODE_S =
            SystemProperties.getInt("persist.sys.nameless.gesture.draw_s", -1);
    public static final int KEY_CODE_V =
            SystemProperties.getInt("persist.sys.nameless.gesture.draw_v", -1);
    public static final int KEY_CODE_W =
            SystemProperties.getInt("persist.sys.nameless.gesture.draw_w", -1);

    public static boolean isSingleTapSupported() {
        return KEY_CODE_SINGLE_TAP > 0;
    }

    public static boolean isMusicControlSupported() {
        return KEY_CODE_LEFT_ARROW > 0 && KEY_CODE_RIGHT_ARROW > 0
                && KEY_CODE_TWO_FINGERS_DOWN > 0;
    }

    public static boolean isDrawMSupported() {
        return KEY_CODE_M > 0;
    }

    public static boolean isDrawOSupported() {
        return KEY_CODE_O > 0;
    }

    public static boolean isDrawSSupported() {
        return KEY_CODE_S > 0;
    }

    public static boolean isDrawVSupported() {
        return KEY_CODE_V > 0;
    }

    public static boolean isDrawWSupported() {
        return KEY_CODE_W > 0;
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
