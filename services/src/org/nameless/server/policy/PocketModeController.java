/*
 * Copyright (C) 2016 The ParanoidAndroid Project
 * Copyright (C) 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.server.policy;

import static android.os.Process.THREAD_PRIORITY_DEFAULT;

import static org.nameless.os.DebugConstants.DEBUG_POCKET;
import static org.nameless.provider.SettingsExt.System.POCKET_JUDGE;

import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyCallback.CallStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Slog;

import com.android.internal.R;

import com.android.server.FgThread;
import com.android.server.ServiceThread;

import org.nameless.server.NamelessSystemExService;

public class PocketModeController {

    private static final String TAG = "PocketModeController";

    private static final float POCKET_LIGHT_MAX_THRESHOLD = 3.0f;

    private static final long REGISTER_SENSORS_DELAY = 50L;
    private static final long SCREEN_OFF_TIMEOUT = 10000L;

    private final Handler mHandler;
    private final ServiceThread mServiceThread;

    private NamelessSystemExService mSystemExService;
    private PowerManager mPowerManager;
    private SensorManager mSensorManager;
    private SettingsObserver mSettingsObserver;
    private TelephonyManager mTelephonyManager;

    private final SensorEventListener mProximityListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent event) {
            final boolean wasInPocket = mProximityInPocket;
            if (event == null || event.values == null || event.values.length == 0) {
                mProximityInPocket = false;
            } else {
                mProximityInPocket = event.values[0] < mProximityMaxRange;
            }
            if (wasInPocket != mProximityInPocket) {
                logD("mProximityInPocket changed to: " + mProximityInPocket);
                updatePocketState();
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };
    private Sensor mProximitySensor;
    private float mProximityMaxRange;
    private boolean mProximityRegistered;
    private boolean mProximityInPocket;

    private PocketLock mPocketLockView;

    private final SensorEventListener mVendorListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent event) {
            final boolean wasInPocket = mVendorInPocket;
            if (event == null || event.values == null || event.values.length == 0) {
                mVendorInPocket = false;
            } else {
                mVendorInPocket = event.values[0] == 1.0;
            }
            if (wasInPocket != mVendorInPocket) {
                logD("mVendorInPocket changed to: " + mVendorInPocket);
                updatePocketState();
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };
    private Sensor mVendorSensor;
    private boolean mVendorRegistered;
    private boolean mVendorInPocket;

    private final SensorEventListener mLightListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent event) {
            final boolean wasInPocket = mLightPoor;
            if (event == null || event.values == null || event.values.length == 0) {
                mLightPoor = false;
            } else {
                mLightPoor = event.values[0] >= 0 && event.values[0] < POCKET_LIGHT_MAX_THRESHOLD;
            }
            if (wasInPocket != mLightPoor) {
                logD("mLightPoor changed to: " + mLightPoor);
                updatePocketState();
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };
    private Sensor mLightSensor;
    private float mLightMaxRange;
    private boolean mLightRegistered;
    private boolean mLightPoor;

    private final Runnable mRegisterAllRunnable = () -> {
        registerAll();
    };
    private final Runnable mScreenOffRunnable = () -> {
        mPowerManager.goToSleep(SystemClock.uptimeMillis(),
                PowerManager.GO_TO_SLEEP_REASON_TIMEOUT, 0);
    };

    private final class CallStateCallback extends TelephonyCallback implements CallStateListener {
        @Override
        public void onCallStateChanged(int state) {
            if (state != TelephonyManager.CALL_STATE_IDLE) {
                logD("Call received");
                unregisterAll();
            }
        }
    }
    private final CallStateCallback mCallStateCallback = new CallStateCallback();
    private boolean mCallCallbackRegistered;

    private boolean mEnabled;
    private boolean mShowing;

    private static class InstanceHolder {
        private static PocketModeController INSTANCE = new PocketModeController();
    }

    public static PocketModeController getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private final class SettingsObserver extends ContentObserver {

        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            mSystemExService.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(POCKET_JUDGE),
                    false, this, UserHandle.USER_ALL);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (POCKET_JUDGE.equals(uri.getLastPathSegment())) {
                updateSettings(UserHandle.USER_CURRENT);
            }
        }
    }

    private PocketModeController() {
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
            mSensorManager = mSystemExService.getContext().getSystemService(SensorManager.class);
            mTelephonyManager = mSystemExService.getContext().getSystemService(TelephonyManager.class);

            final String proximitySensorType = mSystemExService.getContext().getResources()
                    .getString(R.string.config_pocketJudgeVendorProximitySensorName);
            if (!TextUtils.isEmpty(proximitySensorType)) {
                mProximitySensor = getSensor(proximitySensorType);
            } else {
                mProximitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
            }
            if (mProximitySensor != null) {
                mProximityMaxRange = mProximitySensor.getMaximumRange();
            }

            final String vendorSensorType = mSystemExService.getContext().getResources()
                    .getString(R.string.config_pocketJudgeVendorSensorName);
            if (!TextUtils.isEmpty(vendorSensorType)) {
                mVendorSensor = getSensor(vendorSensorType);
            }

            final boolean useLightSensor = mSystemExService.getContext().getResources()
                    .getBoolean(R.bool.config_pocketUseLightSensor);
            if (useLightSensor) {
                mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
                if (mLightSensor != null) {
                    mLightMaxRange = mLightSensor.getMaximumRange();
                }
            }

            mSettingsObserver = new SettingsObserver(mHandler);
            mSettingsObserver.observe();
        });
    }

    public void onBootCompleted() {
        mHandler.post(() -> {
            logD("onBootCompleted");
            mPocketLockView = new PocketLock(mSystemExService.getContext());
            updateSettings(UserHandle.USER_CURRENT);
        });
    }

    public void onUserSwitching(int newUserId) {
        mHandler.post(() -> {
            logD("onUserSwitching, newUserId: " + newUserId);
            updateSettings(newUserId);
        });
    }

    public void onScreenOff() {
        mHandler.post(() -> {
            unregisterAll();
        });
    }

    public void onScreenOn() {
        mHandler.postDelayed(mRegisterAllRunnable, REGISTER_SENSORS_DELAY);
    }

    public void onScreenUnlocked() {
        if (mHandler.hasCallbacks(mRegisterAllRunnable)) {
            mHandler.removeCallbacks(mRegisterAllRunnable);
        }
        mHandler.post(() -> {
            unregisterAll();
        });
    }

    private Sensor getSensor(String type) {
        for (Sensor sensor : mSensorManager.getSensorList(Sensor.TYPE_ALL)) {
            if (type.equals(sensor.getStringType())) {
                return sensor;
            }
        }
        return null;
    }

    private boolean isDeviceInPocket() {
        if (mProximitySensor == null && mVendorSensor == null) {
            // No sensors are working, never enter pocket mode for this case.
            logE("isDeviceInPocket, sensors are unavailable!");
            return false;
        }

        boolean inPocket = true;
        if (mProximitySensor != null) {
            inPocket &= mProximityInPocket;
        }
        if (mVendorSensor != null) {
            inPocket &= mVendorInPocket;
        }
        if (mLightSensor != null) {
            inPocket &= mLightPoor;
        }
        return inPocket;
    }

    public boolean isPocketLockShowing() {
        return mShowing;
    }

    private void registerAll() {
        if (!mEnabled) {
            return;
        }
        logD("registerAll");
        if (!mCallCallbackRegistered) {
            mTelephonyManager.registerTelephonyCallback(
                    FgThread.getExecutor(), mCallStateCallback);
            mCallCallbackRegistered = true;
        }
        if (mVendorSensor != null) {
            if (!mVendorRegistered) {
                mSensorManager.registerListener(mVendorListener, mVendorSensor,
                        SensorManager.SENSOR_DELAY_NORMAL, mHandler);
                mSensorManager.flush(mVendorListener);
                mVendorRegistered = true;
            }
        } else if (mProximitySensor != null && !mProximityRegistered) {
            mSensorManager.registerListener(mProximityListener, mProximitySensor,
                    SensorManager.SENSOR_DELAY_NORMAL, mHandler);
            mSensorManager.flush(mProximityListener);
            mProximityRegistered = true;
        }
        if (mLightSensor != null && !mLightRegistered) {
            mSensorManager.registerListener(mLightListener, mLightSensor,
                    SensorManager.SENSOR_DELAY_NORMAL, mHandler);
            mSensorManager.flush(mLightListener);
            mLightRegistered = true;
        }
    }

    public void unregisterAll() {
        logD("unregisterAll");
        if (mHandler.hasCallbacks(mScreenOffRunnable)) {
            mHandler.removeCallbacks(mScreenOffRunnable);
        }
        if (mProximityRegistered) {
            mSensorManager.unregisterListener(mProximityListener);
            mProximityRegistered = false;
            mProximityInPocket = false;
        }
        if (mVendorRegistered) {
            mSensorManager.unregisterListener(mVendorListener);
            mVendorRegistered = false;
            mVendorInPocket = false;
        }
        if (mLightRegistered) {
            mSensorManager.unregisterListener(mLightListener);
            mLightRegistered = false;
            mLightPoor = false;
        }
        if (mShowing) {
            setPocketViewShow(false);
        }
        if (mCallCallbackRegistered) {
            mTelephonyManager.unregisterTelephonyCallback(mCallStateCallback);
            mCallCallbackRegistered = false;
        }
    }

    private void setPocketViewShow(boolean show) {
        if (show) {
            mPocketLockView.show();
            mHandler.postDelayed(mScreenOffRunnable, SCREEN_OFF_TIMEOUT);
            mShowing = true;
        } else {
            if (mHandler.hasCallbacks(mScreenOffRunnable)) {
                mHandler.removeCallbacks(mScreenOffRunnable);
            }
            mPocketLockView.hide();
            mShowing = false;
        }
    }

    private void updatePocketState() {
        final boolean inPocket = isDeviceInPocket();
        if (mShowing == inPocket) {
            return;
        }
        logD("updatePocketState, inPocket=" + inPocket);
        setPocketViewShow(inPocket);
    }

    private void updateSettings(int userId) {
        mEnabled = Settings.System.getIntForUser(mSystemExService.getContentResolver(),
                POCKET_JUDGE, 1, userId) != 0;
        logD("updateSettings, mEnabled=" + mEnabled);
        if (!mEnabled) {
            unregisterAll();
        }
    }

    private static void logD(String msg) {
        if (DEBUG_POCKET) {
            Slog.d(TAG, msg);
        }
    }

    private static void logE(String msg) {
        Slog.e(TAG, msg);
    }
}
