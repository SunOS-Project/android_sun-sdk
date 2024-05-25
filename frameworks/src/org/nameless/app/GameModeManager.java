/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.app;

import android.annotation.SystemService;
import android.content.Context;
import android.os.RemoteException;
import android.util.Slog;

import org.nameless.content.ContextExt;

/** @hide */
@SystemService(ContextExt.GAME_MODE_SERVICE)
public class GameModeManager {

    private static final String TAG = "GameModeManager";

    public static final int IN_GAME_CALL_NO_ACTION = 0;
    public static final int IN_GAME_CALL_AUTO_ACCEPT = 1;
    public static final int IN_GAME_CALL_AUTO_REJECT = 2;

    private final IGameModeManagerService mService;

    public GameModeManager(Context context, IGameModeManagerService service) {
        mService = service;
    }

    public boolean addGame(String packageName) {
        if (mService == null) {
            Slog.e(TAG, "Failed to add game. Service is null");
            return false;
        }
        try {
            return mService.addGame(packageName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean removeGame(String packageName) {
        if (mService == null) {
            Slog.e(TAG, "Failed to remove game. Service is null");
            return false;
        }
        try {
            return mService.removeGame(packageName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isAppGame(String packageName) {
        if (mService == null) {
            Slog.e(TAG, "Failed to check if app is game. Service is null");
            return false;
        }
        try {
            return mService.isAppGame(packageName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public GameModeInfo getGameModeInfo() {
        if (mService == null) {
            Slog.e(TAG, "Failed to get game mode info. Service is null");
            return null;
        }
        try {
            return mService.getGameModeInfo();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean registerGameModeInfoListener(IGameModeInfoListener.Stub listener) {
        if (mService == null) {
            Slog.e(TAG, "Failed to register game mode info listener. Service is null");
            return false;
        }
        try {
            return mService.registerGameModeInfoListener(listener);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean unregisterGameModeInfoListener(IGameModeInfoListener.Stub listener) {
        if (mService == null) {
            Slog.e(TAG, "Failed to unregister game mode info listener. Service is null");
            return false;
        }
        try {
            return mService.unregisterGameModeInfoListener(listener);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static String callActionToString(int action) {
        switch (action) {
            case IN_GAME_CALL_NO_ACTION:
                return "NO_ACTION";
            case IN_GAME_CALL_AUTO_ACCEPT:
                return "AUTO_ACCEPT";
            case IN_GAME_CALL_AUTO_REJECT:
                return "AUTO_REJECT";
            default:
                return "UNKNOWN";
        }
    }
}
