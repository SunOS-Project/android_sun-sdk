/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.oplus.os;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemService;
import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;

import org.sun.content.ContextExt;

@SystemService(ContextExt.LINEARMOTOR_VIBRATOR_SERVICE)
public class LinearmotorVibrator {

    private static final String TAG = "LinearmotorVibrator";

    private final ILinearmotorVibratorService mService;

    /** @hide */
    public LinearmotorVibrator(Context context, ILinearmotorVibratorService service) {
        mService = service;
    }

    public void vibrate(@Nullable WaveformEffect effect) {
        if (effect == null) {
            Slog.w(TAG, "Ignore vibrate in favor of invalid params.");
            return;
        }
        try {
            mService.vibrate(effect);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void cancelVibrate(@Nullable WaveformEffect effect, @NonNull IBinder token) {
        try {
            mService.cancelVibrate();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
}
