/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.server.app;

import static android.app.ActivityTaskManager.INVALID_TASK_ID;
import static android.os.Process.THREAD_PRIORITY_DEFAULT;

import static org.sun.app.GameModeManager.IN_GAME_CALL_NO_ACTION;
import static org.sun.content.ContextExt.GAME_MODE_SERVICE;
import static org.sun.os.DebugConstants.DEBUG_GAME;
import static org.sun.provider.SettingsExt.System.GAME_MODE_APP_LIST;
import static org.sun.provider.SettingsExt.System.GAME_MODE_CALL_ACTION;
import static org.sun.provider.SettingsExt.System.GAME_MODE_DANMAKU_NOTIFICATION;
import static org.sun.provider.SettingsExt.System.GAME_MODE_DISABLE_AUTO_BRIGHTNESS;
import static org.sun.provider.SettingsExt.System.GAME_MODE_DISABLE_HEADS_UP;
import static org.sun.provider.SettingsExt.System.GAME_MODE_DISABLE_THREE_FINGER_GESTURES;
import static org.sun.provider.SettingsExt.System.GAME_MODE_LOCK_GESTURES;
import static org.sun.provider.SettingsExt.System.GAME_MODE_LOCK_STATUS_BAR;
import static org.sun.provider.SettingsExt.System.GAME_MODE_SILENT_NOTIFICATION;
import static org.sun.provider.SettingsExt.System.GAME_MODE_SUPPRESS_FULLSCREEN_INTENT;

import android.app.ActivityThread;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Slog;
import android.widget.Toast;

import com.android.internal.R;
import com.android.internal.util.sun.MutablePair;

import com.android.server.ServiceThread;
import com.android.server.UiThread;
import com.android.server.wm.TopActivityRecorder;

import java.util.ArrayList;

import org.sun.app.GameModeInfo;
import org.sun.app.IGameModeInfoListener;
import org.sun.app.IGameModeManagerService;
import org.sun.server.SunSystemExService;
import org.sun.server.display.DisplayFeatureController;

public class GameModeController {

    private static class InstanceHolder {
        private static GameModeController INSTANCE = new GameModeController();
    }

    public static GameModeController getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private static final String TAG = "GameModeController";

    private static final long GESTURE_AUTO_LOCK_INTERVAL = 3500L;
    private static final long GESTURE_UPDATE_SWIPE_INTERVAL = 2500L;

    public static final int GESTURE_TYPE_BACK = 0;
    public static final int GESTURE_TYPE_NAVIGATION_BAR = 1;
    public static final int GESTURE_TYPE_STATUS_BAR = 2;
    private static final int GESTURE_TYPE_SIZE = GESTURE_TYPE_STATUS_BAR + 1;

    private final class GameModeInfoListener {
        final IGameModeInfoListener mListener;
        final IBinder.DeathRecipient mDeathRecipient;

        GameModeInfoListener(IGameModeInfoListener listener,
                IBinder.DeathRecipient deathRecipient) {
            mListener = listener;
            mDeathRecipient = deathRecipient;
        }
    }

    private final Object mListenerLock = new Object();
    private final Object mPackageLock = new Object();
    private final Object mStateLock = new Object();

    private final Handler mHandler;
    private final ServiceThread mServiceThread;

    private final Handler mUiHandler = new Handler(UiThread.getHandler().getLooper());

    private final ArraySet<String> mGamePackages = new ArraySet<>();

    private final ArrayList<MutablePair<Boolean, Long>> mGestureLockedList = new ArrayList<>();
    private final ArrayList<GameModeInfoListener> mListeners = new ArrayList<>();

    private SunSystemExService mSystemExService;
    private SettingsObserver mSettingsObserver;

    private boolean mInGame;
    private String mGamePackage;
    private int mGameTaskId;
    private boolean mDanmakuNotification;
    private boolean mDisableAutoBrightness;
    private boolean mDisableHeadsUp;
    private boolean mDisableThreeFingerGestures;
    private boolean mLockGestures;
    private boolean mLockStatusbar;
    private boolean mSilentNotification;
    private boolean mSuppressFullscreenIntent;
    private int mCallAction;

    private long mLastGestureUnlockTime = -1L;

    private Toast mGestureLockedToast;

