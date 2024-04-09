/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.screenshot;

import static org.nameless.provider.SettingsExt.System.SCREENSHOT_SOUND;

import static vendor.nameless.hardware.vibratorExt.V1_0.Effect.HEAVY_CLICK;
import static vendor.nameless.hardware.vibratorExt.V1_0.Effect.SCREENSHOT;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.UserHandle;
import android.os.VibrationAttributes;
import android.os.VibrationExtInfo;

import com.android.systemui.settings.UserTracker;
import com.android.systemui.statusbar.VibratorHelper;
import com.android.systemui.util.settings.SystemSettings;

import java.util.concurrent.Executor;

import org.nameless.systemui.statusbar.policy.ForegroundActivityListener;

class ScreenshotControllerExt {

    private static class InstanceHolder {
        private static final ScreenshotControllerExt INSTANCE = new ScreenshotControllerExt();
    }

    static ScreenshotControllerExt getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private static final VibrationAttributes HARDWARE_FEEDBACK_VIBRATION_ATTRIBUTES =
            VibrationAttributes.createForUsage(VibrationAttributes.USAGE_HARDWARE_FEEDBACK);

    private ContentObserver mSettingsObserver;
    private ForegroundActivityListener mForegroundActivityListener;
    private SystemSettings mSystemSettings;
    private UserTracker mUserTracker;
    private UserTracker.Callback mUserTrackerCallback;
    private VibratorHelper mVibratorHelper;

    private boolean mScreenshotSound;

    void init(Executor executor, Handler handler, SystemSettings systemSettings,
            UserTracker userTracker, VibratorHelper vibratorHelper,
            ForegroundActivityListener foregroundActivityListener) {
        mForegroundActivityListener = foregroundActivityListener;
        mSystemSettings = systemSettings;
        mUserTracker = userTracker;
        mVibratorHelper = vibratorHelper;
        mSettingsObserver = new ContentObserver(handler) {
            @Override
            public void onChange(boolean selfChange) {
                mScreenshotSound = mSystemSettings.getIntForUser(SCREENSHOT_SOUND, 1,
                        mUserTracker.getUserId()) == 1;
            }
        };
        mSystemSettings.registerContentObserverForUser(SCREENSHOT_SOUND,
                mSettingsObserver, UserHandle.USER_ALL);
        mSettingsObserver.onChange(false);

        mUserTrackerCallback = new UserTracker.Callback() {
            @Override
            public void onUserChanged(int newUser, Context userContext) {
                mSettingsObserver.onChange(false);
            }
        };
        mUserTracker.addCallback(mUserTrackerCallback, executor);
    }

    void releaseContext() {
        mUserTracker.removeCallback(mUserTrackerCallback);
        mSystemSettings.unregisterContentObserver(mSettingsObserver);
    }

    boolean interceptPlayCameraSound() {
        mVibratorHelper.vibrateExt(new VibrationExtInfo.Builder()
                .setEffectId(SCREENSHOT)
                .setFallbackEffectId(HEAVY_CLICK)
                .setVibrationAttributes(HARDWARE_FEEDBACK_VIBRATION_ATTRIBUTES)
                .build());
        if (!mScreenshotSound) {
            return true;
        }
        return false;
    }

    String getForegroundAppLabel() {
        return mForegroundActivityListener.getTopPackageName();
    }
}
