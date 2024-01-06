/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.view;

import android.annotation.SystemService;
import android.content.ComponentName;
import android.content.Context;
import android.os.RemoteException;
import android.util.Slog;

import org.nameless.content.ContextExt;

/** @hide */
@SystemService(ContextExt.APP_FOCUS_MANAGER_SERVICE)
public class AppFocusManager {

    private static final String TAG = "AppFocusManager";

    private final IAppFocusManagerService mService;

    public AppFocusManager(Context context, IAppFocusManagerService service) {
        mService = service;
    }

    /** @hide */
    public ComponentName getTopFullscreenComponent() {
        if (mService == null) {
            Slog.e(TAG, "Failed to get top fullscreen component. Service is null");
            return null;
        }
        try {
            return mService.getTopFullscreenComponent();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** @hide */
    public boolean registerAppFocusObserver(IAppFocusObserver.Stub listener) {
        return registerAppFocusObserver(listener, false);
    }

    /** @hide */
    public boolean registerAppFocusObserver(IAppFocusObserver.Stub listener, boolean observeActivity) {
        if (mService == null) {
            Slog.e(TAG, "Failed to register app focus observer. Service is null");
            return false;
        }
        try {
            return mService.registerAppFocusObserver(listener, observeActivity);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** @hide */
    public boolean unregisterAppFocusObserver(IAppFocusObserver.Stub listener) {
        if (mService == null) {
            Slog.e(TAG, "Failed to unregister app focus observer. Service is null");
            return false;
        }
        try {
            return mService.unregisterAppFocusObserver(listener);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
}
