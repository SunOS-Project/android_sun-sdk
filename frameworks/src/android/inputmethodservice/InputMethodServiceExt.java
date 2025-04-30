/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package android.inputmethodservice;

import android.os.SystemProperties;

/** @hide */
public class InputMethodServiceExt {

    private static final String PROP_ALLOW_BACK_GESTURE_ON_IME =
            "persist.sys.sun.keyboard.back_gesture";

    public static boolean getAllowBackGestureOnIme() {
        return SystemProperties.getBoolean(PROP_ALLOW_BACK_GESTURE_ON_IME, true);
    }

    public static void setAllowBackGestureOnIme(boolean allow) {
        SystemProperties.set(PROP_ALLOW_BACK_GESTURE_ON_IME, allow ? "true" : "false");
    }
}
