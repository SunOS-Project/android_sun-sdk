/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.server.policy.gesture;

import static org.nameless.os.DebugConstants.DEBUG_TOUCH_GESTURE;
import static org.nameless.provider.SettingsExt.System.TOUCH_GESTURE_M;
import static org.nameless.provider.SettingsExt.System.TOUCH_GESTURE_MUSIC_CONTROL;
import static org.nameless.provider.SettingsExt.System.TOUCH_GESTURE_O;
import static org.nameless.provider.SettingsExt.System.TOUCH_GESTURE_S;
import static org.nameless.provider.SettingsExt.System.TOUCH_GESTURE_SINGLE_TAP_SHOW_AMBIENT;
import static org.nameless.provider.SettingsExt.System.TOUCH_GESTURE_V;
import static org.nameless.provider.SettingsExt.System.TOUCH_GESTURE_W;
import static org.nameless.server.policy.gesture.TouchGestureActionTrigger.ACTION_LAST_SONG;
import static org.nameless.server.policy.gesture.TouchGestureActionTrigger.ACTION_NEXT_SONG;
import static org.nameless.server.policy.gesture.TouchGestureActionTrigger.ACTION_NONE;
import static org.nameless.server.policy.gesture.TouchGestureActionTrigger.ACTION_PLAY_PAUSE_SONG;
import static org.nameless.server.policy.gesture.TouchGestureActionTrigger.ACTION_SHOW_AMBIENT_DISPLAY;
import static org.nameless.server.policy.gesture.TouchGestureActionTrigger.actionToString;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.Slog;

import org.nameless.hardware.TouchGestureManager;

public class TouchGestureController {

    private static class InstanceHolder {
        private static final TouchGestureController INSTANCE = new TouchGestureController();
    }

    public static TouchGestureController getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private static final String TAG = "TouchGestureController";

    private final ArrayMap<Integer, Integer> mGestureActionMap = new ArrayMap<>();

    private final ContentObserver mObserver;
    private final Handler mHandler;
    private final HandlerThread mHandlerThread;

    private ContentResolver mResolver;
    private Context mContext;
    private TouchGestureActionTrigger mActionTrigger;

    private boolean mSystemReady = false;

