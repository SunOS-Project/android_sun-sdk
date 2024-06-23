/*
 * Copyright (C) 2022-2023 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.server.vibrator;

import static org.nameless.content.ContextExt.LINEARMOTOR_VIBRATOR_SERVICE;
import static org.nameless.os.DebugConstants.DEBUG_OP_LM;

import android.os.Binder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Slog;

import com.oplus.os.ILinearmotorVibratorService;
import com.oplus.os.WaveformEffect;

import org.nameless.server.NamelessSystemExService;

public class LinearmotorVibratorController {

    private static final String TAG = "LinearmotorVibratorController";

    private static final VibrationEffect EFFECT_CLICK =
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK);

    private static class InstanceHolder {
        private static LinearmotorVibratorController INSTANCE = new LinearmotorVibratorController();
    }

    public static LinearmotorVibratorController getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private NamelessSystemExService mSystemExService;
    private Vibrator mVibrator;

    private final class LinearmotorVibratorService extends ILinearmotorVibratorService.Stub {
        @Override
        public void vibrate(WaveformEffect effect) {
            if (mVibrator == null) {
                return;
            }
            logD("vibrate, WaveformEffect: " + effect);
            final long ident = Binder.clearCallingIdentity();
            try {
                mVibrator.vibrate(EFFECT_CLICK); // TODO: Use different effect for each WaveformEffect id.
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        @Override
        public void cancelVibrate() {
            if (mVibrator == null) {
                return;
            }
            final long ident = Binder.clearCallingIdentity();
            try {
                mVibrator.cancel();
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    public void initSystemExService(NamelessSystemExService service) {
        mSystemExService = service;
        mSystemExService.publishBinderService(LINEARMOTOR_VIBRATOR_SERVICE,
                new LinearmotorVibratorService());
    }

    public void onSystemServicesReady() {
        mVibrator = mSystemExService.getContext().getSystemService(Vibrator.class);
    }

    private static void logD(String msg) {
        if (DEBUG_OP_LM) {
            Slog.d(TAG, msg);
        }
    }
}
