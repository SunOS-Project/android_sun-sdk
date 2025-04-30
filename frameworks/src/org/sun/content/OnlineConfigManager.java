/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.content;

import android.annotation.SystemService;
import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Slog;

import java.util.List;

import org.sun.content.ContextExt;

/** @hide */
@SystemService(ContextExt.ONLINE_CONFIG_MANAGER_SERVICE)
public class OnlineConfigManager {

    private static final String TAG = "OnlineConfigManager";

    public static final String ACTION_UPDATE_CONFIG = "org.sun.intent.UPDATE_ONLINE_CONFIG";

    private final IOnlineConfigManagerService mService;

    public OnlineConfigManager(Context context, IOnlineConfigManagerService service) {
        mService = service;
    }

    public List<IOnlineConfigurable> getRegisteredClients() {
        if (mService == null) {
            Slog.e(TAG, "Failed to get registered clients. Service is null");
            return null;
        }
        try {
            return mService.getRegisteredClients();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean registerOnlineConfigurable(IOnlineConfigurable.Stub configurable) {
        if (mService == null) {
            Slog.e(TAG, "Failed to register online config. Service is null");
            return false;
        }
        try {
            return mService.registerOnlineConfigurable(configurable);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean unregisterOnlineConfigurable(IOnlineConfigurable.Stub configurable) {
        if (mService == null) {
            Slog.e(TAG, "Failed to unregister online config. Service is null");
            return false;
        }
        try {
            return mService.unregisterOnlineConfigurable(configurable);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static void sendUpdateBroadcast(Context context) {
        context.sendBroadcastAsUser(new Intent(ACTION_UPDATE_CONFIG), UserHandle.SYSTEM);
    }
}