    private TouchGestureController() {
        mHandlerThread = new HandlerThread("TouchGestureController-handler");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
        mObserver = new ContentObserver(mHandler) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                switch (uri.getLastPathSegment()) {
                    case TOUCH_GESTURE_SINGLE_TAP_SHOW_AMBIENT:
                        updateSingleTap();
                        break;
                    case TOUCH_GESTURE_MUSIC_CONTROL:
                        updateMusicControl();
                        break;
                    case TOUCH_GESTURE_M:
                        updateDrawM();
                        break;
                    case TOUCH_GESTURE_O:
                        updateDrawO();
                        break;
                    case TOUCH_GESTURE_S:
                        updateDrawS();
                        break;
                    case TOUCH_GESTURE_V:
                        updateDrawV();
                        break;
                    case TOUCH_GESTURE_W:
                        updateDrawW();
                        break;
                }
            }
        };
    }

    public void init(Context context) {
        mContext = context;
        mResolver = context.getContentResolver();
    }

    public void systemReady() {
        if (DEBUG_TOUCH_GESTURE) {
            Slog.d(TAG, "systemReady");
        }
        mSystemReady = true;
        mActionTrigger = new TouchGestureActionTrigger(mContext);

        if (TouchGestureManager.isSingleTapSupported()) {
            if (DEBUG_TOUCH_GESTURE) {
                Slog.d(TAG, "init, single tap supported");
            }
            updateSingleTap();
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    TOUCH_GESTURE_SINGLE_TAP_SHOW_AMBIENT), false,
                    mObserver, UserHandle.USER_ALL);
        }

        if (TouchGestureManager.isMusicControlSupported()) {
            if (DEBUG_TOUCH_GESTURE) {
                Slog.d(TAG, "init, music control supported");
            }
            updateMusicControl();
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    TOUCH_GESTURE_MUSIC_CONTROL), false,
                    mObserver, UserHandle.USER_ALL);
        }

        if (TouchGestureManager.isDrawMSupported()) {
            if (DEBUG_TOUCH_GESTURE) {
                Slog.d(TAG, "init, M supported");
            }
            updateDrawM();
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    TOUCH_GESTURE_M), false,
                    mObserver, UserHandle.USER_ALL);
        }

        if (TouchGestureManager.isDrawOSupported()) {
            if (DEBUG_TOUCH_GESTURE) {
                Slog.d(TAG, "init, O supported");
            }
            updateDrawO();
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    TOUCH_GESTURE_O), false,
                    mObserver, UserHandle.USER_ALL);
        }

        if (TouchGestureManager.isDrawSSupported()) {
            if (DEBUG_TOUCH_GESTURE) {
                Slog.d(TAG, "init, S supported");
            }
            updateDrawS();
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    TOUCH_GESTURE_S), false,
                    mObserver, UserHandle.USER_ALL);
        }

        if (TouchGestureManager.isDrawVSupported()) {
            if (DEBUG_TOUCH_GESTURE) {
                Slog.d(TAG, "init, V supported");
            }
            updateDrawV();
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    TOUCH_GESTURE_V), false,
                    mObserver, UserHandle.USER_ALL);
        }

        if (TouchGestureManager.isDrawWSupported()) {
            if (DEBUG_TOUCH_GESTURE) {
                Slog.d(TAG, "init, W supported");
            }
            updateDrawW();
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    TOUCH_GESTURE_W), false,
                    mObserver, UserHandle.USER_ALL);
        }
    }

    public boolean handleKeyEvent(int scanCode, boolean down) {
        synchronized (mGestureActionMap) {
            final int action = mGestureActionMap.getOrDefault(scanCode, -1);
            if (!down) {
                if (DEBUG_TOUCH_GESTURE) {
                    Slog.d(TAG, "handleKeyEvent, scanCode=" + scanCode + ", action=" + actionToString(action));
                }
                if (action > ACTION_NONE) {
                    mHandler.post(() -> mActionTrigger.trigger(action));
                    return true;
                }
            }
            return action != -1;
        }
    }

    public void updateSettings() {
        if (!mSystemReady) {
            return;
        }
        if (TouchGestureManager.isSingleTapSupported()) {
            updateSingleTap();
        }
        if (TouchGestureManager.isMusicControlSupported()) {
            updateMusicControl();
        }
        if (TouchGestureManager.isDrawMSupported()) {
            updateDrawM();
        }
        if (TouchGestureManager.isDrawOSupported()) {
            updateDrawO();
        }
        if (TouchGestureManager.isDrawSSupported()) {
            updateDrawS();
        }
        if (TouchGestureManager.isDrawVSupported()) {
            updateDrawV();
        }
        if (TouchGestureManager.isDrawWSupported()) {
            updateDrawW();
        }
    }

    private void updateSingleTap() {
        final boolean singleTapEnabled = Settings.System.getIntForUser(mResolver,
                TOUCH_GESTURE_SINGLE_TAP_SHOW_AMBIENT, 1, UserHandle.USER_CURRENT) == 1;
        if (DEBUG_TOUCH_GESTURE) {
            Slog.d(TAG, "updateSingleTap, singleTapEnabled=" + singleTapEnabled);
        }
        synchronized (mGestureActionMap) {
            mGestureActionMap.put(TouchGestureManager.KEY_CODE_SINGLE_TAP,
                    singleTapEnabled ? ACTION_SHOW_AMBIENT_DISPLAY : ACTION_NONE);
        } 
    }

    private void updateMusicControl() {
        final boolean musicControlEnabled = Settings.System.getIntForUser(mResolver,
                TOUCH_GESTURE_MUSIC_CONTROL, 0, UserHandle.USER_CURRENT) == 1;
        if (DEBUG_TOUCH_GESTURE) {
            Slog.d(TAG, "updateMusicControl, musicControlEnabled=" + musicControlEnabled);
        }
        synchronized (mGestureActionMap) {
            mGestureActionMap.put(TouchGestureManager.KEY_CODE_LEFT_ARROW,
                    musicControlEnabled ? ACTION_LAST_SONG : ACTION_NONE);
            mGestureActionMap.put(TouchGestureManager.KEY_CODE_RIGHT_ARROW,
                    musicControlEnabled ? ACTION_NEXT_SONG : ACTION_NONE);
            mGestureActionMap.put(TouchGestureManager.KEY_CODE_TWO_FINGERS_DOWN,
                    musicControlEnabled ? ACTION_PLAY_PAUSE_SONG : ACTION_NONE);
        }
    }

    private void updateDrawM() {
        final int action = Settings.System.getIntForUser(mResolver,
                TOUCH_GESTURE_M, ACTION_NONE, UserHandle.USER_CURRENT);
        if (DEBUG_TOUCH_GESTURE) {
            Slog.d(TAG, "updateDrawM, action=" + actionToString(action));
        }
        synchronized (mGestureActionMap) {
            mGestureActionMap.put(TouchGestureManager.KEY_CODE_M, action);
        }
    }

    private void updateDrawO() {
        final int action = Settings.System.getIntForUser(mResolver,
                TOUCH_GESTURE_O, ACTION_NONE, UserHandle.USER_CURRENT);
        synchronized (mGestureActionMap) {
            mGestureActionMap.put(TouchGestureManager.KEY_CODE_O, action);
        }
    }

    private void updateDrawS() {
        final int action = Settings.System.getIntForUser(mResolver,
                TOUCH_GESTURE_S, ACTION_NONE, UserHandle.USER_CURRENT);
        if (DEBUG_TOUCH_GESTURE) {
            Slog.d(TAG, "updateDrawS, action=" + actionToString(action));
        }
        synchronized (mGestureActionMap) {
            mGestureActionMap.put(TouchGestureManager.KEY_CODE_S, action);
        }
    }

    private void updateDrawV() {
        final int action = Settings.System.getIntForUser(mResolver,
                TOUCH_GESTURE_V, ACTION_NONE, UserHandle.USER_CURRENT);
        if (DEBUG_TOUCH_GESTURE) {
            Slog.d(TAG, "updateDrawV, action=" + actionToString(action));
        }
        synchronized (mGestureActionMap) {
            mGestureActionMap.put(TouchGestureManager.KEY_CODE_V, action);
        }
    }

    private void updateDrawW() {
        final int action = Settings.System.getIntForUser(mResolver,
                TOUCH_GESTURE_W, ACTION_NONE, UserHandle.USER_CURRENT);
        if (DEBUG_TOUCH_GESTURE) {
            Slog.d(TAG, "updateDrawW, action=" + actionToString(action));
        }
        synchronized (mGestureActionMap) {
            mGestureActionMap.put(TouchGestureManager.KEY_CODE_W, action);
        }
    }
}
