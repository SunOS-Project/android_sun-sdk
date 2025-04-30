/*
 * Copyright (C) 2022-2023 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.display;

import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Slog;

import java.util.NoSuchElementException;

import vendor.sun.hardware.displayfeature.IDisplayFeature;

/** @hide */
public class DisplayFeatureManager {

    private static final String TAG = "DisplayFeatureManager";

    private static final String SERVICE_NAME = "vendor.sun.hardware.displayfeature.IDisplayFeature/default";

    public static final int CUSTOM_DISPLAY_COLOR_MODE_START = 10001;

    // Show "One-Pulse EM mode" instead of "DC dimming"
    public static final boolean DC_ALIAS_ONE_PULSE =
            SystemProperties.getBoolean("sys.sun.feature.display.dc_alias_one_pulse", false);

    private final IDisplayFeature mService;

    private static class InstanceHolder {
        private static final DisplayFeatureManager INSTANCE = new DisplayFeatureManager();
    }

    public static DisplayFeatureManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private DisplayFeatureManager() {
        IDisplayFeature service;
        try {
            service = IDisplayFeature.Stub.asInterface(ServiceManager.getService(SERVICE_NAME));
        } catch (NoSuchElementException e) {
            Slog.w(TAG, "DisplayFeature HAL is not found");
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

    public void setColorMode(int mode) {
        if (mService == null) {
            return;
        }
        try {
            mService.setColorMode(mode);
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to set color mode", e);
        }
    }

    public void setDisplayOrientation(int orientation) {
        if (mService == null) {
            return;
        }
        try {
            mService.setDisplayOrientation(orientation);
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to set display orientation", e);
        }
    }

    public int sendCommand(int command) {
        if (mService == null) {
            return -1;
        }
        try {
            return mService.sendCommand(command);
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to send command", e);
        }
        return -1;
    }
}