    private final class GameModeManagerService extends IGameModeManagerService.Stub {
        @Override
        public boolean addGame(String packageName) {
            synchronized (mPackageLock) {
                if (DEBUG_GAME) {
                    Slog.d(TAG, "addGame: " + packageName);
                }
                if (!mGamePackages.add(packageName)) {
                    return false;
                }
                saveGameListIntoSettingsLocked();
                updateGameModeState(mSystemExService.getTopFullscreenPackage(),
                        mSystemExService.getTopFullscreenTaskId());
                return true;
            }
        }

        @Override
        public boolean removeGame(String packageName) {
            synchronized (mPackageLock) {
                if (DEBUG_GAME) {
                    Slog.d(TAG, "removeGame: " + packageName);
                }
                if (!mGamePackages.remove(packageName)) {
                    return false;
                }
                saveGameListIntoSettingsLocked();
                updateGameModeState(mSystemExService.getTopFullscreenPackage(),
                        mSystemExService.getTopFullscreenTaskId());
                return true;
            }
        }

        @Override
        public boolean isAppGame(String packageName) {
            synchronized (mPackageLock) {
                return mGamePackages.contains(packageName);
            }
        }

        @Override
        public GameModeInfo getGameModeInfo() {
            synchronized (mStateLock) {
                return buildGameModeInfoLocked();
            }
        }

        @Override
        public boolean registerGameModeInfoListener(IGameModeInfoListener listener) {
            final IBinder listenerBinder = listener.asBinder();
            IBinder.DeathRecipient dr = new IBinder.DeathRecipient() {
                @Override
                public void binderDied() {
                    synchronized (mListenerLock) {
                        for (int i = 0; i < mListeners.size(); i++) {
                            if (listenerBinder == mListeners.get(i).mListener.asBinder()) {
                                GameModeInfoListener removed = mListeners.remove(i);
                                IBinder binder = removed.mListener.asBinder();
                                if (binder != null) {
                                    binder.unlinkToDeath(this, 0);
                                }
                                i--;
                            }
                        }
                    }
                }
            };

            synchronized (mListenerLock) {
                try {
                    listener.asBinder().linkToDeath(dr, 0);
                    mListeners.add(new GameModeInfoListener(listener, dr));
                } catch (RemoteException e) {
                    // Client died, no cleanup needed.
                    return false;
                }
                return true;
            }
        }

        @Override
        public boolean unregisterGameModeInfoListener(IGameModeInfoListener listener) {
            boolean found = false;
            final IBinder listenerBinder = listener.asBinder();
            synchronized (mListenerLock) {
                for (int i = 0; i < mListeners.size(); i++) {
                    found = true;
                    GameModeInfoListener gameModeInfoListener = mListeners.get(i);
                    if (listenerBinder == gameModeInfoListener.mListener.asBinder()) {
                        GameModeInfoListener removed = mListeners.remove(i);
                        IBinder binder = removed.mListener.asBinder();
                        if (binder != null) {
                            binder.unlinkToDeath(removed.mDeathRecipient, 0);
                        }
                        i--;
                    }
                }
            }
            return found;
        }
    }

