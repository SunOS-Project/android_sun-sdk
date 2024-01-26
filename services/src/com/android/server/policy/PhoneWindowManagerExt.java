/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.server.policy;

import static com.android.server.policy.WindowManagerPolicy.SYSTEM_GESTURE_DOWN;
import static com.android.server.policy.WindowManagerPolicy.SYSTEM_GESTURE_MOVE;
import static com.android.server.policy.WindowManagerPolicy.SYSTEM_GESTURE_MOVE_TRIGGERED;
import static com.android.server.policy.WindowManagerPolicy.SYSTEM_GESTURE_NONE;
import static com.android.server.policy.WindowManagerPolicy.SYSTEM_GESTURE_UP;
import static com.android.server.policy.WindowManagerPolicy.SYSTEM_GESTURE_UP_TRIGGERED;

import static org.nameless.os.DebugConstants.DEBUG_PHONE_WINDOW_MANAGER;
import static org.nameless.server.policy.gesture.GestureListenerBase.motionEventToString;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Slog;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.android.internal.app.AssistUtils;

import com.android.server.policy.WindowManagerPolicy.WindowState;

import org.nameless.audio.AlertSliderManager;
import org.nameless.os.IPocketCallback;
import org.nameless.os.PocketManager;
import org.nameless.provider.SettingsExt;
import org.nameless.server.policy.PocketLock;
import org.nameless.server.policy.gesture.SystemGesture;
import org.nameless.view.DisplayResolutionManager;
import org.nameless.view.IDisplayResolutionListener;
import org.nameless.view.ISystemGestureListener;

public class PhoneWindowManagerExt {

    private static String TAG = "PhoneWindowManagerExt";

    private final Handler mHandler = new Handler();

    private AssistUtils mAssistUtils;
    private PhoneWindowManager mPhoneWindowManager;
    private PocketLock mPocketLock;
    private PocketManager mPocketManager;
    private SystemGesture mSystemGesture;

    private WindowState mWindowState = null;

    private boolean mHasAlertSlider;
    private boolean mClickPartialScreenshot;
    private boolean mThreeFingerHoldScreenshot;
    private boolean mThreeFingerSwipeScreenshot;
    private boolean mPocketLockShowing;
    private boolean mIsDeviceInPocket;

    private static class InstanceHolder {
        private static final PhoneWindowManagerExt INSTANCE = new PhoneWindowManagerExt();
    }

    public static PhoneWindowManagerExt getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private final IDisplayResolutionListener.Stub mDisplayResolutionListener =
            new IDisplayResolutionListener.Stub() {
        @Override
        public void onDisplayResolutionChanged(int width, int height) {
            mHandler.postDelayed(() -> mSystemGesture.configure(), 500L);
        }
    };

    private final IPocketCallback mPocketCallback = new IPocketCallback.Stub() {
        @Override
        public void onStateChanged(boolean isDeviceInPocket, int reason) {
            final boolean wasDeviceInPocket = mIsDeviceInPocket;
            if (reason == PocketManager.REASON_SENSOR) {
                mIsDeviceInPocket = isDeviceInPocket;
            } else {
                mIsDeviceInPocket = false;
            }
            if (wasDeviceInPocket != mIsDeviceInPocket) {
                handleDevicePocketStateChanged();
            }
        }
    };

    public void init(PhoneWindowManager pw) {
        mPhoneWindowManager = pw;
        mAssistUtils = new AssistUtils(pw.mContext);
        mSystemGesture = new SystemGesture(pw.mContext, this);
        mHasAlertSlider = AlertSliderManager.hasAlertSlider(pw.mContext);
        mPocketManager = pw.mContext.getSystemService(PocketManager.class);
        mPocketManager.addCallback(mPocketCallback);
        mPocketLock = new PocketLock(pw.mContext);
    }

    public void systemReady() {
        mSystemGesture.configure();
    }

    public void systemBooted() {
        final DisplayResolutionManager drm =
                mPhoneWindowManager.mContext.getSystemService(DisplayResolutionManager.class);
        drm.registerDisplayResolutionListener(mDisplayResolutionListener);
    }

