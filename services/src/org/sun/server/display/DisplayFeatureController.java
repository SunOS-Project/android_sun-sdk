/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.server.display;

import static android.os.Process.THREAD_PRIORITY_DEFAULT;

import static org.sun.os.DebugConstants.DEBUG_DISPLAY_FEATURE;
import static org.sun.provider.SettingsExt.System.DC_DIMMING_STATE;
import static org.sun.provider.SettingsExt.System.HIGH_TOUCH_SAMPLE_MODE;
import static org.sun.provider.SettingsExt.System.LTPO_ENABLED;
import static org.sun.provider.SettingsExt.System.UNLIMIT_EDGE_TOUCH_MODE;

import static vendor.sun.hardware.displayfeature.Feature.DC_DIMMING;
import static vendor.sun.hardware.displayfeature.Feature.EDGE_TOUCH;
import static vendor.sun.hardware.displayfeature.Feature.HIGH_SAMPLE_TOUCH;
import static vendor.sun.hardware.displayfeature.Feature.LTPO;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Slog;

import com.android.server.ServiceThread;
import com.android.server.policy.PhoneWindowManagerExt;

import org.sun.display.DisplayFeatureManager;
import org.sun.server.SunSystemExService;
import org.sun.server.app.GameModeController;

public class DisplayFeatureController {

    private static final String TAG = "DisplayFeatureController";

    private final Handler mHandler;
    private final ServiceThread mServiceThread;

    private DisplayFeatureManager mDisplayFeatureManager;
    private SunSystemExService mSystemExService;

    private DcDimmingController mDcDimmingController;
    private EdgeTouchController mEdgeTouchController;
    private HighTouchSampleController mHighTouchSampleController;
    private LtpoController mLtpoController;

    private SettingsObserver mSettingsObserver;

    private boolean mDelayedUpdateGameState;

    private static class InstanceHolder {
        private static DisplayFeatureController INSTANCE = new DisplayFeatureController();
    }

    public static DisplayFeatureController getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private final class SettingsObserver extends ContentObserver {

        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            if (mDcDimmingController != null) {
                mSystemExService.getContentResolver().registerContentObserver(
                        Settings.System.getUriFor(DC_DIMMING_STATE),
                        false, this, UserHandle.USER_ALL);
            }
            if (mEdgeTouchController != null) {
                mSystemExService.getContentResolver().registerContentObserver(
                        Settings.System.getUriFor(UNLIMIT_EDGE_TOUCH_MODE),
                        false, this, UserHandle.USER_ALL);
            }
            if (mHighTouchSampleController != null) {
                mSystemExService.getContentResolver().registerContentObserver(
                        Settings.System.getUriFor(HIGH_TOUCH_SAMPLE_MODE),
                        false, this, UserHandle.USER_ALL);
            }
            if (mLtpoController != null) {
                mSystemExService.getContentResolver().registerContentObserver(
                        Settings.System.getUriFor(LTPO_ENABLED),
                        false, this, UserHandle.USER_ALL);
            }
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            switch (uri.getLastPathSegment()) {
                case DC_DIMMING_STATE:
                    mDcDimmingController.updateSettings();
                    break;
                case UNLIMIT_EDGE_TOUCH_MODE:
                    mEdgeTouchController.updateSettings();
                    break;
                case HIGH_TOUCH_SAMPLE_MODE:
                    mHighTouchSampleController.updateSettings();
                    break;
                case LTPO_ENABLED:
                    mLtpoController.updateSettings();
                    break;
            }
        }
    }

    private DisplayFeatureController() {
        mServiceThread = new ServiceThread(TAG, THREAD_PRIORITY_DEFAULT, false);
        mServiceThread.start();
        mHandler = new Handler(mServiceThread.getLooper());
    }

    public void initSystemExService(SunSystemExService service) {
        mSystemExService = service;
        mDisplayFeatureManager = DisplayFeatureManager.getInstance();
    }

    public void onSystemServicesReady() {
        mHandler.post(() -> {
            logD("onSystemServicesReady");

            if (mDisplayFeatureManager.hasFeature(DC_DIMMING)) {
                mDcDimmingController = new DcDimmingController(
                        mSystemExService.getContentResolver(), mDisplayFeatureManager);
            } else {
                mDcDimmingController = null;
                logD("DcDimmingController is not supported");
            }

            if (mDisplayFeatureManager.hasFeature(EDGE_TOUCH)) {
                mEdgeTouchController = new EdgeTouchController(
                        mSystemExService.getContentResolver(), mDisplayFeatureManager);
            } else {
                mEdgeTouchController = null;
                logD("EdgeTouchController is not supported");
            }

            if (mDisplayFeatureManager.hasFeature(HIGH_SAMPLE_TOUCH)) {
                mHighTouchSampleController = new HighTouchSampleController(
                        mSystemExService.getContentResolver(), mDisplayFeatureManager);
            } else {
                mHighTouchSampleController = null;
                logD("HighTouchSampleController is not supported");
            }

            if (mDisplayFeatureManager.hasFeature(LTPO)) {
                mLtpoController = new LtpoController(
                        mSystemExService.getContentResolver(), mDisplayFeatureManager);
            } else {
                mLtpoController = null;
                logD("LtpoController is not supported");
            }

            mSettingsObserver = new SettingsObserver(mHandler);
            mSettingsObserver.observe();
        });
    }

    public void onBootCompleted() {
        mHandler.post(() -> {
            logD("onBootCompleted");

            if (mDcDimmingController != null) {
                mDcDimmingController.onBootCompleted();
            }
            if (mEdgeTouchController != null) {
                mEdgeTouchController.onBootCompleted();
            }
            if (mHighTouchSampleController != null) {
                mHighTouchSampleController.onBootCompleted();
            }
            if (mLtpoController != null) {
                mLtpoController.onBootCompleted();
            }
        });
    }

    public void onGameStateChanged(boolean inGame) {
        if (PhoneWindowManagerExt.getInstance().isTouching()) {
            logD("Delay game state update due to in touching");
            mDelayedUpdateGameState = true;
            return;
        }
        mHandler.post(() -> {
            if (mEdgeTouchController != null) {
                mEdgeTouchController.onGameStateChanged(inGame);
            }
            if (mHighTouchSampleController != null) {
                mHighTouchSampleController.onGameStateChanged(inGame);
            }
            if (mLtpoController != null) {
                mLtpoController.onGameStateChanged(inGame);
            }
        });
    }

    public void maybeUpdateGameState() {
        if (mDelayedUpdateGameState) {
            mDelayedUpdateGameState = false;
            mHandler.post(() -> {
                final boolean inGame = GameModeController.getInstance().isInGame();
                if (mEdgeTouchController != null) {
                    mEdgeTouchController.onGameStateChanged(inGame);
                }
                if (mHighTouchSampleController != null) {
                    mHighTouchSampleController.onGameStateChanged(inGame);
                }
                if (mLtpoController != null) {
                    mLtpoController.onGameStateChanged(inGame);
                }
            });
        }
    }

    public void onScreenOn() {
        mHandler.post(() -> {
            if (mLtpoController != null) {
                mLtpoController.onScreenOn();
            }
        });
    }

    private static void logD(String msg) {
        if (DEBUG_DISPLAY_FEATURE) {
            Slog.d(TAG, msg);
        }
    }

    public static void logD(String tag, String msg) {
        if (DEBUG_DISPLAY_FEATURE) {
            Slog.d(TAG + "::" + tag, msg);
        }
    }

    static void logE(String tag, String msg) {
        Slog.e(TAG + "::" + tag, msg);
    }
}