    private final class SettingsObserver extends ContentObserver {

        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            mSystemExService.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(GAME_MODE_CALL_ACTION),
                    false, this, UserHandle.USER_ALL);
            mSystemExService.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(GAME_MODE_DANMAKU_NOTIFICATION),
                    false, this, UserHandle.USER_ALL);
            mSystemExService.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(GAME_MODE_DISABLE_AUTO_BRIGHTNESS),
                    false, this, UserHandle.USER_ALL);
            mSystemExService.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(GAME_MODE_DISABLE_HEADS_UP),
                    false, this, UserHandle.USER_ALL);
            mSystemExService.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(GAME_MODE_DISABLE_THREE_FINGER_GESTURES),
                    false, this, UserHandle.USER_ALL);
            mSystemExService.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(GAME_MODE_LOCK_GESTURES),
                    false, this, UserHandle.USER_ALL);
            mSystemExService.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(GAME_MODE_LOCK_STATUS_BAR),
                    false, this, UserHandle.USER_ALL);
            mSystemExService.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(GAME_MODE_SILENT_NOTIFICATION),
                    false, this, UserHandle.USER_ALL);
            mSystemExService.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(GAME_MODE_SUPPRESS_FULLSCREEN_INTENT),
                    false, this, UserHandle.USER_ALL);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            synchronized (mStateLock) {
                switch (uri.getLastPathSegment()) {
                    case GAME_MODE_CALL_ACTION:
                        mCallAction = Settings.System.getIntForUser(
                                mSystemExService.getContentResolver(),
                                GAME_MODE_CALL_ACTION,
                                IN_GAME_CALL_NO_ACTION, UserHandle.USER_CURRENT);
                        break;
                    case GAME_MODE_DANMAKU_NOTIFICATION:
                        mDanmakuNotification = Settings.System.getIntForUser(
                                mSystemExService.getContentResolver(),
                                GAME_MODE_DANMAKU_NOTIFICATION,
                                1, UserHandle.USER_CURRENT) == 1;
                        break;
                    case GAME_MODE_DISABLE_AUTO_BRIGHTNESS:
                        mDisableAutoBrightness = Settings.System.getIntForUser(
                                mSystemExService.getContentResolver(),
                                GAME_MODE_DISABLE_AUTO_BRIGHTNESS,
                                0, UserHandle.USER_CURRENT) == 1;
                        break;
                    case GAME_MODE_DISABLE_HEADS_UP:
                        mDisableHeadsUp = Settings.System.getIntForUser(
                                mSystemExService.getContentResolver(),
                                GAME_MODE_DISABLE_HEADS_UP,
                                0, UserHandle.USER_CURRENT) == 1;
                        break;
                    case GAME_MODE_DISABLE_THREE_FINGER_GESTURES:
                        mDisableThreeFingerGestures = Settings.System.getIntForUser(
                                mSystemExService.getContentResolver(),
                                GAME_MODE_DISABLE_THREE_FINGER_GESTURES,
                                1, UserHandle.USER_CURRENT) == 1;
                        break;
                    case GAME_MODE_LOCK_GESTURES:
                        final boolean gestureLocked = Settings.System.getIntForUser(
                                mSystemExService.getContentResolver(),
                                GAME_MODE_LOCK_GESTURES,
                                0, UserHandle.USER_CURRENT) == 1;
                        mGestureLockedList.get(GESTURE_TYPE_BACK).first = gestureLocked;
                        mGestureLockedList.get(GESTURE_TYPE_NAVIGATION_BAR).first = gestureLocked;
                        resetGestureLockedTime();
                        break;
                    case GAME_MODE_LOCK_STATUS_BAR:
                        final boolean statusBarLocked = Settings.System.getIntForUser(
                                mSystemExService.getContentResolver(),
                                GAME_MODE_LOCK_STATUS_BAR,
                                0, UserHandle.USER_CURRENT) == 1;
                        mGestureLockedList.get(GESTURE_TYPE_STATUS_BAR).first = statusBarLocked;
                        resetGestureLockedTime();
                        break;
                    case GAME_MODE_SILENT_NOTIFICATION:
                        mSilentNotification = Settings.System.getIntForUser(
                                mSystemExService.getContentResolver(),
                                GAME_MODE_SILENT_NOTIFICATION,
                                0, UserHandle.USER_CURRENT) == 1;
                        break;
                    case GAME_MODE_SUPPRESS_FULLSCREEN_INTENT:
                        mSuppressFullscreenIntent = Settings.System.getIntForUser(
                                mSystemExService.getContentResolver(),
                                GAME_MODE_SUPPRESS_FULLSCREEN_INTENT,
                                0, UserHandle.USER_CURRENT) == 1;
                        break;
                }
                notifyGameStateChanged();
            }
        }
    }

    private GameModeController() {
        mServiceThread = new ServiceThread(TAG, THREAD_PRIORITY_DEFAULT, false);
        mServiceThread.start();
        mHandler = new Handler(mServiceThread.getLooper());

        for (int i = 0; i < GESTURE_TYPE_SIZE; i++) {
            mGestureLockedList.add(new MutablePair<Boolean, Long>(false, -1L));
        }
    }

    public void initSystemExService(SunSystemExService service) {
        mSystemExService = service;
        mSystemExService.publishBinderService(GAME_MODE_SERVICE, new GameModeManagerService());
    }

    public void onSystemServicesReady() {
        if (DEBUG_GAME) {
            Slog.d(TAG, "onSystemServicesReady");
        }
        mSettingsObserver = new SettingsObserver(mHandler);
        mSettingsObserver.observe();
        mHandler.post(() -> {
            updateSettings(UserHandle.USER_CURRENT);
            synchronized (mPackageLock) {
                initGameAppsListLocked(UserHandle.USER_CURRENT);
            }
        });
    }

    public void onUserSwitching(int newUserId) {
        if (DEBUG_GAME) {
            Slog.d(TAG, "onUserSwitching, newUserId: " + newUserId);
        }
        mHandler.post(() -> {
            updateSettings(newUserId);
            synchronized (mPackageLock) {
                initGameAppsListLocked(newUserId);
                updateGameModeState(mSystemExService.getTopFullscreenPackage(),
                        mSystemExService.getTopFullscreenTaskId());
            }
        });
    }

    public void onPackageRemoved(String packageName) {
        if (DEBUG_GAME) {
            Slog.d(TAG, "onPackageRemoved, packageName: " + packageName);
        }
        mHandler.post(() -> {
            synchronized (mPackageLock) {
                if (mGamePackages.contains(packageName)) {
                    if (DEBUG_GAME) {
                        Slog.d(TAG, "removeGame: " + packageName);
                    }
                    mGamePackages.remove(packageName);
                    saveGameListIntoSettingsLocked();
                    updateGameModeState(mSystemExService.getTopFullscreenPackage(),
                            mSystemExService.getTopFullscreenTaskId());
                }
            }
        });
    }

    public void onScreenOff() {
        mHandler.post(() -> {
            synchronized (mPackageLock) {
                updateGameModeState("", INVALID_TASK_ID);
            }
        });
    }

    public void onTopFullscreenPackageChanged(String packageName, int taskId) {
        mHandler.post(() -> {
            synchronized (mPackageLock) {
                updateGameModeState(packageName, taskId);
            }
        });
    }

    private void updateGameModeState(String packageName, int taskId) {
        synchronized (mStateLock) {
            final boolean isGame = mGamePackages.contains(packageName);
            if (isGame == mInGame && !mInGame) {
                return;
            }
            mInGame = isGame;
            mGamePackage = mInGame ? packageName : "";
            mGameTaskId = mInGame ? taskId : INVALID_TASK_ID;
            if (DEBUG_GAME) {
                Slog.d(TAG, "updateGameModeState, inGame=" + mInGame +
                        ", package=" + mGamePackage + ", taskId=" + mGameTaskId);
            }
            DisplayFeatureController.getInstance().onGameStateChanged(mInGame);
            resetGestureLockedTime();
            notifyGameStateChanged();
        }
    }

    private void updateSettings(int userId) {
        synchronized (mStateLock) {
            mCallAction = Settings.System.getIntForUser(
                    mSystemExService.getContentResolver(),
                    GAME_MODE_CALL_ACTION,
                    IN_GAME_CALL_NO_ACTION, userId);
            mDanmakuNotification = Settings.System.getIntForUser(
                    mSystemExService.getContentResolver(),
                    GAME_MODE_DANMAKU_NOTIFICATION,
                    1, userId) == 1;
            mDisableAutoBrightness = Settings.System.getIntForUser(
                    mSystemExService.getContentResolver(),
                    GAME_MODE_DISABLE_AUTO_BRIGHTNESS,
                    0, userId) == 1;
            mDisableHeadsUp = Settings.System.getIntForUser(
                    mSystemExService.getContentResolver(),
                    GAME_MODE_DISABLE_HEADS_UP,
                    0, userId) == 1;
            mDisableThreeFingerGestures = Settings.System.getIntForUser(
                    mSystemExService.getContentResolver(),
                    GAME_MODE_DISABLE_THREE_FINGER_GESTURES,
                    1, userId) == 1;
            mSilentNotification = Settings.System.getIntForUser(
                    mSystemExService.getContentResolver(),
                    GAME_MODE_SILENT_NOTIFICATION,
                    0, userId) == 1;
            mSuppressFullscreenIntent = Settings.System.getIntForUser(
                    mSystemExService.getContentResolver(),
                    GAME_MODE_SUPPRESS_FULLSCREEN_INTENT,
                    0, userId) == 1;

            final boolean gestureLocked = Settings.System.getIntForUser(
                    mSystemExService.getContentResolver(),
                    GAME_MODE_LOCK_STATUS_BAR,
                    0, userId) == 1;
            final boolean statusBarLocked = Settings.System.getIntForUser(
                    mSystemExService.getContentResolver(),
                    GAME_MODE_LOCK_GESTURES,
                    0, userId) == 1;
            mGestureLockedList.get(GESTURE_TYPE_BACK).first = gestureLocked;
            mGestureLockedList.get(GESTURE_TYPE_NAVIGATION_BAR).first = gestureLocked;
            mGestureLockedList.get(GESTURE_TYPE_STATUS_BAR).first = statusBarLocked;
            resetGestureLockedTime();
        }
    }

    private void resetGestureLockedTime() {
        for (MutablePair<Boolean, Long> p : mGestureLockedList) {
            p.second = -1L;
        }
        mLastGestureUnlockTime = -1L;
    }

    private GameModeInfo buildGameModeInfoLocked() {
        return new GameModeInfo.Builder()
                .setInGame(mInGame)
                .setGamePackage(mGamePackage)
                .setGameTaskId(mGameTaskId)
                .setCallAction(mCallAction)
                .setDanmakuNotificationEnabled(mDanmakuNotification)
                .setDisableAutoBrightness(mDisableAutoBrightness)
                .setDisableHeadsUp(mDisableHeadsUp)
                .setDisableThreeFingerGesture(mDisableThreeFingerGestures)
                .setLockGesture(mGestureLockedList.get(GESTURE_TYPE_BACK).first)
                .setLockStatusbar(mGestureLockedList.get(GESTURE_TYPE_STATUS_BAR).first)
                .setMuteNotification(mSilentNotification)
                .setSuppressFullscreenIntent(mSuppressFullscreenIntent)
                .build();
    }

    private void notifyGameStateChanged() {
        if (DEBUG_GAME) {
            Slog.d(TAG, "notifyGameStateChanged, info: " + buildGameModeInfoLocked());
        }
        synchronized (mListenerLock) {
            for (GameModeInfoListener listener : mListeners) {
                try {
                    listener.mListener.onGameModeInfoChanged();
                } catch (RemoteException | RuntimeException e) {
                    Slog.e(TAG, "Failed to notify game state changed");
                }
            }
        }
    }

    private void initGameAppsListLocked(int userId) {
        mGamePackages.clear();
        final String settings = Settings.System.getStringForUser(
                mSystemExService.getContentResolver(),
                GAME_MODE_APP_LIST, userId);
        if (TextUtils.isEmpty(settings)) {
            return;
        }
        final String[] apps = settings.split(";");
        for (String app : apps) {
            mGamePackages.add(app);
            if (DEBUG_GAME) {
                Slog.d(TAG, "initGameAppsListLocked, added packageName: " + app);
            }
        }
    }

    private void saveGameListIntoSettingsLocked() {
        StringBuilder sb = new StringBuilder();
        for (String app : mGamePackages) {
            sb.append(app).append(";");
        }
        Settings.System.putStringForUser(mSystemExService.getContentResolver(),
                GAME_MODE_APP_LIST, sb.toString(), UserHandle.USER_CURRENT);
        if (DEBUG_GAME) {
                Slog.d(TAG, "saveGameListIntoSettingsLocked, list: " + sb.toString());
            }
    }

    public boolean isInGame() {
        synchronized (mStateLock) {
            return mInGame;
        }
    }

    public boolean shouldDisableThreeFingerGestures() {
        synchronized (mStateLock) {
            return mInGame && mDisableThreeFingerGestures;
        }
    }

    public boolean shouldSilentNotification() {
        synchronized (mStateLock) {
            return mInGame && mSilentNotification;
        }
    }

    private boolean shouldLockGestures(int gestureType) {
        if (TopActivityRecorder.getInstance().hasMiniWindow()) {
            return false;
        }
        synchronized (mStateLock) {
            return mInGame && mGestureLockedList.get(gestureType).first;
        }
    }

    public boolean isGestureLocked(int gestureType) {
        if (gestureType < 0 || gestureType >= mGestureLockedList.size()) {
            return false;
        }
        if (!shouldLockGestures(gestureType)) {
            return false;
        }
        final long now = SystemClock.uptimeMillis();
        if (now - mLastGestureUnlockTime <= GESTURE_AUTO_LOCK_INTERVAL) {
            mLastGestureUnlockTime = now;
            return false;
        }
        final MutablePair<Boolean, Long> p = mGestureLockedList.get(gestureType);
        if (now - p.second > GESTURE_UPDATE_SWIPE_INTERVAL) {
            p.second = now;
            return true;
        }
        p.second = now;
        mLastGestureUnlockTime = now;
        return false;
    }

    public void warnGestureLocked() {
        mUiHandler.post(() -> {
            if (mGestureLockedToast != null) {
                mGestureLockedToast.cancel();
            }
            mGestureLockedToast = Toast.makeText(
                    ActivityThread.currentActivityThread().getSystemUiContext(),
                    R.string.gesture_locked_warning, Toast.LENGTH_SHORT);
            mGestureLockedToast.show();
        });
    }
}
