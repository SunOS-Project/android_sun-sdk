/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.os;

import static vendor.nameless.hardware.battery.V1_0.ChargingStatus.UNKNOWN;

import android.os.RemoteException;
import android.util.Log;

import java.util.NoSuchElementException;

import vendor.nameless.hardware.battery.V1_0.IBattery;

/** @hide */
public class BatteryFeatureManager {

    private static final String TAG = "BatteryFeatureManager";

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
            service = IBattery.getService();
        } catch (NoSuchElementException | RemoteException e) {
            Log.w(TAG, "Battery HAL is not found");
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
            Log.e(TAG, "Failed to check feature list", e);
        }
        return false;
    }

    public int readChargingStatus() {
        if (mService == null) {
            return UNKNOWN;
        }
        try {
            return mService.readChargingStatus();
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to read charging status", e);
        }
        return UNKNOWN;
    }

    public boolean isChargingSuspended() {
        if (mService == null) {
            return false;
        }
        try {
            return mService.isChargingSuspended();
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to get charging suspended status", e);
        }
        return false;
    }

    public void setChargingSuspended(boolean suspended) {
        if (mService == null) {
            return;
        }
        try {
            mService.setChargingSuspended(suspended);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to set charging suspended", e);
        }
    }

    public boolean isWirelessQuietModeEnabled() {
        if (mService == null) {
            return false;
        }
        try {
            return mService.isWirelessQuietModeEnabled();
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to get wireless charging quiet mode status", e);
        }
        return false;
    }

    public void setWirelessQuietModeEnabled(boolean enabled) {
        if (mService == null) {
            return;
        }
        try {
            mService.setWirelessQuietModeEnabled(enabled);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to set wireless charging quiet mode", e);
        }
    }

    public boolean isWirelessRXEnabled() {
        if (mService == null) {
            return false;
        }
        try {
            return mService.isWirelessRXEnabled();
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to get wireless charging RX status", e);
        }
        return false;
    }

    public void setWirelessRXEnabled(boolean enabled) {
        if (mService == null) {
            return;
        }
        try {
            mService.setWirelessRXEnabled(enabled);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to set wireless charging RX", e);
        }
    }

    public boolean isWirelessTXEnabled() {
        if (mService == null) {
            return false;
        }
        try {
            return mService.isWirelessTXEnabled();
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to get wireless charging TX status", e);
        }
        return false;
    }

    public void setWirelessTXEnabled(boolean enabled) {
        if (mService == null) {
            return;
        }
        try {
            mService.setWirelessTXEnabled(enabled);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to set wireless charging TX", e);
        }
    }
}
