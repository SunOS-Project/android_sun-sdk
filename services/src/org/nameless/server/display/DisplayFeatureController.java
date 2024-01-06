/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.server.display;

import static org.nameless.os.DebugConstants.DEBUG_DISPLAY_FEATURE;
import static org.nameless.provider.SettingsExt.System.DC_DIMMING_STATE;
import static org.nameless.provider.SettingsExt.System.HIGH_TOUCH_SAMPLE_MODE;
import static org.nameless.provider.SettingsExt.System.UNLIMIT_EDGE_TOUCH_MODE;

import static vendor.nameless.hardware.displayfeature.V1_0.Feature.DC_DIMMING;
import static vendor.nameless.hardware.displayfeature.V1_0.Feature.EDGE_TOUCH;
import static vendor.nameless.hardware.displayfeature.V1_0.Feature.HIGH_SAMPLE_TOUCH;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Slog;

import org.nameless.display.DisplayFeatureManager;
import org.nameless.server.NamelessSystemExService;

public class DisplayFeatureController {

    private static final String TAG = "DisplayFeatureController";

    private final Object mLock = new Object();

    private DisplayFeatureManager mDisplayFeatureManager;
    private NamelessSystemExService mSystemExService;

    private DcDimmingController mDcDimmingController;
    private EdgeTouchController mEdgeTouchController;
    private HighTouchSampleController mHighTouchSampleController;

    private SettingsObserver mSettingsObserver;

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
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            synchronized (mLock) {
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
                }
            }
        }
    }

    public void initSystemExService(NamelessSystemExService service) {
        mSystemExService = service;
        mDisplayFeatureManager = DisplayFeatureManager.getInstance();
    }

    public void onSystemServicesReady() {
        synchronized (mLock) {
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

            mSettingsObserver = new SettingsObserver(mSystemExService.getHandler());
            mSettingsObserver.observe();
        }
    }

    public void onBootCompleted() {
        synchronized (mLock) {
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
        }
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

    public static void logE(String tag, String msg) {
        Slog.e(TAG + "::" + tag, msg);
    }
}
