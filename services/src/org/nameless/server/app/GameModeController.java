/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.server.app;

import static android.os.Process.THREAD_PRIORITY_DEFAULT;

import static org.nameless.content.ContextExt.GAME_MODE_SERVICE;
import static org.nameless.os.DebugConstants.DEBUG_GAME;
import static org.nameless.provider.SettingsExt.System.GAME_MODE_APP_LIST;
import static org.nameless.provider.SettingsExt.System.GAME_MODE_DISABLE_AUTO_BRIGHTNESS;
import static org.nameless.provider.SettingsExt.System.GAME_MODE_DISABLE_HEADS_UP;
import static org.nameless.provider.SettingsExt.System.GAME_MODE_DISABLE_THREE_FINGER_GESTURES;
import static org.nameless.provider.SettingsExt.System.GAME_MODE_DISABLE_POP_UP_VIEW_GESTURE;
import static org.nameless.provider.SettingsExt.System.GAME_MODE_LOCK_GESTURES;
import static org.nameless.provider.SettingsExt.System.GAME_MODE_LOCK_STATUS_BAR;
import static org.nameless.provider.SettingsExt.System.GAME_MODE_SILENT_NOTIFICATION;
import static org.nameless.provider.SettingsExt.System.GAME_MODE_SUPPRESS_FULLSCREEN_INTENT;

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
import android.util.Slog;
import android.view.WindowInsets.Type;
import android.widget.Toast;

import com.android.internal.R;

import com.android.server.ServiceThread;
import com.android.server.UiThread;
import com.android.server.wm.TopActivityRecorder;

import java.util.ArrayList;
import java.util.HashSet;

