/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.hardware;

import android.annotation.SystemService;
import android.content.Context;
import android.hardware.Sensor;
import android.os.RemoteException;
import android.util.Slog;

import java.util.Set;

import org.nameless.content.ContextExt;

/** @hide */
@SystemService(ContextExt.SENSOR_BLOCK_MANAGER_SERVICE)
public class SensorBlockManager {

    private static final String TAG = "SensorBlockManager";

    public static final int SHAKE_SENSORS_ALLOW = 0;
    public static final int SHAKE_SENSORS_BLOCK_FIRST_SCREEN = 1;
    public static final int SHAKE_SENSORS_BLOCK_ALWAYS = 2;

    public static final long APP_FIRST_SCREEN_MS = 7000L;  // Most apps hold 5s Ad. Give another 2s to avoid some edge case.
    public static final long SHAKE_SENSORS_CHECK_INTERVAL = 3000L;

    private static final Set<Integer> SHAKE_SENSORS_SET = Set.of(
        Sensor.TYPE_ACCELEROMETER,
        Sensor.TYPE_GYROSCOPE,
        Sensor.TYPE_GYROSCOPE_UNCALIBRATED
    );

    private final Context mContext;
    private final ISensorBlockService mService;

    public SensorBlockManager(Context context, ISensorBlockService service) {
        mContext = context;
        mService = service;
    }

    public int getShakeSensorsConfigForPackage(String packageName) {
        if (mService == null) {
            Slog.e(TAG, "Failed to get shake sensors config. Service is null");
            return SHAKE_SENSORS_ALLOW;
        }
        try {
            return mService.getShakeSensorsConfigForPackage(packageName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setShakeSensorsConfigForPackage(String packageName, int config) {
        if (mService == null) {
            Slog.e(TAG, "Failed to set shake sensors config. Service is null");
            return;
        }
        if (!isShakeSensorsConfigValid(config)) {
            Slog.e(TAG, "Failed to set shake sensors config. Invalid config");
        }
        try {
            mService.setShakeSensorsConfigForPackage(packageName, config);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean shouldBlockShakeSensorsNow(String packageName) {
        if (mService == null) {
            Slog.e(TAG, "Failed to get shake sensors block state. Service is null");
            return false;
        }
        try {
            return mService.shouldBlockShakeSensorsNow(packageName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static boolean isShakeSensor(int sensor) {
        return SHAKE_SENSORS_SET.contains(sensor);
    }

    public static boolean isShakeSensorsConfigValid(int config) {
        return config == SHAKE_SENSORS_ALLOW ||
                config == SHAKE_SENSORS_BLOCK_FIRST_SCREEN ||
                config == SHAKE_SENSORS_BLOCK_ALWAYS;
    }

    public static String shakeSensorsConfigToString(int config) {
        switch (config) {
            case SHAKE_SENSORS_ALLOW:
                return "shake_sensors_allow";
            case SHAKE_SENSORS_BLOCK_FIRST_SCREEN:
                return "shake_sensors_block_first_screen";
            case SHAKE_SENSORS_BLOCK_ALWAYS:
                return "shake_sensors_block_always";
            default:
                return "unknown";
        }
    }
}
