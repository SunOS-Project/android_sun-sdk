/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.os;

import android.os.Build;
import android.os.SystemProperties;

import java.util.LinkedHashMap;

/** @hide */
public class DebugConstants {

    private DebugConstants() {}

    public static LinkedHashMap<String, String> CONSTANTS_MAP = new LinkedHashMap<>();

    static {
        CONSTANTS_MAP.put("DEBUG_GLOBAL", "persist.sys.sun.debug.global");
        CONSTANTS_MAP.put("DEBUG_APP_PROPS", "persist.sys.sun.app.props.debug");
        CONSTANTS_MAP.put("DEBUG_AUDIO_SLIDER", "persist.sys.sun.audio.slider.debug");
        CONSTANTS_MAP.put("DEBUG_BATTERY_FEATURE", "persist.sys.sun.battery.feature.debug");
        CONSTANTS_MAP.put("DEBUG_DISPLAY_FEATURE", "persist.sys.sun.display.feature.debug");
        CONSTANTS_MAP.put("DEBUG_DISPLAY_ROTATE", "persist.sys.sun.display.rotate.debug");
        CONSTANTS_MAP.put("DEBUG_DISPLAY_RR", "persist.sys.sun.display.rr.debug");
        CONSTANTS_MAP.put("DEBUG_DOZE", "persist.sys.sun.doze.debug");
        CONSTANTS_MAP.put("DEBUG_GAME", "persist.sys.sun.game.debug");
        CONSTANTS_MAP.put("DEBUG_LAUNCHER", "persist.sys.sun.launcher.debug");
        CONSTANTS_MAP.put("DEBUG_NR_MODE", "persist.sys.sun.radio.nrmode.debug");
        CONSTANTS_MAP.put("DEBUG_OP_LM", "persist.sys.sun.vibrator.oplm.debug");
        CONSTANTS_MAP.put("DEBUG_PHONE_WINDOW_MANAGER", "persist.sys.sun.policy.debug");
        CONSTANTS_MAP.put("DEBUG_PMS", "persist.sys.sun.pm.debug");
        CONSTANTS_MAP.put("DEBUG_POCKET", "persist.sys.sun.pocket.debug");
        CONSTANTS_MAP.put("DEBUG_POP_UP", "persist.sys.sun.popup.debug");
        CONSTANTS_MAP.put("DEBUG_RICHTAP", "persist.sys.sun.richtap.debug");
        CONSTANTS_MAP.put("DEBUG_SENSOR", "persist.sys.sun.sensor.debug");
        CONSTANTS_MAP.put("DEBUG_SYSTEM_TOOL", "persist.sys.sun.system_tool.debug");
        CONSTANTS_MAP.put("DEBUG_THREE_FINGER", "persist.sys.sun.threefinger.debug");
        CONSTANTS_MAP.put("DEBUG_TICKER", "persist.sys.sun.ticker.debug");
        CONSTANTS_MAP.put("DEBUG_TOUCH_GESTURE", "persist.sys.sun.gesture.debug");
        CONSTANTS_MAP.put("DEBUG_VIBRATION_ADAPTER", "persist.sys.sun.vibrator.adapter.debug");
        CONSTANTS_MAP.put("DEBUG_WMS_RESOLUTION", "persist.sys.sun.wm.resolution.debug");
        CONSTANTS_MAP.put("DEBUG_WMS_TOP_APP", "persist.sys.sun.wm.top_app.debug");
    }

    // Enable this to debug all sun features
    private static final boolean DEBUG_GLOBAL = Build.IS_ENG || SystemProperties.getBoolean(
        "persist.sys.sun.debug.global", false
    );

    // Enable this to debug app props spoof feature
    // Key: AppPropsController, AppPropsManager
    public static final boolean DEBUG_APP_PROPS = DEBUG_GLOBAL || SystemProperties.getBoolean(
        "persist.sys.sun.app.props.debug", false
    );

    // Enable this to debug alert slider feature
    // Package: org.sun.server.audio.AlertSliderController
    // Key: AlertSliderController
    public static final boolean DEBUG_AUDIO_SLIDER = DEBUG_GLOBAL || SystemProperties.getBoolean(
        "persist.sys.sun.audio.slider.debug", false
    );

    // Enable this to debug battery HAL features
    // Package: org.sun.server.battery.BatteryFeatureController
    // Key: BatteryFeatureController
    public static final boolean DEBUG_BATTERY_FEATURE = DEBUG_GLOBAL || SystemProperties.getBoolean(
        "persist.sys.sun.battery.feature.debug", false
    );

    // Enable this to debug displayfeature HAL features
    // Package: org.sun.server.display.DisplayFeatureController
    // Key: DisplayFeatureController
    public static final boolean DEBUG_DISPLAY_FEATURE = DEBUG_GLOBAL || SystemProperties.getBoolean(
        "persist.sys.sun.display.feature.debug", false
    );

    // Enable this to debug per-app auto rotation feature
    // Package: org.sun.server.wm.DisplayRotationController
    // Key: DisplayRotationController
    public static final boolean DEBUG_DISPLAY_ROTATE = DEBUG_GLOBAL || SystemProperties.getBoolean(
        "persist.sys.sun.display.rotate.debug", false
    );

    // Enable this to debug refresh rate switch feature
    // Package: org.sun.server.display.DisplayRefreshRateController
    // Key: DisplayModeDirector, DisplayRefreshRateController, DisplayRefreshRateHelper
    public static final boolean DEBUG_DISPLAY_RR = DEBUG_GLOBAL || SystemProperties.getBoolean(
        "persist.sys.sun.display.rr.debug", false
    );