    public void onConfigureChanged() {
        mSystemGesture.configure();
    }

    public void onDefaultDisplayFocusChangedLw(WindowState win) {
        mWindowState = win;
    }

    public void onLongPressPowerHidePocket() {
        hidePocketLock(true);
        mPocketManager.setListeningExternal(false);
    }

    public void onStartedGoingToSleep() {
        if (mPocketManager != null) {
            mPocketManager.onInteractiveChanged(false);
        }
    }

    public void onStartedWakingUp() {
        if (mPocketManager != null) {
            mPocketManager.onInteractiveChanged(true);
        }
    }

    public WindowState getWindowState() {
        return mWindowState;
    }

    public void observe(ContentResolver resolver, ContentObserver observer) {
        resolver.registerContentObserver(Settings.System.getUriFor(
                SettingsExt.System.CLICK_PARTIAL_SCREENSHOT), false, observer,
                UserHandle.USER_ALL);
        resolver.registerContentObserver(Settings.System.getUriFor(
                SettingsExt.System.THREE_FINGER_HOLD_SCREENSHOT), false, observer,
                UserHandle.USER_ALL);
        resolver.registerContentObserver(Settings.System.getUriFor(
                SettingsExt.System.THREE_FINGER_SWIPE_SCREENSHOT), false, observer,
                UserHandle.USER_ALL);
    }

    public void updateSettings(ContentResolver resolver) {
        mClickPartialScreenshot = Settings.System.getIntForUser(resolver,
                SettingsExt.System.CLICK_PARTIAL_SCREENSHOT, 0,
                UserHandle.USER_CURRENT) == 1;
        mThreeFingerHoldScreenshot = Settings.System.getIntForUser(resolver,
                SettingsExt.System.THREE_FINGER_HOLD_SCREENSHOT, 0,
                UserHandle.USER_CURRENT) == 1;
        mThreeFingerSwipeScreenshot = Settings.System.getIntForUser(resolver,
                SettingsExt.System.THREE_FINGER_SWIPE_SCREENSHOT, 0,
                UserHandle.USER_CURRENT) == 1;
    }

