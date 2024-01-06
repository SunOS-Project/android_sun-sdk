/*
 * Copyright (C) 2022-2023 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.display;

import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import java.util.NoSuchElementException;

import vendor.nameless.hardware.displayfeature.V1_0.IDisplayFeature;

/** @hide */
public class DisplayFeatureManager {

    private static final String TAG = "DisplayFeatureManager";

    public static final int CUSTOM_DISPLAY_COLOR_MODE_START = 10001;

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
            service = IDisplayFeature.getService();
        } catch (NoSuchElementException | RemoteException e) {
            Log.w(TAG, "DisplayFeature HAL is not found");
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

    public boolean getFeatureEnabled(int feature) {
        if (mService == null) {
            return false;
        }
        try {
            return mService.getFeatureEnabled(feature);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to get feature enabled status", e);
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
            Log.e(TAG, "Failed to set feature enabled", e);
        }
    }

    public void setColorMode(int mode) {
        if (mService == null) {
            return;
        }
        try {
            mService.setColorMode(mode);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to set color mode", e);
        }
    }

    public void setDisplayOrientation(int orientation) {
        if (mService == null) {
            return;
        }
        try {
            mService.setDisplayOrientation(orientation);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to set display orientation", e);
        }
    }
}