    // Enable this to debug doze action feature
    // Package: org.sun.server.policy.DozeController
    // Key: DozeController
    public static final boolean DEBUG_DOZE = DEBUG_GLOBAL || SystemProperties.getBoolean(
        "persist.sys.sun.doze.debug", false
    );

    // Enable this to debug game mode feature
    // Package: org.sun.server.app.GameModeController
    // Key: GameModeController
    public static final boolean DEBUG_GAME = DEBUG_GLOBAL || SystemProperties.getBoolean(
        "persist.sys.sun.game.debug", false
    );

    // Enable this to debug launcher switcher feature
    // Package: org.sun.server.pm.LaunchStateController
    // Key: LaunchStateController, LauncherUtils
    public static final boolean DEBUG_LAUNCHER = DEBUG_GLOBAL || SystemProperties.getBoolean(
        "persist.sys.sun.launcher.debug", false
    );

    // Enable this to debug oplus NrModeSwitcher feature
    public static final boolean DEBUG_NR_MODE = DEBUG_GLOBAL || SystemProperties.getBoolean(
        "persist.sys.sun.radio.nrmode.debug", false
    );

    // Enable this to allow third-party apps access LinearMotorVibrator
    // TODO: Remove once we map all oplus effect ids to our HAL's
    // Don't follow DEBUG_GLOBAL as it brings functional change
    public static final boolean DEBUG_OP_LM = SystemProperties.getBoolean(
        "persist.sys.sun.vibrator.oplm.debug", false
    );

    // Enable this to debug PhoneWindowManagerExt (Include system gestures feature)
    // Package: com.android.server.policy.PhoneWindowManagerExt
    // Key: PhoneWindowManagerExt, GestureListenerBase, SystemGesture
    public static final boolean DEBUG_PHONE_WINDOW_MANAGER = DEBUG_GLOBAL || SystemProperties.getBoolean(
        "persist.sys.sun.policy.debug", false
    );

    // Enable this to debug app force fullscreen feature
    // Package: com.android.server.pm.ForceFullController
    // Key: ForceFullController
    public static final boolean DEBUG_PMS = DEBUG_GLOBAL || SystemProperties.getBoolean(
        "persist.sys.sun.pm.debug", false
    );

    // Enable this to debug pocket mode feature
    // Package: org.sun.server.policy.PocketModeController
    // Key: PocketModeController
    public static final boolean DEBUG_POCKET = DEBUG_GLOBAL || SystemProperties.getBoolean(
        "persist.sys.sun.pocket.debug", false
    );

    // Enable this to debug Pop-Up View feature
    public static final boolean DEBUG_POP_UP = DEBUG_GLOBAL || SystemProperties.getBoolean(
        "persist.sys.sun.popup.debug", false
    );

    // Enable this to debug RichTap
    public static final boolean DEBUG_RICHTAP = DEBUG_GLOBAL || SystemProperties.getBoolean(
        "persist.sys.sun.richtap.debug", false
    );

    // Enable this to debug sensor block feature
    // Package: org.sun.server.sensors.SensorBlockController
    // Key: SensorBlockController
    public static final boolean DEBUG_SENSOR = DEBUG_GLOBAL || SystemProperties.getBoolean(
        "persist.sys.sun.sensor.debug", false
    );

    // Enable this to debug System Tool app
    // Key: SystemTool
    public static final boolean DEBUG_SYSTEM_TOOL = DEBUG_GLOBAL || SystemProperties.getBoolean(
        "persist.sys.sun.system_tool.debug", false
    );

    // Enable this to debug three-finger screenshot feature
    // Key: ThreeFinger
    public static final boolean DEBUG_THREE_FINGER = DEBUG_GLOBAL || SystemProperties.getBoolean(
        "persist.sys.sun.threefinger.debug", false
    );

    // Enable this to debug status bar ticker
    // Package: org.sun.systemui.ticker.*
    // Key: Ticker
    public static final boolean DEBUG_TICKER = DEBUG_GLOBAL || SystemProperties.getBoolean(
        "persist.sys.sun.ticker.debug", false
    );

    // Enable this to debug off-screen gesture feature
    // Package: org.sun.server.policy.gesture.TouchGestureController
    // Key: TouchGestureController
    public static final boolean DEBUG_TOUCH_GESTURE = DEBUG_GLOBAL || SystemProperties.getBoolean(
        "persist.sys.sun.gesture.debug", false
    );

    // Enable this to debug adapt vibration effect feature
    // Package: org.sun.server.vibrator.VibrationEffectAdapter
    // Key: VibrationEffectAdapter
    public static final boolean DEBUG_VIBRATION_ADAPTER = DEBUG_GLOBAL || SystemProperties.getBoolean(
        "persist.sys.sun.vibrator.adapter.debug", false
    );

    // Enable this to debug resolution switch feature
    // Package: org.sun.server.wm.DisplayResolutionController
    // Key: DisplayResolutionController
    public static final boolean DEBUG_WMS_RESOLUTION = DEBUG_GLOBAL || SystemProperties.getBoolean(
        "persist.sys.sun.wm.resolution.debug", false
    );

    // Enable this to debug top activity change
    // Package: com.android.server.wm.TopActivityRecorder
    // Key: TopActivityRecorder
    public static final boolean DEBUG_WMS_TOP_APP = DEBUG_GLOBAL || SystemProperties.getBoolean(
        "persist.sys.sun.wm.top_app.debug", false
    );
}
