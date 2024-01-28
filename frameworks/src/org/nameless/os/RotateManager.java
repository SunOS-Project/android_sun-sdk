/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.os;

import android.annotation.SystemService;
import android.content.Context;
import android.os.RemoteException;
import android.util.Slog;

import org.nameless.content.ContextExt;

/** @hide */
@SystemService(ContextExt.ROTATE_MANAGER_SERVICE)
public class RotateManager {

    private static final String TAG = "RotateManager";

    public static final int ROTATE_FOLLOW_SYSTEM = 0;
    public static final int ROTATE_FORCE_OFF = 1;
    public static final int ROTATE_FORCE_ON = 2;

    private final Context mContext;
    private final IRotateManagerService mService;

    public RotateManager(Context context, IRotateManagerService service) {
        mContext = context;
        mService = service;
    }

    public int getRotateConfigForPackage(String packageName) {
        if (mService == null) {
            Slog.e(TAG, "Failed to get rotate config. Service is null");
            return ROTATE_FOLLOW_SYSTEM;
        }
        try {
            return mService.getRotateConfigForPackage(packageName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setRotateConfigForPackage(String packageName, int config) {
        if (mService == null) {
            Slog.e(TAG, "Failed to set rotate config. Service is null");
            return;
        }
        if (config != ROTATE_FOLLOW_SYSTEM &&
                config != ROTATE_FORCE_OFF &&
                config != ROTATE_FORCE_ON) {
            Slog.e(TAG, "Failed to set rotate config. Invalid config");
        }
        try {
            mService.setRotateConfigForPackage(packageName, config);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getCurrentRotateConfig() {
        if (mService == null) {
            Slog.e(TAG, "Failed to get rotate state. Service is null");
            return ROTATE_FOLLOW_SYSTEM;
        }
        try {
            return mService.getCurrentRotateConfig();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean registerRotateConfigListener(IRotateConfigListener.Stub listener) {
        if (mService == null) {
            Slog.e(TAG, "Failed to register rotate config listener. Service is null");
            return false;
        }
        try {
            return mService.registerRotateConfigListener(listener);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean unregisterRotateConfigListener(IRotateConfigListener.Stub listener) {
        if (mService == null) {
            Slog.e(TAG, "Failed to unregister rotate config listener. Service is null");
            return false;
        }
        try {
            return mService.unregisterRotateConfigListener(listener);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static boolean isConfigValid(int config) {
        return config == ROTATE_FOLLOW_SYSTEM ||
                config == ROTATE_FORCE_OFF ||
                config == ROTATE_FORCE_ON;
    }

    public static String configToString(int config) {
        switch (config) {
            case ROTATE_FOLLOW_SYSTEM:
                return "follow system";
            case ROTATE_FORCE_OFF:
                return "force off";
            case ROTATE_FORCE_ON:
                return "force on";
            default:
                return "unknown";
        }
    }
}