import org.nameless.app.GameModeInfo;
import org.nameless.app.IGameModeInfoListener;
import org.nameless.app.IGameModeManagerService;
import org.nameless.server.NamelessSystemExService;
import org.nameless.server.display.DisplayFeatureController;

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

    private final HashSet<String> mGamePackages = new HashSet<>();

    private final ArrayList<GameModeInfoListener> mListeners = new ArrayList<>();

    private NamelessSystemExService mSystemExService;
    private SettingsObserver mSettingsObserver;

    private boolean mInGame;
    private boolean mDisableAutoBrightness;
    private boolean mDisableHeadsUp;
    private boolean mDisableThreeFingerGestures;
    private boolean mDisablePopUpViewGesture;
    private boolean mLockGestures;
    private boolean mLockStatusbar;
    private boolean mSilentNotification;
    private boolean mSuppressFullscreenIntent;

    private long mLastGestureSwipeTime = -1L;
    private long mLastGestureUnlockedTime = -1L;

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
                updateGameModeState(mSystemExService.getTopFullscreenPackage());
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
                updateGameModeState(mSystemExService.getTopFullscreenPackage());
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
                    Settings.System.getUriFor(GAME_MODE_DISABLE_AUTO_BRIGHTNESS),
                    false, this, UserHandle.USER_ALL);
            mSystemExService.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(GAME_MODE_DISABLE_HEADS_UP),
                    false, this, UserHandle.USER_ALL);
            mSystemExService.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(GAME_MODE_DISABLE_THREE_FINGER_GESTURES),
                    false, this, UserHandle.USER_ALL);
            mSystemExService.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(GAME_MODE_DISABLE_POP_UP_VIEW_GESTURE),
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
                    case GAME_MODE_DISABLE_POP_UP_VIEW_GESTURE:
                        mDisablePopUpViewGesture = Settings.System.getIntForUser(
                                mSystemExService.getContentResolver(),
                                GAME_MODE_DISABLE_POP_UP_VIEW_GESTURE,
                                0, UserHandle.USER_CURRENT) == 1;
                        break;
                    case GAME_MODE_LOCK_GESTURES:
                        mLockGestures = Settings.System.getIntForUser(
                                mSystemExService.getContentResolver(),
                                GAME_MODE_LOCK_GESTURES,
                                0, UserHandle.USER_CURRENT) == 1;
                        mLastGestureSwipeTime = -1L;
                        mLastGestureUnlockedTime = -1L;
                        break;
                    case GAME_MODE_LOCK_STATUS_BAR:
                        mLockStatusbar = Settings.System.getIntForUser(
                                mSystemExService.getContentResolver(),
                                GAME_MODE_LOCK_STATUS_BAR,
                                0, UserHandle.USER_CURRENT) == 1;
                        mLastGestureSwipeTime = -1L;
                        mLastGestureUnlockedTime = -1L;
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
            }
        }
    }

    private GameModeController() {
        mServiceThread = new ServiceThread(TAG, THREAD_PRIORITY_DEFAULT, false);
        mServiceThread.start();
        mHandler = new Handler(mServiceThread.getLooper());
    }

    public void initSystemExService(NamelessSystemExService service) {
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
                updateGameModeState(mSystemExService.getTopFullscreenPackage());
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
                    updateGameModeState(mSystemExService.getTopFullscreenPackage());
                }
            }
        });
    }

    public void onScreenOff() {
        mHandler.post(() -> {
            synchronized (mPackageLock) {
                updateGameModeState("");
            }
        });
    }

    public void onTopFullscreenPackageChanged(String packageName) {
        mHandler.post(() -> {
            synchronized (mPackageLock) {
                updateGameModeState(packageName);
            }
        });
    }

    private void updateGameModeState(String packageName) {
        synchronized (mStateLock) {
            final boolean isGame = mGamePackages.contains(packageName);
            if (isGame == mInGame) {
                return;
            }
            mInGame = isGame;
            if (DEBUG_GAME) {
                Slog.d(TAG, "updateGameModeState, inGame=" + mInGame);
            }
            DisplayFeatureController.getInstance().onGameStateChanged(mInGame);
            notifyGameStateChanged();
        }
    }

    private void updateSettings(int userId) {
        synchronized (mStateLock) {
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
            mDisablePopUpViewGesture = Settings.System.getIntForUser(
                    mSystemExService.getContentResolver(),
                    GAME_MODE_DISABLE_POP_UP_VIEW_GESTURE,
                    0, userId) == 1;
            mLockGestures = Settings.System.getIntForUser(
                    mSystemExService.getContentResolver(),
                    GAME_MODE_LOCK_GESTURES,
                    0, userId) == 1;
            mLockStatusbar = Settings.System.getIntForUser(
                    mSystemExService.getContentResolver(),
                    GAME_MODE_LOCK_STATUS_BAR,
                    0, userId) == 1;
            mSilentNotification = Settings.System.getIntForUser(
                    mSystemExService.getContentResolver(),
                    GAME_MODE_SILENT_NOTIFICATION,
                    0, userId) == 1;
            mSuppressFullscreenIntent = Settings.System.getIntForUser(
                    mSystemExService.getContentResolver(),
                    GAME_MODE_SUPPRESS_FULLSCREEN_INTENT,
                    0, userId) == 1;
            mLastGestureSwipeTime = -1L;
            mLastGestureUnlockedTime = -1L;
        }
    }

    public GameModeInfo buildGameModeInfoLocked() {
        return new GameModeInfo.Builder()
                .setInGame(mInGame)
                .setDisableAutoBrightness(mDisableAutoBrightness)
                .setDisableHeadsUp(mDisableHeadsUp)
                .setSuppressFullscreenIntent(mSuppressFullscreenIntent)
                .build();
    }

    private void notifyGameStateChanged() {
        final GameModeInfo info = buildGameModeInfoLocked();
        if (DEBUG_GAME) {
            Slog.d(TAG, "notifyGameStateChanged, info: " + info);
        }
        for (GameModeInfoListener listener : mListeners) {
            try {
                listener.mListener.onGameModeInfoChanged(info);
            } catch (RemoteException | RuntimeException e) {
                Slog.e(TAG, "Failed to notify game state changed");
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

    public boolean shouldDisablePopUpViewGesture() {
        synchronized (mStateLock) {
            return mInGame && mDisablePopUpViewGesture;
        }
    }

    public boolean shouldSilentNotification() {
        synchronized (mStateLock) {
            return mInGame && mSilentNotification;
        }
    }

    private boolean shouldLockGestures() {
        if (TopActivityRecorder.getInstance().hasMiniWindow()) {
            return false;
        }
        synchronized (mStateLock) {
            return mInGame && mLockGestures;
        }
    }

    public boolean shouldLockStatusbar() {
        if (TopActivityRecorder.getInstance().hasMiniWindow()) {
            return false;
        }
        synchronized (mStateLock) {
            return mInGame && mLockStatusbar;
        }
    }

    public boolean isGestureLocked(boolean updateSwipeTime) {
        if (!shouldLockGestures()) {
            return false;
        }
        final long now = SystemClock.uptimeMillis();
        if (now - mLastGestureUnlockedTime <= GESTURE_AUTO_LOCK_INTERVAL) {
            mLastGestureUnlockedTime = now;
            return false;
        }
        if (now - mLastGestureSwipeTime > GESTURE_UPDATE_SWIPE_INTERVAL) {
            if (updateSwipeTime) {
                mLastGestureSwipeTime = now;
            }
            return true;
        }
        mLastGestureUnlockedTime = now;
        return false;
    }

    public boolean interceptShowTransient(int type, boolean swipeOnStatusBar,
            boolean lockedGestures, boolean lockedStatusbar) {
        if ((type & Type.statusBars()) != 0) {
            if (lockedStatusbar && swipeOnStatusBar) {
                return true;
            }
            if (lockedGestures && !swipeOnStatusBar) {
                return true;
            }
        }
        if ((type & Type.navigationBars()) != 0) {
            if (lockedGestures || (lockedStatusbar && swipeOnStatusBar)) {
                return true;
            }
        }
        return false;
    }

    public void warnGestureLocked() {
        mUiHandler.post(() -> {
            Toast.makeText(ActivityThread.currentActivityThread().getSystemUiContext(),
                    R.string.gesture_locked_warning, Toast.LENGTH_SHORT).show();
        });
    }
}
