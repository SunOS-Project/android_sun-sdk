/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.internal.policy;

import static org.nameless.view.PopUpViewManager.FEATURE_SUPPORTED;

import static vendor.nameless.hardware.vibratorExt.Effect.BACK_GESTURE;
import static vendor.nameless.hardware.vibratorExt.Effect.HEAVY_CLICK;
import static vendor.nameless.hardware.vibratorExt.Effect.TICK;
import static vendor.nameless.hardware.vibratorExt.Effect.UNIFIED_SUCCESS;

import android.content.Context;
import android.os.UserHandle;
import android.os.VibrationAttributes;
import android.os.VibrationExtInfo;
import android.os.Vibrator;
import android.provider.Settings;

import com.android.internal.util.nameless.CustomUtils;

import org.nameless.provider.SettingsExt;

/** @hide */
public class GestureLongSwipeUtils {

    private GestureLongSwipeUtils() {}

    private static final VibrationAttributes HARDWARE_FEEDBACK_VIBRATION_ATTRIBUTES =
            VibrationAttributes.createForUsage(VibrationAttributes.USAGE_HARDWARE_FEEDBACK);

    public static final int ACTION_EMPTY = 0;
    public static final int ACTION_KILL_FOREGROUND_APP = 1;
    public static final int ACTION_SWITCH_TO_LAST_APP = 2;
    public static final int ACTION_OPEN_NOTIFICATION_PANEL = 3;
    public static final int ACTION_OPEN_QS_PANEL = 4;
    public static final int ACTION_OPEN_VOLUME_PANEL = 5;
    public static final int ACTION_SCREEN_OFF = 6;
    public static final int ACTION_TAKE_SCREENSHOT_FULL = 7;
    public static final int ACTION_TAKE_SCREENSHOT_PARTIAL = 8;
    public static final int ACTION_TOGGLE_FLASHLIGHT = 9;
    public static final int ACTION_PIN_APP_WINDOW = 10;

    public static final int ACTION_END = FEATURE_SUPPORTED ? ACTION_PIN_APP_WINDOW : ACTION_TOGGLE_FLASHLIGHT;

    public static final float[] LONG_SWIPE_THRESHOLD_PORT_VALUES = {
        0.3f, 0.4f, 0.5f, 0.6f, 0.7f
    };
    public static final float[] LONG_SWIPE_THRESHOLD_LAND_VALUES = {
        0.2f, 0.28f, 0.36f, 0.44f, 0.52f
    };
    public static final float DEFAULT_LONG_SWIPE_THRESHOLD_PORT = LONG_SWIPE_THRESHOLD_PORT_VALUES[2];
    public static final float DEFAULT_LONG_SWIPE_THRESHOLD_LAND = LONG_SWIPE_THRESHOLD_LAND_VALUES[1];

    public static Runnable getTriggerActionRunnable(Context context, int action) {
        if (shouldVibrateOnTriggered(context, action)) {
            final Vibrator vibrator = context.getSystemService(Vibrator.class);
            vibrator.vibrateExt(new VibrationExtInfo.Builder()
                    .setEffectId(UNIFIED_SUCCESS)
                    .setFallbackEffectId(HEAVY_CLICK)
                    .setVibrationAttributes(HARDWARE_FEEDBACK_VIBRATION_ATTRIBUTES)
                    .build());
        }
        switch (action) {
            case ACTION_KILL_FOREGROUND_APP:
                return () -> CustomUtils.killForegroundApp(context);
            case ACTION_SWITCH_TO_LAST_APP:
                return () -> CustomUtils.switchToLastApp(context);
            case ACTION_OPEN_NOTIFICATION_PANEL:
                return () -> CustomUtils.toggleNotificationPanel();
            case ACTION_OPEN_QS_PANEL:
                return () -> CustomUtils.toggleQSPanel();
            case ACTION_OPEN_VOLUME_PANEL:
                return () -> CustomUtils.toggleVolumePanel(context);
            case ACTION_SCREEN_OFF:
                return () -> CustomUtils.turnScreenOff(context);
            case ACTION_TAKE_SCREENSHOT_FULL:
                return () -> CustomUtils.takeScreenshot(true);
            case ACTION_TAKE_SCREENSHOT_PARTIAL:
                return () -> CustomUtils.takeScreenshot(false);
            case ACTION_TOGGLE_FLASHLIGHT:
                return () -> CustomUtils.toggleCameraFlash();
            case ACTION_PIN_APP_WINDOW:
                return () -> CustomUtils.pinCurrentAppIntoWindow(context);
            default:
                return null;
        }
    }

    public static void playReachedVibration(Context context) {
        final Vibrator vibrator = context.getSystemService(Vibrator.class);
        vibrator.vibrateExt(new VibrationExtInfo.Builder()
                .setEffectId(BACK_GESTURE)
                .setFallbackEffectId(TICK)
                .setVibrationAttributes(HARDWARE_FEEDBACK_VIBRATION_ATTRIBUTES)
                .build());
    }

    private static boolean shouldVibrateOnTriggered(Context context, int action) {
        switch (action) {
            case ACTION_OPEN_NOTIFICATION_PANEL:
            case ACTION_OPEN_QS_PANEL:
                return Settings.System.getIntForUser(context.getContentResolver(),
                        SettingsExt.System.CUSTOM_HAPTIC_ON_MISC_SCENES,
                        1, UserHandle.USER_CURRENT) == 0;
            case ACTION_TAKE_SCREENSHOT_FULL:
            case ACTION_TAKE_SCREENSHOT_PARTIAL:
                return false;
            default:
                return true;
        }
    }
}
