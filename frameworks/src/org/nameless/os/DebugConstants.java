/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.os;

import android.os.SystemProperties;

/** @hide */
public class DebugConstants {

    private DebugConstants() {}

    // Enable this to debug all nameless features
    private static final boolean DEBUG_GLOBAL = SystemProperties.getBoolean(
        "persist.sys.nameless.debug.global", false
    );

    // Enable this to debug PackageManagerServiceExt
    // Package: com.android.server.pm.PackageManagerServiceExt
    // Key: PackageManagerServiceExt
    public static final boolean DEBUG_PMS = DEBUG_GLOBAL || SystemProperties.getBoolean(
        "persist.sys.nameless.pm.debug", false
    );

    // Enable this to debug VibrationEffectAdapter
    // Package: org.nameless.server.vibrator.VibrationEffectAdapter
    // Key: VibrationEffectAdapter
    public static final boolean DEBUG_VIBRATION_ADAPTER = DEBUG_GLOBAL || SystemProperties.getBoolean(
        "persist.sys.nameless.vibrator.adapter.debug", false
    );
}
