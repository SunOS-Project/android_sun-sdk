/*
 * Copyright (C) 2021-2022 The LineageOS Project
 * Copyright (C) 2024 The LineageOS Project
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
import android.os.SystemClock;

import com.android.internal.util.nameless.DozeHelper;

import org.nameless.server.policy.DozeController;

public class PickUpSensor implements SensorEventListener {

    private static final String TAG = "PickUpSensor";

    private static final long MIN_EVENT_INTERVAL_MS = 2500L;

    private final Context mContext;
    private final String mSensorType;
    private final float mSensorValue;
    private final SensorManager mSensorManager;

    private Sensor mSensor = null;

    private boolean mListening = false;
    private int mAction = DOZE_ACTION_NONE;
    private long mEntryTimestamp = 0L;

    public PickUpSensor(Context context, String sensorType, float sensorValue) {
        mContext = context;
        mSensorType = sensorType;
        mSensorValue = sensorValue;

        mSensorManager = context.getSystemService(SensorManager.class);
        for (Sensor s : mSensorManager.getSensorList(Sensor.TYPE_ALL)) {
            if (s.getStringType().equals(mSensorType)) {
                mSensor = s;
                break;
            }
        }

        logD(TAG, "init, mSensorType=" + mSensorType + ", mSensorValue=" + mSensorValue
                + ", foundSensor=" + (mSensor != null));

        updateSettings();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        logD(TAG, "Got sensor event: " + event.values[0]);
        final long delta = SystemClock.elapsedRealtime() - mEntryTimestamp;
        if (delta < MIN_EVENT_INTERVAL_MS) {
            return;
        }
        mEntryTimestamp = SystemClock.elapsedRealtime();
        if (event.values[0] == mSensorValue) {
            switch (mAction) {
                case DOZE_ACTION_SHOW_AMBINET:
                    DozeController.getInstance().launchDozePulse();
                    break;
                case DOZE_ACTION_SHOW_LOCKSCREEN:
                    DozeController.getInstance().wakeUpScreen();
                    break;
            }
        }
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
            mEntryTimestamp = SystemClock.elapsedRealtime();
            mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
            mListening = true;
        }
    }

    public void onScreenOn() {
        if (mListening) {
            logD(TAG, "onScreenOn, unregister sensor");
            mSensorManager.unregisterListener(this, mSensor);
            mListening = false;
        }
    }
}
