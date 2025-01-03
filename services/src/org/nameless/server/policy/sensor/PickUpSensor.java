/*
 * Copyright (C) 2021-2022 The LineageOS Project
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.server.policy.sensor;

import static com.android.internal.util.nameless.DozeHelper.DOZE_ACTION_SHOW_AMBINET;
import static com.android.internal.util.nameless.DozeHelper.DOZE_ACTION_SHOW_LOCKSCREEN;
import static com.android.internal.util.nameless.DozeHelper.DOZE_ACTION_NONE;
import static com.android.internal.util.nameless.DozeHelper.dozeActionToString;

import static org.nameless.server.policy.DozeController.logD;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.os.SystemClock;

import com.android.internal.util.nameless.DozeHelper;

import org.nameless.server.policy.DozeController;

public class PickUpSensor extends TriggerEventListener implements SensorEventListener {

    private static final String TAG = "PickUpSensor";

    private static final long MIN_EVENT_INTERVAL_MS = 2500L;

    private final Context mContext;
    private final String mSensorType;
    private final float mSensorValue;
    private final boolean mUseNativePickUpSensor;
    private final SensorManager mSensorManager;

    private Sensor mSensor = null;

    private boolean mListening = false;
    private int mAction = DOZE_ACTION_NONE;
    private long mEntryTimestamp = 0L;

    public PickUpSensor(Context context, String sensorType, float sensorValue, boolean useNativePickUpSensor) {
        super();
        mContext = context;
        mSensorType = sensorType;
        mSensorValue = sensorValue;
        mUseNativePickUpSensor = useNativePickUpSensor;

        mSensorManager = context.getSystemService(SensorManager.class);

        if (mUseNativePickUpSensor) {
            mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PICK_UP_GESTURE);
            logD(TAG, "init, mUseNativePickUpSensor=true, mSensorValue=" + mSensorValue
                    + ", foundSensor=" + (mSensor != null));
        } else {
            for (Sensor s : mSensorManager.getSensorList(Sensor.TYPE_ALL)) {
                if (s.getStringType().equals(mSensorType)) {
                    mSensor = s;
                    break;
                }
            }
            logD(TAG, "init, mUseNativePickUpSensor=false, mSensorType=" + mSensorType
                    + ", mSensorValue=" + mSensorValue
                    + ", foundSensor=" + (mSensor != null));
        }

        updateSettings();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        handleSensorEvent(event.values[0]);
    }

    @Override
    public void onTrigger(TriggerEvent event) {
        if (!handleSensorEvent(event.values[0])) {
            mSensorManager.requestTriggerSensor(this, mSensor);
        }
    }

    private boolean handleSensorEvent(float value) {
        logD(TAG, "handleSensorEvent, value=" + value);
        final long delta = SystemClock.elapsedRealtime() - mEntryTimestamp;
        if (delta < MIN_EVENT_INTERVAL_MS) {
            logD(TAG, "handleSensorEvent, ignore event in min interval");
            return false;
        }
        mEntryTimestamp = SystemClock.elapsedRealtime();
        if (value == mSensorValue) {
            logD(TAG, "handleSensorEvent, action=" + dozeActionToString(mAction));
            switch (mAction) {
                case DOZE_ACTION_SHOW_AMBINET:
                    DozeController.getInstance().launchDozePulse();
                    return false;
                case DOZE_ACTION_SHOW_LOCKSCREEN:
                    DozeController.getInstance().wakeUpScreen(true);
                    return true;
            }
        }
        return false;
    }

    public boolean isSupported() {
        return mSensor != null;
    }

    public void updateSettings() {
        mAction = DozeHelper.getPickUpAction(mContext);
        logD(TAG, "updateSettings, updated action to " + dozeActionToString(mAction));
    }

    public void onUserSwitching(int newUserId) {
        mAction = DozeHelper.getPickUpAction(mContext, newUserId);
        logD(TAG, "onUserSwitching, updated action to " + dozeActionToString(mAction));
    }

    public void onScreenOff() {
        if (!mListening) {
            logD(TAG, "onScreenOff, register sensor");
            mEntryTimestamp = 0L;
            if (mUseNativePickUpSensor) {
                mSensorManager.requestTriggerSensor(this, mSensor);
            } else {
                mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
            mListening = true;
        }
    }

    public void onScreenOn() {
        if (mListening) {
            logD(TAG, "onScreenOn, unregister sensor");
            if (mUseNativePickUpSensor) {
                mSensorManager.cancelTriggerSensor(this, mSensor);
            } else {
                mSensorManager.unregisterListener(this, mSensor);
            }
            mListening = false;
        }
    }
}
