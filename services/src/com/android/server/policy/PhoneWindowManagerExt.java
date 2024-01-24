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
import android.view.MotionEvent;

import com.android.internal.app.AssistUtils;

import com.android.server.policy.WindowManagerPolicy.WindowState;

import org.nameless.audio.AlertSliderManager;
import org.nameless.provider.SettingsExt;
import org.nameless.server.policy.gesture.SystemGesture;
import org.nameless.view.DisplayResolutionManager;
import org.nameless.view.IDisplayResolutionListener;
import org.nameless.view.ISystemGestureListener;

public class PhoneWindowManagerExt {

    private static String TAG = "PhoneWindowManagerExt";

    private final Handler mHandler = new Handler();

    private AssistUtils mAssistUtils;
    private PhoneWindowManager mPhoneWindowManager;
    private SystemGesture mSystemGesture;

    private WindowState mWindowState = null;

    private boolean mHasAlertSlider;
    private boolean mClickPartialScreenshot;

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

    public void init(PhoneWindowManager pw) {
        mPhoneWindowManager = pw;
        mAssistUtils = new AssistUtils(pw.mContext);
        mSystemGesture = new SystemGesture(pw.mContext, this);
        mHasAlertSlider = AlertSliderManager.hasAlertSlider(pw.mContext);
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

    public WindowState getWindowState() {
        return mWindowState;
    }

    public void observe(ContentResolver resolver, ContentObserver observer) {
        resolver.registerContentObserver(Settings.System.getUriFor(
                SettingsExt.System.CLICK_PARTIAL_SCREENSHOT), false, observer,
                UserHandle.USER_ALL);
    }

    public void updateSettings(ContentResolver resolver) {
        mClickPartialScreenshot = Settings.System.getIntForUser(resolver,
                SettingsExt.System.CLICK_PARTIAL_SCREENSHOT, 0,
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

    public boolean interceptKeyBeforeQueueing(int keyCode, boolean down) {
        if (mHasAlertSlider && AlertSliderManager.maybeNotifyUpdate(
                mPhoneWindowManager.mContext, keyCode, down)) {
            return true;
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

    public boolean isClickPartialScreenshot() {
        return mClickPartialScreenshot;
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
