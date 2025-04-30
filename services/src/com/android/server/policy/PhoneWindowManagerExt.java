/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.server.policy;

import static android.provider.Settings.Secure.USER_SETUP_COMPLETE;

import static org.sun.os.DebugConstants.DEBUG_PHONE_WINDOW_MANAGER;
import static org.sun.server.policy.gesture.GestureListenerBase.motionEventToString;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.SystemClock;
import android.provider.Settings;
import android.service.dreams.IDreamManager;
import android.util.Slog;
import android.view.Display;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import com.android.internal.app.AssistUtils;
import com.android.internal.util.sun.CustomUtils;

import com.android.server.policy.WindowManagerPolicy.WindowState;

import org.sun.app.GameModeInfo;
import org.sun.app.GameModeManager;
import org.sun.app.IGameModeInfoListener;
import org.sun.audio.AlertSliderManager;
import org.sun.provider.SettingsExt;
import org.sun.server.policy.PocketModeController;
import org.sun.server.policy.gesture.SystemGesture;
import org.sun.server.policy.gesture.ThreeFingerGestureController;
import org.sun.server.policy.gesture.TouchGestureController;
import org.sun.view.DisplayResolutionManager;
import org.sun.view.IDisplayResolutionListener;
import org.sun.view.ISystemGestureListener;

public class PhoneWindowManagerExt {

    private static String TAG = "PhoneWindowManagerExt";

    private static final int MSG_DISPATCH_VOLKEY_WITH_WAKE_LOCK = 101;

    private static final int LONG_PRESS_POWER_HIDE_POCKET_LOCK = 11;

    static final int EXT_UNHANDLED = 0;
    static final int EXT_PASS_TO_USER = 1;
    static final int EXT_CONSUMED = 2;

    private AssistUtils mAssistUtils;
    private AudioManager mAudioManager;
    private GameModeManager mGameModeManager;
    private Handler mHandler;
    private PhoneWindowManager mPhoneWindowManager;
    private SystemGesture mSystemGesture;

    private WindowState mWindowState = null;

    private boolean mHasAlertSlider;
    private boolean mClickPartialScreenshot;
    private boolean mPowerTorchGesture;
    private boolean mThreeFingerHoldScreenshot;
    private boolean mThreeFingerSwipeScreenshot;
    private boolean mUserSetupCompleted;
    private boolean mVolBtnMusicControls;
    private boolean mVolBtnLongPress;

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

    private final IGameModeInfoListener.Stub mGameModeInfoListener =
            new IGameModeInfoListener.Stub() {
        @Override
        public void onGameModeInfoChanged() {
            final GameModeInfo info = mGameModeManager.getGameModeInfo();
            if (info != null) {
                mSystemGesture.onGameModeInfoChanged(info);
                ThreeFingerGestureController.getInstance().onGameModeInfoChanged(info);
            }
        }
    };

    void init(PhoneWindowManager pw, Handler handler) {
        mPhoneWindowManager = pw;
        mHandler = handler;
        mAssistUtils = new AssistUtils(pw.mContext);
        mSystemGesture = new SystemGesture(pw.mContext, this);
        mHasAlertSlider = AlertSliderManager.hasAlertSlider(pw.mContext);
        mAudioManager = pw.mContext.getSystemService(AudioManager.class);
        TouchGestureController.getInstance().init(pw.mContext);
    }

    void systemReady() {
        mSystemGesture.systemReady();
        TouchGestureController.getInstance().systemReady();
    }

    void systemBooted() {
        final DisplayResolutionManager drm =
                mPhoneWindowManager.mContext.getSystemService(DisplayResolutionManager.class);
        drm.registerDisplayResolutionListener(mDisplayResolutionListener);

        mGameModeManager = mPhoneWindowManager.mContext.getSystemService(GameModeManager.class);
        mGameModeManager.registerGameModeInfoListener(mGameModeInfoListener);

        ThreeFingerGestureController.getInstance().onBootCompleted();
    }

    void onConfigureChanged() {
        mSystemGesture.configure();
    }

    void onDefaultDisplayFocusChangedLw(WindowState win) {
        mWindowState = win;
    }

    void onHandleMessage(Message msg) {
        switch (msg.what) {
            case MSG_DISPATCH_VOLKEY_WITH_WAKE_LOCK:
                KeyEvent event = (KeyEvent) msg.obj;
                mVolBtnLongPress = true;
                mPhoneWindowManager.dispatchMediaKeyWithWakeLockToAudioService(event);
                mPhoneWindowManager.dispatchMediaKeyWithWakeLockToAudioService(
                        KeyEvent.changeAction(event, KeyEvent.ACTION_UP));
                break;
        }
    }

    public WindowState getWindowState() {
        return mWindowState;
    }

