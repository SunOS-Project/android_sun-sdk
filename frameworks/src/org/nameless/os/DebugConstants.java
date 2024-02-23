/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.os;

import android.os.Build;
import android.os.SystemProperties;

import java.util.LinkedHashMap;

/** @hide */
public class DebugConstants {

    private DebugConstants() {}

    public static LinkedHashMap<String, String> CONSTANTS_MAP = new LinkedHashMap<>();

    static {
        CONSTANTS_MAP.put("DEBUG_GLOBAL", "persist.sys.nameless.debug.global");
        CONSTANTS_MAP.put("DEBUG_APP_PROPS", "persist.sys.nameless.app.props.debug");
        CONSTANTS_MAP.put("DEBUG_AUDIO_SLIDER", "persist.sys.nameless.audio.slider.debug");
        CONSTANTS_MAP.put("DEBUG_BATTERY_FEATURE", "persist.sys.nameless.battery.feature.debug");
        CONSTANTS_MAP.put("DEBUG_DISPLAY_FEATURE", "persist.sys.nameless.display.feature.debug");
        CONSTANTS_MAP.put("DEBUG_DISPLAY_IRIS", "persist.sys.nameless.display.iris.debug");
        CONSTANTS_MAP.put("DEBUG_DISPLAY_ROTATE", "persist.sys.nameless.display.rotate.debug");
        CONSTANTS_MAP.put("DEBUG_DISPLAY_RR", "persist.sys.nameless.display.rr.debug");
        CONSTANTS_MAP.put("DEBUG_DOZE", "persist.sys.nameless.doze.debug");
        CONSTANTS_MAP.put("DEBUG_GAME", "persist.sys.nameless.game.debug");
        CONSTANTS_MAP.put("DEBUG_LAUNCHER", "persist.sys.nameless.launcher.debug");
        CONSTANTS_MAP.put("DEBUG_NR_MODE", "persist.sys.nameless.radio.nrmode.debug");
        CONSTANTS_MAP.put("DEBUG_PHONE_WINDOW_MANAGER", "persist.sys.nameless.policy.debug");
        CONSTANTS_MAP.put("DEBUG_PMS", "persist.sys.nameless.pm.debug");
        CONSTANTS_MAP.put("DEBUG_POCKET", "persist.sys.nameless.pocket.debug");
        CONSTANTS_MAP.put("DEBUG_POP_UP", "persist.sys.nameless.popup.debug");
        CONSTANTS_MAP.put("DEBUG_SENSOR", "persist.sys.nameless.sensor.debug");
        CONSTANTS_MAP.put("DEBUG_SYSTEM_TOOL", "persist.sys.nameless.system_tool.debug");
        CONSTANTS_MAP.put("DEBUG_TOUCH_GESTURE", "persist.sys.nameless.gesture.debug");
        CONSTANTS_MAP.put("DEBUG_VIBRATION_ADAPTER", "persist.sys.nameless.vibrator.adapter.debug");
        CONSTANTS_MAP.put("DEBUG_WMS_RESOLUTION", "persist.sys.nameless.wm.resolution.debug");
        CONSTANTS_MAP.put("DEBUG_WMS_TOP_APP", "persist.sys.nameless.wm.top_app.debug");
    }

    public static boolean shouldShowDebugManager() {
        return Build.IS_ENG || SystemProperties.getBoolean("persist.sys.nameless.debug.manager", false);
    }

    // Enable this to debug all nameless features
    private static final boolean DEBUG_GLOBAL = Build.IS_ENG || SystemProperties.getBoolean(
        "persist.sys.nameless.debug.global", false
    );

    // Enable this to debug AppPropsController
    // Package: org.nameless.server.app.AppPropsController
    // Key: AppPropsController, AppPropsManager
    public static final boolean DEBUG_APP_PROPS = DEBUG_GLOBAL || SystemProperties.getBoolean(
        "persist.sys.nameless.app.props.debug", false
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

    // Enable this to debug DisplayRotationController
    // Package: org.nameless.server.wm.DisplayRotationController
    // Key: DisplayRotationController
    public static final boolean DEBUG_DISPLAY_ROTATE = DEBUG_GLOBAL || SystemProperties.getBoolean(
        "persist.sys.nameless.display.rotate.debug", false
    );

    // Enable this to debug DisplayRefreshRateController
    // Package: org.nameless.server.display.DisplayRefreshRateController
    // Key: DisplayModeDirector, DisplayRefreshRateController
    public static final boolean DEBUG_DISPLAY_RR = DEBUG_GLOBAL || SystemProperties.getBoolean(
        "persist.sys.nameless.display.rr.debug", false
    );

    // Enable this to debug DozeController
    // Package: org.nameless.server.policy.DozeController
    // Key: DozeController
    public static final boolean DEBUG_DOZE = DEBUG_GLOBAL || SystemProperties.getBoolean(
        "persist.sys.nameless.doze.debug", false
    );

    // Enable this to debug GameModeController
    // Package: org.nameless.server.app.GameModeController
    // Key: GameModeController
    public static final boolean DEBUG_GAME = DEBUG_GLOBAL || SystemProperties.getBoolean(
        "persist.sys.nameless.game.debug", false
    );

    // Enable this to debug LaunchStateController
    // Package: org.nameless.server.pm.LaunchStateController
    // Key: LaunchStateController
    public static final boolean DEBUG_LAUNCHER = DEBUG_GLOBAL || SystemProperties.getBoolean(
        "persist.sys.nameless.launcher.debug", false
    );

    // Enable this to debug NrModeSwitcher
    // Key: NrModeSwitcher, OplusRadioWrapper, SimCardListenerService
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
    // Key: PackageManagerServiceExt, ForceFullController
    public static final boolean DEBUG_PMS = DEBUG_GLOBAL || SystemProperties.getBoolean(
        "persist.sys.nameless.pm.debug", false
    );

    // Enable this to debug PocketModeController
    // Package: org.nameless.server.policy.PocketModeController
    // Key: PocketModeController
    public static final boolean DEBUG_POCKET = DEBUG_GLOBAL || SystemProperties.getBoolean(
        "persist.sys.nameless.pocket.debug", false
    );

    // Enable this to debug Pop-Up View
    // Packages and keys are not listed cause they are too much
    // Search DEBUG_POP_UP for usage
    public static final boolean DEBUG_POP_UP = DEBUG_GLOBAL || SystemProperties.getBoolean(
        "persist.sys.nameless.popup.debug", false
    );

    // Enable this to debug SensorBlockController
    // Package: org.nameless.server.sensors.SensorBlockController
    // Key: SensorBlockController
    public static final boolean DEBUG_SENSOR = DEBUG_GLOBAL || SystemProperties.getBoolean(
        "persist.sys.nameless.sensor.debug", false
    );

    // Enable this to debug System Tool
    // Key: SystemTool
    public static final boolean DEBUG_SYSTEM_TOOL = DEBUG_GLOBAL || SystemProperties.getBoolean(
        "persist.sys.nameless.system_tool.debug", false
    );

    // Enable this to debug TouchGestureController
    // Package: org.nameless.server.policy.gesture.TouchGestureController
    // Key: TouchGestureController
    public static final boolean DEBUG_TOUCH_GESTURE = DEBUG_GLOBAL || SystemProperties.getBoolean(
        "persist.sys.nameless.gesture.debug", false
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
