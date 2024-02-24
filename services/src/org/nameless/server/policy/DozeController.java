/*
 * Copyright (C) 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.server.policy;

import static android.os.Process.THREAD_PRIORITY_DEFAULT;

import static org.nameless.os.DebugConstants.DEBUG_DOZE;
import static org.nameless.provider.SettingsExt.System.DOZE_PICK_UP_ACTION;

import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Slog;

import com.android.internal.util.nameless.DozeHelper;

import com.android.server.ServiceThread;

import org.nameless.server.NamelessSystemExService;
import org.nameless.server.policy.sensor.PickUpSensor;

public class DozeController {

    private static final String TAG = "DozeController";

    public static final String DOZE_INTENT = "com.android.systemui.doze.pulse";

    private static final long WAKELOCK_TIMEOUT_MS = 300L;

    private final Object mLock = new Object();

    private final Handler mHandler;
    private final ServiceThread mServiceThread;

    private PickUpSensor mPickUpSensor;

    private NamelessSystemExService mSystemExService;
    private PowerManager mPowerManager;
    private SettingsObserver mSettingsObserver;
    private WakeLock mWakeLock;

    private static class InstanceHolder {
        private static DozeController INSTANCE = new DozeController();
    }

    public static DozeController getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private final class SettingsObserver extends ContentObserver {

        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            if (mPickUpSensor != null && mPickUpSensor.isSupported()) {
                mSystemExService.getContentResolver().registerContentObserver(
                        Settings.System.getUriFor(DOZE_PICK_UP_ACTION),
                        false, this, UserHandle.USER_ALL);
            }
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            switch (uri.getLastPathSegment()) {
                case DOZE_PICK_UP_ACTION:
                    mPickUpSensor.updateSettings();
                    break;
            }
        }
    }

    private DozeController() {
        mServiceThread = new ServiceThread(TAG, THREAD_PRIORITY_DEFAULT, false);
        mServiceThread.start();
        mHandler = new Handler(mServiceThread.getLooper());
    }

    public void initSystemExService(NamelessSystemExService service) {
        mSystemExService = service;
    }

    public void onSystemServicesReady() {
        mHandler.post(() -> {
            logD("onSystemServicesReady");

            mPowerManager = mSystemExService.getContext().getSystemService(PowerManager.class);
            mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);

            final String pickUpSensorType = DozeHelper.getPickUpSensorType(mSystemExService.getContext());
            final float pickUpSensorValue = DozeHelper.getPickUpSensorValue(mSystemExService.getContext());
            if (!TextUtils.isEmpty(pickUpSensorType) && pickUpSensorValue >= 0.0f) {
                mPickUpSensor = new PickUpSensor(mSystemExService.getContext(),
                        pickUpSensorType, pickUpSensorValue);
            } else {
                mPickUpSensor = null;
                logD("PickUpSensor is not supported");
            }

            mSettingsObserver = new SettingsObserver(mHandler);
            mSettingsObserver.observe();
        });
    }

    public void onUserSwitching(int newUserId) {
        mHandler.post(() -> {
            logD("onUserSwitching, newUserId: " + newUserId);

            if (mPickUpSensor != null && mPickUpSensor.isSupported()) {
                mPickUpSensor.onUserSwitching(newUserId);
            }
        });
    }

    public void onScreenOff() {
        mHandler.post(() -> {
            logD("onScreenOff");

            if (mPickUpSensor != null && mPickUpSensor.isSupported()) {
                mPickUpSensor.onScreenOff();
            }
        });
    }

    public void onScreenOn() {
        mHandler.post(() -> {
            logD("onScreenOn");

            if (mPickUpSensor != null && mPickUpSensor.isSupported()) {
                mPickUpSensor.onScreenOn();
            }
        });
    }

    public void launchDozePulse() {
        mHandler.post(() -> {
            mSystemExService.getContext().sendBroadcastAsUser(new Intent(DOZE_INTENT), UserHandle.SYSTEM);
        });
    }

    public void wakeUpScreen(boolean vibrate) {
        mHandler.post(() -> {
            mWakeLock.acquire(WAKELOCK_TIMEOUT_MS);
            if (vibrate) {
                mPowerManager.wakeUp(SystemClock.uptimeMillis(), PowerManager.WAKE_REASON_GESTURE, TAG);
            } else {
                mPowerManager.wakeUp(SystemClock.uptimeMillis(), PowerManager.WAKE_REASON_CAMERA_LAUNCH, TAG);
            }
        });
    }

    private static void logD(String msg) {
        if (DEBUG_DOZE) {
            Slog.d(TAG, msg);
        }
    }

    public static void logD(String tag, String msg) {
        if (DEBUG_DOZE) {
            Slog.d(TAG + "::" + tag, msg);
        }
    }
}
