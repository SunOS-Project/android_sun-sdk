/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.server.policy.gesture;

import static org.nameless.hardware.TouchGestureManager.GESTURE_DOUBLE_TAP;
import static org.nameless.hardware.TouchGestureManager.GESTURE_SINGLE_TAP;
import static org.nameless.hardware.TouchGestureManager.GESTURE_START_KEY_CUSTOM;
import static org.nameless.os.DebugConstants.DEBUG_TOUCH_GESTURE;

import static vendor.nameless.hardware.displayfeature.Command.CMD_INIT_TOUCH_GESTURE;
import static vendor.nameless.hardware.displayfeature.Command.CMD_REPORT_TOUCH_GESTURE;

import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.Slog;

import org.nameless.display.DisplayFeatureManager;
import org.nameless.hardware.TouchGestureManager;

class OplusTouchGestureHelper {

    private static final String TAG = "OplusTouchGestureHelper";

    private static final boolean TOUCH_GESTURE_OPLUS_MODE =
            SystemProperties.getBoolean("ro.nameless.feature.touch_gesture.oplus", false);

    private static final int SCAN_CODE_OPLUS = 62;

    private static final long DOUBLE_TAP_THRESHOLD = 350L;

    private static boolean sHasSingleTap = false;
    private static long sLastSingleTapTime = 0L;

    static void initGestureNode() {
        if (!TOUCH_GESTURE_OPLUS_MODE) {
            return;
        }
        if (DEBUG_TOUCH_GESTURE) {
            Slog.d(TAG, "initGestureNode");
        }
        DisplayFeatureManager.getInstance().sendCommand(CMD_INIT_TOUCH_GESTURE);
    }

    static int convertScanCode(int scanCode) {
        if (!TOUCH_GESTURE_OPLUS_MODE || scanCode != SCAN_CODE_OPLUS) {
            return scanCode;
        }

        final long current = SystemClock.uptimeMillis();
        if (sHasSingleTap && current - sLastSingleTapTime <= DOUBLE_TAP_THRESHOLD) {
            sHasSingleTap = false;
            sLastSingleTapTime = 0L;
            return GESTURE_START_KEY_CUSTOM + GESTURE_DOUBLE_TAP;
        }

        final int reportedId = DisplayFeatureManager.getInstance().sendCommand(CMD_REPORT_TOUCH_GESTURE);
        if (DEBUG_TOUCH_GESTURE) {
            Slog.d(TAG, "convertScanCode, scanCode=" + scanCode + ", reportedId=" + reportedId);
        }

        if (reportedId < 0 || !TouchGestureManager.isGestureIdSupported(reportedId)) {
            return scanCode;
        }

        if (reportedId == GESTURE_SINGLE_TAP) {
            sHasSingleTap = true;
            sLastSingleTapTime = current;
        }
        return GESTURE_START_KEY_CUSTOM + reportedId;
    }

    static boolean shouldDelayDoze() {
        return TOUCH_GESTURE_OPLUS_MODE;
    }
}
