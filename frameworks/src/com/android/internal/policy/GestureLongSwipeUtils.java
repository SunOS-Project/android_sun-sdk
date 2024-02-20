/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.internal.policy;

import android.content.Context;
import android.os.Handler;
import android.os.UserHandle;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;

import com.android.internal.util.nameless.CustomUtils;

import org.nameless.provider.SettingsExt;

/** @hide */
public class GestureLongSwipeUtils {

    private GestureLongSwipeUtils() {}

    private static final VibrationAttributes HARDWARE_FEEDBACK_VIBRATION_ATTRIBUTES =
            VibrationAttributes.createForUsage(VibrationAttributes.USAGE_HARDWARE_FEEDBACK);
    private static final VibrationEffect LONG_SWIPE_REACHED_EFFECT =
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK);
    private static final VibrationEffect LONG_SWIPE_TRIGGERED_EFFECT =
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK);

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

    public static final int ACTION_END = ACTION_PIN_APP_WINDOW;

    public static final float[] LONG_SWIPE_THRESHOLD_PORT_VALUES = {
        0.3f, 0.4f, 0.5f, 0.6f, 0.7f
    };
    public static final float[] LONG_SWIPE_THRESHOLD_LAND_VALUES = {
        0.2f, 0.28f, 0.36f, 0.44f, 0.52f
    };
    public static final float DEFAULT_LONG_SWIPE_THRESHOLD_PORT = LONG_SWIPE_THRESHOLD_PORT_VALUES[2];
    public static final float DEFAULT_LONG_SWIPE_THRESHOLD_LAND = LONG_SWIPE_THRESHOLD_LAND_VALUES[1];

    public static void triggerAction(Context context, int action) {
        if (shouldVibrateOnTriggered(context, action)) {
            final Vibrator vibrator = context.getSystemService(Vibrator.class);
            vibrator.vibrate(LONG_SWIPE_TRIGGERED_EFFECT, HARDWARE_FEEDBACK_VIBRATION_ATTRIBUTES);
        }
        switch (action) {
            case ACTION_KILL_FOREGROUND_APP:
                CustomUtils.killForegroundApp(context);
                break;
            case ACTION_SWITCH_TO_LAST_APP:
                CustomUtils.switchToLastApp(context);
                break;
            case ACTION_OPEN_NOTIFICATION_PANEL:
                CustomUtils.toggleNotificationPanel();
                break;
            case ACTION_OPEN_QS_PANEL:
                CustomUtils.toggleQSPanel();
                break;
            case ACTION_OPEN_VOLUME_PANEL:
                CustomUtils.toggleVolumePanel(context);
                break;
            case ACTION_SCREEN_OFF:
                CustomUtils.turnScreenOff(context);
                break;
            case ACTION_TAKE_SCREENSHOT_FULL:
                new Handler().postDelayed(() -> {
                    CustomUtils.takeScreenshot(true);
                }, 500L);
                break;
            case ACTION_TAKE_SCREENSHOT_PARTIAL:
                new Handler().postDelayed(() -> {
                    CustomUtils.takeScreenshot(true);
                }, 500L);
                break;
            case ACTION_TOGGLE_FLASHLIGHT:
                CustomUtils.toggleCameraFlash();
                break;
            case ACTION_PIN_APP_WINDOW:
                new Handler().postDelayed(() -> {
                    CustomUtils.pinCurrentAppIntoWindow(context);
                }, 500L);
                break;
        }
    }

    public static void playReachedVibration(Context context) {
        final Vibrator vibrator = context.getSystemService(Vibrator.class);
        vibrator.vibrate(LONG_SWIPE_REACHED_EFFECT, HARDWARE_FEEDBACK_VIBRATION_ATTRIBUTES);
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