    void observe(ContentResolver resolver, ContentObserver observer) {
        resolver.registerContentObserver(Settings.Secure.getUriFor(
                Settings.Secure.USER_SETUP_COMPLETE), false, observer,
                UserHandle.USER_ALL);
        resolver.registerContentObserver(Settings.System.getUriFor(
                SettingsExt.System.CLICK_PARTIAL_SCREENSHOT), false, observer,
                UserHandle.USER_ALL);
        resolver.registerContentObserver(Settings.System.getUriFor(
                SettingsExt.System.TORCH_POWER_BUTTON_GESTURE), false, observer,
                UserHandle.USER_ALL);
        resolver.registerContentObserver(Settings.System.getUriFor(
                SettingsExt.System.THREE_FINGER_HOLD_SCREENSHOT), false, observer,
                UserHandle.USER_ALL);
        resolver.registerContentObserver(Settings.System.getUriFor(
                SettingsExt.System.THREE_FINGER_SWIPE_SCREENSHOT), false, observer,
                UserHandle.USER_ALL);
        resolver.registerContentObserver(Settings.System.getUriFor(
                SettingsExt.System.VOLBTN_MUSIC_CONTROLS), false, observer,
                UserHandle.USER_ALL);
    }

    void updateSettings(ContentResolver resolver) {
        mUserSetupCompleted = Settings.Secure.getIntForUser(resolver,
                Settings.Secure.USER_SETUP_COMPLETE, 0,
                UserHandle.USER_CURRENT) == 1;
        mClickPartialScreenshot = Settings.System.getIntForUser(resolver,
                SettingsExt.System.CLICK_PARTIAL_SCREENSHOT, 0,
                UserHandle.USER_CURRENT) == 1;
        mPowerTorchGesture = Settings.System.getIntForUser(resolver,
                SettingsExt.System.TORCH_POWER_BUTTON_GESTURE, 0,
                UserHandle.USER_CURRENT) == 1;
        mThreeFingerHoldScreenshot = Settings.System.getIntForUser(resolver,
                SettingsExt.System.THREE_FINGER_HOLD_SCREENSHOT, 0,
                UserHandle.USER_CURRENT) == 1;
        mThreeFingerSwipeScreenshot = Settings.System.getIntForUser(resolver,
                SettingsExt.System.THREE_FINGER_SWIPE_SCREENSHOT, 0,
                UserHandle.USER_CURRENT) == 1;
        mVolBtnMusicControls = Settings.System.getIntForUser(resolver,
                SettingsExt.System.VOLBTN_MUSIC_CONTROLS, 0,
                UserHandle.USER_CURRENT) == 1;
        ThreeFingerGestureController.getInstance().updateListenerState();
        TouchGestureController.getInstance().updateSettings();
    }

