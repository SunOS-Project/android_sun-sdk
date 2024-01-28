/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.os;

import android.os.Build;
import android.os.SystemProperties;

/** @hide */
public class DebugConstants {

    private DebugConstants() {}

    // Enable this to debug all nameless features
    private static final boolean DEBUG_GLOBAL = Build.IS_ENG || SystemProperties.getBoolean(
        "persist.sys.nameless.debug.global", false
    );

    // Enable this to debug AlertSliderController
    // Package: org.nameless.server.audio.AlertSliderController
    // Key: AlertSliderController
    public static final boolean DEBUG_AUDIO_SLIDER = DEBUG_GLOBAL || SystemProperties.getBoolean(
        "persist.sys.nameless.audio.slider.debug", false
    );

    // Enable this to debug BatteryFeatureController
    // Package: org.nameless.server.battery.BatteryFeatureController
    // Key: BatteryFeatureController
    public static final boolean DEBUG_BATTERY_FEATURE = DEBUG_GLOBAL || SystemProperties.getBoolean(
        "persist.sys.nameless.battery.feature.debug", false
    );

    // Enable this to debug DisplayFeatureController
    // Package: org.nameless.server.display.DisplayFeatureController
    // Key: DisplayFeatureController
    public static final boolean DEBUG_DISPLAY_FEATURE = DEBUG_GLOBAL || SystemProperties.getBoolean(
        "persist.sys.nameless.display.feature.debug", false
    );

    // Enable this to debug IrisService
    // CommandReceiver will also be enabled. See https://github.com/Nameless-AOSP/packages_apps_IrisService/blob/fourteen/src/org/nameless/iris/observer/CommandReceiver.kt
    // Key: Iris
    public static final boolean DEBUG_DISPLAY_IRIS = DEBUG_GLOBAL || SystemProperties.getBoolean(
        "persist.sys.nameless.display.iris.debug", false
    );

    // Enable this to debug DisplayRotationExt
    // Package: org.nameless.server.wm.DisplayRotationExt
    // Key: DisplayRotationExt
    public static final boolean DEBUG_DISPLAY_ROTATE = DEBUG_GLOBAL || SystemProperties.getBoolean(
        "persist.sys.nameless.display.rotate.debug", false
    );

    // Enable this to debug DisplayRefreshRateController
    // Package: org.nameless.server.display.DisplayRefreshRateController
    // Key: DisplayModeDirector, DisplayRefreshRateController
    public static final boolean DEBUG_DISPLAY_RR = DEBUG_GLOBAL || SystemProperties.getBoolean(
        "persist.sys.nameless.display.rr.debug", false
    );

    // Enable this to debug NrModeSwitcher
    // Key: NrModeSwitcher,OplusRadioWrapper,SimCardListenerService
    public static final boolean DEBUG_NR_MODE = DEBUG_GLOBAL || SystemProperties.getBoolean(
        "persist.sys.nameless.radio.nrmode.debug", false
    );

    // Enable this to debug PhoneWindowManagerExt
    // Package: com.android.server.policy.PhoneWindowManagerExt
    // Key: PhoneWindowManagerExt, GestureListenerBase, SystemGesture
    public static final boolean DEBUG_PHONE_WINDOW_MANAGER = DEBUG_GLOBAL || SystemProperties.getBoolean(
        "persist.sys.nameless.policy.debug", false
    );

    // Enable this to debug PackageManagerServiceExt
    // Package: com.android.server.pm.PackageManagerServiceExt
    // Key: PackageManagerServiceExt
    public static final boolean DEBUG_PMS = DEBUG_GLOBAL || SystemProperties.getBoolean(
        "persist.sys.nameless.pm.debug", false
    );

    // Enable this to debug PocketModeController
    // Package: org.nameless.server.policy.PocketModeController
    // Key: PocketModeController
    public static final boolean DEBUG_POCKET = DEBUG_GLOBAL || SystemProperties.getBoolean(
        "persist.sys.nameless.pocket.debug", false
    );

    // Enable this to debug VibrationEffectAdapter
    // Package: org.nameless.server.vibrator.VibrationEffectAdapter
    // Key: VibrationEffectAdapter
    public static final boolean DEBUG_VIBRATION_ADAPTER = DEBUG_GLOBAL || SystemProperties.getBoolean(
        "persist.sys.nameless.vibrator.adapter.debug", false
    );

    // Enable this to debug DisplayResolutionController
    // Package: org.nameless.server.wm.DisplayResolutionController
    // Key: DisplayResolutionController
    public static final boolean DEBUG_WMS_RESOLUTION = DEBUG_GLOBAL || SystemProperties.getBoolean(
        "persist.sys.nameless.wm.resolution.debug", false
    );

    // Enable this to debug TopActivityRecorder
    // Package: com.android.server.wm.TopActivityRecorder
    // Key: TopActivityRecorder
    public static final boolean DEBUG_WMS_TOP_APP = DEBUG_GLOBAL || SystemProperties.getBoolean(
        "persist.sys.nameless.wm.top_app.debug", false
    );
}
