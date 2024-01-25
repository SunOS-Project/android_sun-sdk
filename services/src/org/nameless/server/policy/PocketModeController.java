/**
 * Copyright (C) 2016 The ParanoidAndroid Project
 * Copyright (C) 2024 The Nameless-AOSP Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.nameless.server.policy;

import static org.nameless.content.ContextExt.POCKET_SERVICE;
import static org.nameless.os.DebugConstants.DEBUG_POCKET;
import static org.nameless.provider.SettingsExt.System.POCKET_JUDGE;

import android.content.Context;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings.System;
import android.text.TextUtils;
import android.util.Slog;

import com.android.internal.R;

import java.util.ArrayList;

import org.nameless.os.IPocketCallback;
import org.nameless.os.IPocketService;
import org.nameless.os.PocketManager;
import org.nameless.server.NamelessSystemExService;

public class PocketModeController implements IBinder.DeathRecipient {

    private static final String TAG = "PocketModeController";

    /**
     * Wheater we don't have yet a valid vendor sensor event or pocket service not running.
     */
    private static final int VENDOR_SENSOR_UNKNOWN = 0;

    /**
     * Vendor sensor has been registered, onSensorChanged() has been called and we have a
     * valid event value from Vendor pocket sensor.
     */
    private static final int VENDOR_SENSOR_IN_POCKET = 1;

    /**
     * The rate proximity sensor events are delivered at.
     */
    private static final int PROXIMITY_SENSOR_DELAY = 400000;

    /**
     * Wheater we don't have yet a valid proximity sensor event or pocket service not running.
     */
    private static final int PROXIMITY_UNKNOWN = 0;

    /**
     * Proximity sensor has been registered, onSensorChanged() has been called and we have a
     * valid event value which determined proximity sensor is covered.
     */
    private static final int PROXIMITY_POSITIVE = 1;

    /**
     * Proximity sensor has been registered, onSensorChanged() has been called and we have a
     * valid event value which determined proximity sensor is not covered.
     */
    private static final int PROXIMITY_NEGATIVE = 2;

    /**
     * The rate light sensor events are delivered at.
     */
    private static final int LIGHT_SENSOR_DELAY = 400000;

    /**
     * Wheater we don't have yet a valid light sensor event or pocket service not running.
     */
    private static final int LIGHT_UNKNOWN = 0;

    /**
     * Light sensor has been registered, onSensorChanged() has been called and we have a
     * valid event value which determined available light is in pocket range.
     */
    private static final int LIGHT_POCKET = 1;

    /**
     * Light sensor has been registered, onSensorChanged() has been called and we have a
     * valid event value which determined available light is outside pocket range.
     */
    private static final int LIGHT_AMBIENT = 2;

    /**
     * Light sensor maximum value registered in pocket with up to semi-transparent fabric.
     */
    private static final float POCKET_LIGHT_MAX_THRESHOLD = 3.0f;

    private static class InstanceHolder {
        private static PocketModeController INSTANCE = new PocketModeController();
    }

    public static PocketModeController getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private final ArrayList<IPocketCallback> mCallbacks = new ArrayList<>();

    private NamelessSystemExService mSystemExService;
    private PocketHandler mHandler;
    private PocketObserver mObserver;
    private SensorManager mSensorManager;

    private boolean mSystemReady = false;
    private boolean mSystemBooted = false;

    private boolean mEnabled;
    private boolean mInteractive;
    private boolean mPending;

    // proximity
    private int mProximityState = PROXIMITY_UNKNOWN;
    private int mLastProximityState = PROXIMITY_UNKNOWN;
    private float mProximityMaxRange;
    private boolean mProximityRegistered;
    private Sensor mProximitySensor;

    // light
    private int mLightState = LIGHT_UNKNOWN;
    private int mLastLightState = LIGHT_UNKNOWN;
    private float mLightMaxRange;
    private boolean mLightRegistered;
    private boolean mUseLightSensor;
    private Sensor mLightSensor;

    // vendor sensor
    private int mVendorSensorState = VENDOR_SENSOR_UNKNOWN;
    private int mLastVendorSensorState = VENDOR_SENSOR_UNKNOWN;
    private String mVendorPocketSensor;
    private boolean mVendorSensorRegistered;
    private Sensor mVendorSensor;

    // Custom methods
    private boolean mPocketLockVisible;

    private class PocketObserver extends ContentObserver {

        private boolean mRegistered;

        public PocketObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            final boolean enabled = System.getIntForUser(mSystemExService.getContentResolver(),
                    POCKET_JUDGE, 1 /* default */, UserHandle.USER_CURRENT) != 0;
            setEnabled(enabled);
        }

        public void register() {
            if (!mRegistered) {
                mSystemExService.getContentResolver().registerContentObserver(
                        System.getUriFor(POCKET_JUDGE), true, this);
                mRegistered = true;
            }
        }

        public void unregister() {
            if (mRegistered) {
                mSystemExService.getContentResolver().unregisterContentObserver(this);
                mRegistered = false;
            }
        }
    }

    private class PocketHandler extends Handler {

        public static final int MSG_DISPATCH_CALLBACKS = 0;
        public static final int MSG_ADD_CALLBACK = 1;
        public static final int MSG_REMOVE_CALLBACK = 2;
        public static final int MSG_INTERACTIVE_CHANGED = 3;
        public static final int MSG_SENSOR_EVENT_PROXIMITY = 4;
        public static final int MSG_SENSOR_EVENT_LIGHT = 5;
        public static final int MSG_UNREGISTER_TIMEOUT = 6;
        public static final int MSG_SET_LISTEN_EXTERNAL = 7;
        public static final int MSG_SET_POCKET_LOCK_VISIBLE = 8;
        public static final int MSG_SENSOR_EVENT_VENDOR = 9;

        public PocketHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case MSG_DISPATCH_CALLBACKS:
                    handleDispatchCallbacks();
                    break;
                case MSG_ADD_CALLBACK:
                    handleAddCallback((IPocketCallback) msg.obj);
                    break;
                case MSG_REMOVE_CALLBACK:
                    handleRemoveCallback((IPocketCallback) msg.obj);
                    break;
                case MSG_INTERACTIVE_CHANGED:
                    handleInteractiveChanged(msg.arg1 != 0);
                    break;
                case MSG_SENSOR_EVENT_PROXIMITY:
                    handleProximitySensorEvent((SensorEvent) msg.obj);
                    break;
                case MSG_SENSOR_EVENT_LIGHT:
                    handleLightSensorEvent((SensorEvent) msg.obj);
                    break;
                case MSG_SENSOR_EVENT_VENDOR:
                    handleVendorSensorEvent((SensorEvent) msg.obj);
                    break;
                case MSG_UNREGISTER_TIMEOUT:
                    handleUnregisterTimeout();
                    break;
                case MSG_SET_LISTEN_EXTERNAL:
                    handleSetListeningExternal(msg.arg1 != 0);
                    break;
                case MSG_SET_POCKET_LOCK_VISIBLE:
                    handleSetPocketLockVisible(msg.arg1 != 0);
                    break;
                default:
                    logE("Unknown message: " + msg.what);
                    break;
            }
        }
    }

    public void initSystemExService(NamelessSystemExService service) {
        mSystemExService = service;

        final HandlerThread handlerThread = new HandlerThread(TAG, Process.THREAD_PRIORITY_BACKGROUND);
        handlerThread.start();
        mHandler = new PocketHandler(handlerThread.getLooper());

        mSensorManager = service.getContext().getSystemService(SensorManager.class);
        mVendorPocketSensor = service.getContext().getResources().getString(
                R.string.config_pocketJudgeVendorSensorName);
        final String vendorProximitySensor = service.getContext().getResources().getString(
                R.string.config_pocketJudgeVendorProximitySensorName);
        if (!TextUtils.isEmpty(vendorProximitySensor)) {
            mProximitySensor = getSensor(mSensorManager, vendorProximitySensor);
        } else {
            mProximitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        }
        if (mProximitySensor != null) {
            mProximityMaxRange = mProximitySensor.getMaximumRange();
        }
        mUseLightSensor = service.getContext().getResources().getBoolean(
                R.bool.config_pocketUseLightSensor);
        mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        if (mLightSensor != null) {
            mLightMaxRange = mLightSensor.getMaximumRange();
        }
        mVendorSensor = getSensor(mSensorManager, mVendorPocketSensor);

        mObserver = new PocketObserver(mHandler);
        mObserver.onChange(true);
        mObserver.register();

        mSystemExService.publishBinderService(POCKET_SERVICE, new PocketServiceWrapper());
    }

    public void onSystemServicesReady() {
        logD("onSystemServicesReady, mVendorPocketSensor=" +  mVendorPocketSensor);
        mSystemReady = true;
        if (mPending) {
            final Message msg = new Message();
            msg.what = PocketHandler.MSG_INTERACTIVE_CHANGED;
            msg.arg1 = mInteractive ? 1 : 0;
            mHandler.sendMessage(msg);
            mPending = false;
        }
    }

    public void onBootCompleted() {
        logD("onBootCompleted");
        mSystemBooted = true;
        if (mPending) {
            final Message msg = new Message();
            msg.what = PocketHandler.MSG_INTERACTIVE_CHANGED;
            msg.arg1 = mInteractive ? 1 : 0;
            mHandler.sendMessage(msg);
            mPending = false;
        }
    }

    public void onUserSwitching(int newUserId) {
        logD("onUserSwitching");
        final boolean enabled = System.getIntForUser(mSystemExService.getContentResolver(),
                POCKET_JUDGE, 1 /* default */, newUserId) != 0;
        setEnabled(enabled);
    }

    @Override
    public void binderDied() {
        logD("binderDied");
        synchronized (mCallbacks) {
            mProximityState = PROXIMITY_UNKNOWN;
            final int callbacksSize = mCallbacks.size();
            for (int i = callbacksSize - 1; i >= 0; i--) {
                if (mCallbacks.get(i) == null) {
                    continue;
                }
                try {
                    mCallbacks.get(i).onStateChanged(false, PocketManager.REASON_RESET);
                } catch (DeadObjectException e) {
                    logW("Death object while invoking sendPocketState: " + e);
                } catch (RemoteException e) {
                    logW("Failed to invoke sendPocketState: " + e);
                }
            }
            mCallbacks.clear();
        }
        unregisterSensorListeners();
        mObserver.unregister();
    }

    private final class PocketServiceWrapper extends IPocketService.Stub {

        @Override // Binder call
        public void addCallback(final IPocketCallback callback) {
            final Message msg = new Message();
            msg.what = PocketHandler.MSG_ADD_CALLBACK;
            msg.obj = callback;
            mHandler.sendMessage(msg);
        }

        @Override // Binder call
        public void removeCallback(final IPocketCallback callback) {
            final Message msg = new Message();
            msg.what = PocketHandler.MSG_REMOVE_CALLBACK;
            msg.obj = callback;
            mHandler.sendMessage(msg);
        }

        @Override // Binder call
        public void onInteractiveChanged(final boolean interactive) {
            final Message msg = new Message();
            msg.what = PocketHandler.MSG_INTERACTIVE_CHANGED;
            msg.arg1 = interactive ? 1 : 0;
            mHandler.sendMessage(msg);
        }

        @Override // Binder call
        public void setListeningExternal(final boolean listen) {
            final Message msg = new Message();
            msg.what = PocketHandler.MSG_SET_LISTEN_EXTERNAL;
            msg.arg1 = listen ? 1 : 0;
            mHandler.sendMessage(msg);
        }

        @Override // Binder call
        public boolean isDeviceInPocket() {
            final long ident = Binder.clearCallingIdentity();
            try {
                if (!mSystemReady || !mSystemBooted) {
                    return false;
                }
                return PocketModeController.this.isDeviceInPocket();
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        @Override // Binder call
        public void setPocketLockVisible(final boolean visible) {
            final Message msg = new Message();
            msg.what = PocketHandler.MSG_SET_POCKET_LOCK_VISIBLE;
            msg.arg1 = visible ? 1 : 0;
            mHandler.sendMessage(msg);
        }

        @Override // Binder call
        public boolean isPocketLockVisible() {
            final long ident = Binder.clearCallingIdentity();
            try {
                if (!mSystemReady || !mSystemBooted) {
                    return false;
                }
                return PocketModeController.this.isPocketLockVisible();
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    private final SensorEventListener mProximityListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            final Message msg = new Message();
            msg.what = PocketHandler.MSG_SENSOR_EVENT_PROXIMITY;
            msg.obj = sensorEvent;
            mHandler.sendMessage(msg);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) { }
    };

    private final SensorEventListener mLightListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            final Message msg = new Message();
            msg.what = PocketHandler.MSG_SENSOR_EVENT_LIGHT;
            msg.obj = sensorEvent;
            mHandler.sendMessage(msg);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) { }
    };

    private final SensorEventListener mVendorSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            final Message msg = new Message();
            msg.what = PocketHandler.MSG_SENSOR_EVENT_VENDOR;
            msg.obj = sensorEvent;
            mHandler.sendMessage(msg);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) { }
    };

    private boolean isDeviceInPocket() {
        if (mVendorSensorState != VENDOR_SENSOR_UNKNOWN) {
            return mVendorSensorState == VENDOR_SENSOR_IN_POCKET;
        }
        if (mLightState != LIGHT_UNKNOWN) {
            return mProximityState == PROXIMITY_POSITIVE
                    && mLightState == LIGHT_POCKET;
        }
        return mProximityState == PROXIMITY_POSITIVE;
    }

    private void setEnabled(boolean enabled) {
        logD("setEnabled, enabled=" + enabled);
        if (enabled != mEnabled) {
            mEnabled = enabled;
            mHandler.removeCallbacksAndMessages(null);
            update();
        }
    }

    private void update() {
        if (!mEnabled || mInteractive) {
            if (mEnabled && isDeviceInPocket()) {
                // if device is judged to be in pocket while switching
                // to interactive state, we need to keep monitoring.
                return;
            }
            unregisterSensorListeners();
        } else {
            mHandler.removeMessages(PocketHandler.MSG_UNREGISTER_TIMEOUT);
            registerSensorListeners();
        }
    }

    private void registerSensorListeners() {
        startListeningForVendorSensor();
        startListeningForProximity();
        startListeningForLight();
    }

    private void unregisterSensorListeners() {
        stopListeningForVendorSensor();
        stopListeningForProximity();
        stopListeningForLight();
    }

    private void startListeningForVendorSensor() {
        logD("startListeningForVendorSensor");
        if (mVendorSensor == null) {
            logE("Cannot detect Vendor pocket sensor, sensor is NULL");
            return;
        }
        if (!mVendorSensorRegistered) {
            mSensorManager.registerListener(mVendorSensorListener, mVendorSensor,
                    SensorManager.SENSOR_DELAY_NORMAL, mHandler);
            mVendorSensorRegistered = true;
        }
    }

    private void stopListeningForVendorSensor() {
        logD("stopListeningForVendorSensor");
        if (mVendorSensorRegistered) {
            mVendorSensorState = mLastVendorSensorState = VENDOR_SENSOR_UNKNOWN;
            mSensorManager.unregisterListener(mVendorSensorListener);
            mVendorSensorRegistered = false;
        }
    }

    private void startListeningForProximity() {
        if (mVendorSensor != null) {
            return;
        }
        logD("startListeningForProximity");
        if (mProximitySensor == null) {
            logE("Cannot detect proximity sensor, sensor is NULL");
            return;
        }
        if (!mProximityRegistered) {
            mSensorManager.registerListener(mProximityListener, mProximitySensor,
                    PROXIMITY_SENSOR_DELAY, mHandler);
            mProximityRegistered = true;
        }
    }

    private void stopListeningForProximity() {
        logD("startListeningForProximity");
        if (mProximityRegistered) {
            mLastProximityState = mProximityState = PROXIMITY_UNKNOWN;
            mSensorManager.unregisterListener(mProximityListener);
            mProximityRegistered = false;
        }
    }

    private void startListeningForLight() {
        if (mVendorSensor != null) {
            return;
        }
        logD("startListeningForLight");
        if (!mUseLightSensor) {
            return;
        }
        if (mLightSensor == null) {
            logE("Cannot detect light sensor, sensor is NULL");
            return;
        }
        if (!mLightRegistered) {
            mSensorManager.registerListener(mLightListener, mLightSensor,
                    LIGHT_SENSOR_DELAY, mHandler);
            mLightRegistered = true;
        }
    }

    private void stopListeningForLight() {
        logD("stopListeningForLight");
        if (mLightRegistered) {
            mLightState = mLastLightState = LIGHT_UNKNOWN;
            mSensorManager.unregisterListener(mLightListener);
            mLightRegistered = false;
        }
    }

    private void handleDispatchCallbacks() {
        synchronized (mCallbacks) {
            final int N = mCallbacks.size();
            boolean cleanup = false;
            for (int i = 0; i < N; i++) {
                final IPocketCallback callback = mCallbacks.get(i);
                try {
                    if (callback != null) {
                        callback.onStateChanged(isDeviceInPocket(), PocketManager.REASON_SENSOR);
                    } else {
                        cleanup = true;
                    }
                } catch (RemoteException e) {
                    cleanup = true;
                }
            }
            if (cleanup) {
                cleanUpCallbacksLocked(null);
            }
        }
    }

    private void cleanUpCallbacksLocked(IPocketCallback callback) {
        synchronized (mCallbacks) {
            for (int i = mCallbacks.size() - 1; i >= 0; i--) {
                final IPocketCallback found = mCallbacks.get(i);
                if (found == null || found == callback) {
                    mCallbacks.remove(i);
                }
            }
        }
    }

    private void handleSetPocketLockVisible(boolean visible) {
        mPocketLockVisible = visible;
    }

    private boolean isPocketLockVisible() {
        return mPocketLockVisible;
    }

    private void handleSetListeningExternal(boolean listen) {
        if (listen) {
            // should prevent external processes to register while interactive,
            // while they are allowed to stop listening in any case as for example
            // coming pocket lock will need to.
            if (!mInteractive) {
                registerSensorListeners();
            }
        } else {
            mHandler.removeCallbacksAndMessages(null);
            unregisterSensorListeners();
        }
        dispatchCallbacks();
    }

    private void handleAddCallback(IPocketCallback callback) {
        synchronized (mCallbacks) {
            if (!mCallbacks.contains(callback)) {
                mCallbacks.add(callback);
            }
        }
    }

    private void handleRemoveCallback(IPocketCallback callback) {
        synchronized (mCallbacks) {
            if (mCallbacks.contains(callback)) {
                mCallbacks.remove(callback);
            }
        }
    }

    private void handleInteractiveChanged(boolean interactive) {
        // always update interactive state.
        mInteractive = interactive;

        if (mPending) {
            // working on it, waiting for proper system conditions.
            return;
        }
        if (!mPending && (!mSystemBooted || !mSystemReady)) {
            // we ain't ready, postpone till system is both booted AND ready.
            mPending = true;
            return;
        }

        update();
    }

    private void handleVendorSensorEvent(SensorEvent sensorEvent) {
        final boolean isDeviceInPocket = isDeviceInPocket();

        mLastVendorSensorState = mVendorSensorState;

        try {
            if (sensorEvent == null) {
                logW("VENDOR_SENSOR: Event is null!");
                mVendorSensorState = VENDOR_SENSOR_UNKNOWN;
            } else if (sensorEvent.values == null || sensorEvent.values.length == 0) {
                logW("VENDOR_SENSOR: Event has no values!");
                mVendorSensorState = VENDOR_SENSOR_UNKNOWN;
            } else {
                final boolean isVendorPocket = sensorEvent.values[0] == 1.0;
                logD("VENDOR_SENSOR: value=" + sensorEvent.values[0] + ", isInPocket=" + isVendorPocket);
                mVendorSensorState = isVendorPocket ? VENDOR_SENSOR_IN_POCKET : VENDOR_SENSOR_UNKNOWN;
            }
        } catch (Exception e) {
            logE("VENDOR_SENSOR: Something went wrong, exception caught, e: " + e);
            mVendorSensorState = VENDOR_SENSOR_UNKNOWN;
        } finally {
            if (isDeviceInPocket != isDeviceInPocket()) {
                dispatchCallbacks();
            }
        }
   }

    private void handleLightSensorEvent(SensorEvent sensorEvent) {
        final boolean isDeviceInPocket = isDeviceInPocket();

        mLastLightState = mLightState;

        try {
            if (sensorEvent == null) {
                logW("LIGHT_SENSOR: Event is null!");
                mLightState = LIGHT_UNKNOWN;
            } else if (sensorEvent.values == null || sensorEvent.values.length == 0) {
                logW("LIGHT_SENSOR: Event has no values!");
                mLightState = LIGHT_UNKNOWN;
            } else {
                final float value = sensorEvent.values[0];
                final boolean isPoor = value >= 0
                        && value <= POCKET_LIGHT_MAX_THRESHOLD;
                logD("LIGHT_SENSOR: value=" + value + ", maxRange=" + mLightMaxRange + ", isPoor=" + isPoor);
                mLightState = isPoor ? LIGHT_POCKET : LIGHT_AMBIENT;
            }
        } catch (Exception e) {
            logE("LIGHT_SENSOR: Something went wrong, exception caught, e: " + e);
            mLightState = LIGHT_UNKNOWN;
        } finally {
            if (isDeviceInPocket != isDeviceInPocket()) {
                dispatchCallbacks();
            }
        }
    }

    private void handleProximitySensorEvent(SensorEvent sensorEvent) {
        final boolean isDeviceInPocket = isDeviceInPocket();

        mLastProximityState = mProximityState;

        try {
            if (sensorEvent == null) {
                logW("PROXIMITY_SENSOR: Event is null!");
                mProximityState = PROXIMITY_UNKNOWN;
            } else if (sensorEvent.values == null || sensorEvent.values.length == 0) {
                logW("PROXIMITY_SENSOR: Event has no values!");
                mProximityState = PROXIMITY_UNKNOWN;
            } else {
                final float value = sensorEvent.values[0];
                final boolean isPositive = sensorEvent.values[0] < mProximityMaxRange;
                logD("PROXIMITY_SENSOR: value=" + value + ", maxRange=" + mProximityMaxRange + ", isPositive=" + isPositive);
                mProximityState = isPositive ? PROXIMITY_POSITIVE : PROXIMITY_NEGATIVE;
            }
        } catch (Exception e) {
            logE("PROXIMITY_SENSOR: Something went wrong, exception caught, e: " + e);
            mProximityState = PROXIMITY_UNKNOWN;
        } finally {
            if (isDeviceInPocket != isDeviceInPocket()) {
                dispatchCallbacks();
            }
        }
    }

    private void handleUnregisterTimeout() {
        mHandler.removeCallbacksAndMessages(null);
        unregisterSensorListeners();
    }

    private static Sensor getSensor(SensorManager sm, String type) {
        for (Sensor sensor : sm.getSensorList(Sensor.TYPE_ALL)) {
            if (type.equals(sensor.getStringType())) {
                return sensor;
            }
        }
        return null;
    }

    private void dispatchCallbacks() {
        final boolean isDeviceInPocket = isDeviceInPocket();
        logD("dispatchCallbacks, isDeviceInPocket=" + isDeviceInPocket + ", mInteractive=" + mInteractive);
        if (mInteractive) {
            if (!isDeviceInPocket) {
                mHandler.sendEmptyMessageDelayed(PocketHandler.MSG_UNREGISTER_TIMEOUT, 5000L /* ms */);
            } else {
                mHandler.removeMessages(PocketHandler.MSG_UNREGISTER_TIMEOUT);
            }
        }
        mHandler.removeMessages(PocketHandler.MSG_DISPATCH_CALLBACKS);
        mHandler.sendEmptyMessage(PocketHandler.MSG_DISPATCH_CALLBACKS);
    }

    private static void logD(String msg) {
        if (DEBUG_POCKET) {
            Slog.d(TAG, msg);
        }
    }

    private static void logW(String msg) {
        Slog.w(TAG, msg);
    }

    private static void logE(String msg) {
        Slog.e(TAG, msg);
    }
}