    public void registerSystemGestureListener(String pkg, int gesture, ISystemGestureListener listener) {
        try {
            mSystemGesture.registerSystemGestureListener(pkg, gesture, listener);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void unregisterSystemGestureListener(String pkg, int gesture, ISystemGestureListener listener) {
        mSystemGesture.unregisterSystemGestureListener(pkg, gesture, listener);
    }

    public boolean hasAssistant(int currentUserId) {
        return mAssistUtils.getAssistComponentForUser(currentUserId) != null;
    }

    public boolean interceptKeyBeforeQueueing(int keyCode, boolean down, boolean interactive) {
        if (mHasAlertSlider && AlertSliderManager.maybeNotifyUpdate(
                mPhoneWindowManager.mContext, keyCode, down)) {
            return true;
        }
        if (mIsDeviceInPocket && (!interactive || mPocketLockShowing)) {
            if (keyCode != KeyEvent.KEYCODE_POWER &&
                    keyCode != KeyEvent.KEYCODE_VOLUME_UP &&
                    keyCode != KeyEvent.KEYCODE_VOLUME_DOWN &&
                    keyCode != KeyEvent.KEYCODE_MEDIA_PLAY &&
                    keyCode != KeyEvent.KEYCODE_MEDIA_PAUSE &&
                    keyCode != KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE &&
                    keyCode != KeyEvent.KEYCODE_HEADSETHOOK &&
                    keyCode != KeyEvent.KEYCODE_MEDIA_STOP &&
                    keyCode != KeyEvent.KEYCODE_MEDIA_NEXT &&
                    keyCode != KeyEvent.KEYCODE_MEDIA_PREVIOUS &&
                    keyCode != KeyEvent.KEYCODE_VOLUME_MUTE) {
                return true;
            }
        }
        return false;
    }

    public int interceptMotionBeforeQueueing(MotionEvent event) {
        if (event.getDisplayId() != Display.DEFAULT_DISPLAY) {
            return SYSTEM_GESTURE_NONE;
        }
        final int result = mSystemGesture.interceptMotionBeforeQueueing(event);
        if (DEBUG_PHONE_WINDOW_MANAGER && event.getAction() != MotionEvent.ACTION_MOVE) {
            Slog.i(TAG, "interceptMotionBeforeQueueing, " + motionEventToString(event)
                    + ", result=" + resultToString(result));
        }
        return result;
    }

    public void takeScreenshotIfSetupCompleted(boolean fullscreen) {
        if (!mPhoneWindowManager.isUserSetupComplete()) {
            return;
        }
        if (!fullscreen && mPhoneWindowManager.isKeyguardShowing()) {
            return;
        }
        mPhoneWindowManager.takeScreenshotExt(fullscreen);
    }

    public boolean isClickPartialScreenshot() {
        return mClickPartialScreenshot;
    }

    public boolean isThreeFingerGestureOn() {
        return mThreeFingerHoldScreenshot || mThreeFingerSwipeScreenshot;
    }

    public boolean isThreeFingerHoldOn() {
        return mThreeFingerHoldScreenshot;
    }

    public boolean isThreeFingerSwipeOn() {
        return mThreeFingerSwipeScreenshot;
    }

    public boolean isDeviceInPocket() {
        return mIsDeviceInPocket;
    }

    public boolean isPocketLockShowing() {
        return mPocketLockShowing;
    }

    private void handleDevicePocketStateChanged() {
        final boolean interactive = mPhoneWindowManager.mPowerManager.isInteractive();
        if (mIsDeviceInPocket) {
            showPocketLock(interactive);
        } else {
            hidePocketLock(interactive);
        }
    }

    private void showPocketLock(boolean animate) {
        if (!mPhoneWindowManager.mSystemReady ||
                !mPhoneWindowManager.mSystemBooted ||
                !mPhoneWindowManager.isKeyguardDrawnLw() ||
                mPocketLock == null ||
                mPocketLockShowing) {
            return;
        }

        if (mPhoneWindowManager.mPowerManager.isInteractive() &&
                !mPhoneWindowManager.isKeyguardShowingAndNotOccluded()){
            return;
        }

        if (DEBUG_PHONE_WINDOW_MANAGER) {
            Slog.d(TAG, "showPocketLock, animate=" + animate);
        }

        mPocketLock.show(animate);
        mPocketLockShowing = true;

        mPocketManager.setPocketLockVisible(true);
    }

    private void hidePocketLock(boolean animate) {
        if (!mPhoneWindowManager.mSystemReady ||
                !mPhoneWindowManager.mSystemBooted ||
                !mPhoneWindowManager.isKeyguardDrawnLw() ||
                mPocketLock == null ||
                mPocketLockShowing) {
            return;
        }

        if (DEBUG_PHONE_WINDOW_MANAGER) {
            Slog.d(TAG, "hidePocketLock, animate=" + animate);
        }

        mPocketLock.hide(animate);
        mPocketLockShowing = false;

        mPocketManager.setPocketLockVisible(false);
    }

    private static String resultToString(int result) {
        switch (result) {
            case SYSTEM_GESTURE_DOWN:
                return "SYSTEM_GESTURE_DOWN";
            case SYSTEM_GESTURE_MOVE:
                return "SYSTEM_GESTURE_MOVE";
            case SYSTEM_GESTURE_MOVE_TRIGGERED:
                return "SYSTEM_GESTURE_MOVE_TRIGGERED";
            case SYSTEM_GESTURE_NONE:
                return "SYSTEM_GESTURE_NONE";
            case SYSTEM_GESTURE_UP:
                return "SYSTEM_GESTURE_UP";
            case SYSTEM_GESTURE_UP_TRIGGERED:
                return "SYSTEM_GESTURE_UP_TRIGGERED";
            default:
                return "UNKNOWN";
        }
    }
}
