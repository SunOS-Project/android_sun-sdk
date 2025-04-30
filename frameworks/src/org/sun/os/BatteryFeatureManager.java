/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.os;

import static vendor.sun.hardware.battery.ChargingStatus.UNKNOWN;

import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Slog;

import java.util.NoSuchElementException;

import vendor.sun.hardware.battery.IBattery;

/** @hide */
public class BatteryFeatureManager {

    private static final String TAG = "BatteryFeatureManager";

    private static final String SERVICE_NAME = "vendor.sun.hardware.battery.IBattery/default";

    public static final String EXTRA_CHARGER_STATUS_CUSTOM = "charger_status_custom";

    private final IBattery mService;

    private static class InstanceHolder {
        private static final BatteryFeatureManager INSTANCE = new BatteryFeatureManager();
    }

    public static BatteryFeatureManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private BatteryFeatureManager() {
        IBattery service;
        try {
            service = IBattery.Stub.asInterface(ServiceManager.getService(SERVICE_NAME));
        } catch (NoSuchElementException e) {
            Slog.w(TAG, "Battery HAL is not found");
            service = null;
        }
        mService = service;
    }

    public boolean isSupported() {
        return mService != null;
    }

    public boolean hasFeature(int feature) {
        if (mService == null) {
            return false;
        }
        try {
            return mService.hasFeature(feature);
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to check feature list", e);
        }
        return false;
    }

    public boolean isFeatureEnabled(int feature) {
        if (mService == null) {
            return false;
        }
        try {
            return mService.isFeatureEnabled(feature);
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to get feature enabled status", e);
        }
        return false;
    }

    public void setFeatureEnabled(int feature, boolean enabled) {
        if (mService == null) {
            return;
        }
        try {
            mService.setFeatureEnabled(feature, enabled);
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to set feature enabled", e);
        }
    }

    public int readChargingStatus() {
        if (mService == null) {
            return UNKNOWN;
        }
        try {
            return mService.readChargingStatus();
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to read charging status", e);
        }
        return UNKNOWN;
    }
}