    void registerSystemGestureListener(String pkg, int gesture, ISystemGestureListener listener) {
        try {
            mSystemGesture.registerSystemGestureListener(pkg, gesture, listener);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    void unregisterSystemGestureListener(String pkg, int gesture, ISystemGestureListener listener) {
        mSystemGesture.unregisterSystemGestureListener(pkg, gesture, listener);
    }

    void notifyBackGestureRegion(int left, int right) {
        mSystemGesture.notifyBackGestureRegion(left, right);
    }

    int getResolvedLongPressOnPowerBehavior() {
        if (PocketModeController.getInstance().isPocketLockShowing()) {
            return LONG_PRESS_POWER_HIDE_POCKET_LOCK;
        }
        return -1;
    }

    boolean hasAssistant(int currentUserId) {
        return mAssistUtils.getAssistComponentForUser(currentUserId) != null;
    }

    void handlePowerLongPress(int behavior) {
        switch (behavior) {
            case LONG_PRESS_POWER_HIDE_POCKET_LOCK:
                mPhoneWindowManager.mPowerKeyHandled = true;
                mPhoneWindowManager.performHapticFeedback(
                        Process.myUid(),
                        mPhoneWindowManager.mContext.getOpPackageName(),
                        HapticFeedbackConstants.LONG_PRESS, true,
                        "Power - Long-Press - Hide Pocket Lock", false);
                PocketModeController.getInstance().unregisterAll();
                break;
        }
    }

    boolean handleTorchPress(boolean fromNonInteractive) {
        if (!isPowerTorchGestureOn()) {
            return false;
        }
        if (!fromNonInteractive && !isTorchTurnedOn()) {
            return false;
        }
        mPhoneWindowManager.performHapticFeedback(
                Process.myUid(),
                mPhoneWindowManager.mContext.getOpPackageName(),
                HapticFeedbackConstants.LONG_PRESS, true,
                "Power - Long Press - Torch", false);
        CustomUtils.toggleCameraFlash();
        return true;
    }

    int handleVolumeKeyPress(KeyEvent event, boolean down) {
        if (!mAudioManager.isMusicActive()) {
            if (DEBUG_PHONE_WINDOW_MANAGER) {
                Slog.d(TAG, "handleVolumeKeyPress, music is not playing");
            }
            return EXT_PASS_TO_USER;
        }

        if (!mVolBtnMusicControls || event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_MUTE) {
            if (DEBUG_PHONE_WINDOW_MANAGER) {
                Slog.d(TAG, "handleVolumeKeyPress, mVolBtnMusicControls=" + mVolBtnMusicControls
                        + ", isMuteKey=" + (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_MUTE)
                        + ", isDown=" + down);
            }
            return down ? EXT_UNHANDLED : EXT_CONSUMED;
        }

        if (down) {
            mVolBtnLongPress = false;
            final int newKeyCode = event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP ?
                    KeyEvent.KEYCODE_MEDIA_NEXT : KeyEvent.KEYCODE_MEDIA_PREVIOUS;
            scheduleLongPressKeyEvent(event, newKeyCode);
            if (DEBUG_PHONE_WINDOW_MANAGER) {
                Slog.d(TAG, "handleVolumeKeyPress, isDown=true");
            }
            return EXT_CONSUMED;
        }

        mHandler.removeMessages(MSG_DISPATCH_VOLKEY_WITH_WAKE_LOCK);
        if (mVolBtnLongPress) {
            if (DEBUG_PHONE_WINDOW_MANAGER) {
                Slog.d(TAG, "handleVolumeKeyPress, isDown=false, isLongPres=true");
            }
            return EXT_CONSUMED;
        }
        if (DEBUG_PHONE_WINDOW_MANAGER) {
            Slog.d(TAG, "handleVolumeKeyPress, isDown=false, isLongPres=false");
        }
        return EXT_UNHANDLED;
    }

    private boolean isTorchTurnedOn() {
        return Settings.Secure.getInt(mPhoneWindowManager.mContext.getContentResolver(),
                Settings.Secure.FLASHLIGHT_ENABLED, 0) != 0;
    }

    boolean interceptKeyBeforeQueueing(int keyCode, int scanCode, boolean down, boolean interactive) {
        if (mHasAlertSlider && AlertSliderManager.maybeNotifyUpdate(
                mPhoneWindowManager.mContext, keyCode, down)) {
            return true;
        }
        if (!mUserSetupCompleted) {
            return false;
        }
        if (TouchGestureController.getInstance().handleKeyEvent(scanCode, down)) {
            return true;
        }
        return false;
    }

    boolean interceptMotionBeforeQueueing(MotionEvent event) {
        if (event.getDisplayId() != Display.DEFAULT_DISPLAY) {
            return false;
        }
        final boolean isSystemGesture = mSystemGesture.interceptMotionBeforeQueueing(event);
        if (DEBUG_PHONE_WINDOW_MANAGER && event.getAction() != MotionEvent.ACTION_MOVE &&
                event.getAction() != MotionEvent.ACTION_HOVER_MOVE) {
            Slog.i(TAG, "interceptMotionBeforeQueueing, " + motionEventToString(event)
                    + ", isSystemGesture=" + isSystemGesture);
        }
        return isSystemGesture;
    }

    boolean interceptDispatchInputWhenNonInteractive(int keyCode) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
                keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            return mVolBtnMusicControls && isDozeMode();
        }
        return false;
    }

    void interceptPowerKeyDown() {
        // Abort possibly stuck animations.
        mHandler.post(mPhoneWindowManager.mWindowManagerFuncs::triggerAnimationFailsafe);
    }

    public void takeScreenshotIfSetupCompleted(boolean fullscreen) {
        if (!mUserSetupCompleted) {
            return;
        }
        if (!fullscreen && mPhoneWindowManager.isKeyguardShowing()) {
            return;
        }
        mPhoneWindowManager.takeScreenshotExt(fullscreen);
    }

    boolean isClickPartialScreenshot() {
        return mClickPartialScreenshot;
    }

    boolean isPowerTorchGestureOn() {
        return mPowerTorchGesture;
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

    boolean isVolumeButtonMusicControl() {
        return mVolBtnMusicControls;
    }

    public boolean isTouching() {
        return mSystemGesture.isTouching();
    }

    private void scheduleLongPressKeyEvent(KeyEvent origEvent, int keyCode) {
        KeyEvent event = new KeyEvent(origEvent.getDownTime(), origEvent.getEventTime(),
                origEvent.getAction(), keyCode, 0);
        Message msg = mHandler.obtainMessage(MSG_DISPATCH_VOLKEY_WITH_WAKE_LOCK, event);
        msg.setAsynchronous(true);
        mHandler.sendMessageDelayed(msg, ViewConfiguration.getLongPressTimeout());
    }

    public void dispatchMediaKeyToMediaSession(int keyCode) {
        KeyEvent event = new KeyEvent(SystemClock.uptimeMillis(),
                SystemClock.uptimeMillis(), KeyEvent.ACTION_DOWN, keyCode, 0);
        mPhoneWindowManager.dispatchMediaKeyWithWakeLockToAudioService(event);
        mPhoneWindowManager.dispatchMediaKeyWithWakeLockToAudioService(
                KeyEvent.changeAction(event, KeyEvent.ACTION_UP));
    }

    private static boolean isDozeMode() {
        final IDreamManager dreamManager = PhoneWindowManager.getDreamManager();
        try {
            return dreamManager != null && dreamManager.isDreaming();
        } catch (RemoteException e) {
            return false;
        }
    }
}
