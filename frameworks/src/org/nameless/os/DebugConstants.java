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

    // Enable this to debug AlertSliderController
    // Package: org.nameless.server.audio.AlertSliderController
    // Key: AlertSliderController
    public static final boolean DEBUG_AUDIO_SLIDER = DEBUG_GLOBAL || SystemProperties.getBoolean(
        "persist.sys.nameless.audio.slider.debug", false
    );

    // Enable this to debug DisplayRefreshRateController
    // Package: org.nameless.server.display.DisplayRefreshRateController
    // Key: DisplayModeDirector, DisplayRefreshRateController
    public static final boolean DEBUG_DISPLAY_RR = DEBUG_GLOBAL || SystemProperties.getBoolean(
        "persist.sys.nameless.display.rr.debug", false
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

    // Enable this to debug TopActivityRecorder
    // Package: com.android.server.wm.TopActivityRecorder
    // Key: TopActivityRecorder
    public static final boolean DEBUG_WMS_TOP_APP = DEBUG_GLOBAL || SystemProperties.getBoolean(
        "persist.sys.nameless.wm.top_app.debug", false
    );
}
