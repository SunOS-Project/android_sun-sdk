/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.os;

import static org.nameless.provider.SettingsExt.System.VIBRATOR_EXT_ALARM_CALL_STRENGTH_LEVEL;
import static org.nameless.provider.SettingsExt.System.VIBRATOR_EXT_HAPTIC_STRENGTH_LEVEL;
import static org.nameless.provider.SettingsExt.System.VIBRATOR_EXT_NOTIFICAITON_STRENGTH_LEVEL;

import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Set;

import vendor.nameless.hardware.vibratorExt.V1_0.IVibratorExt;
import vendor.nameless.hardware.vibratorExt.V1_0.LevelRange;
import vendor.nameless.hardware.vibratorExt.V1_0.Type;

/** @hide */
public class VibratorExtManager {

    private static final String TAG = "VibratorExtManager";

    private final IVibratorExt mService;

    private static final Set<Integer> sAllVibrationTypes = Set.of(
        Type.ALARM_CALL,
        Type.HAPTIC,
        Type.NOTIFICATION
    );

    private final ArrayList<Integer> mValidVibrationTypes;
    private final HashMap<Integer, LevelRange> mLevelRanges;

    private static class InstanceHolder {
        private static final VibratorExtManager INSTANCE = new VibratorExtManager();
    }

    public static VibratorExtManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private VibratorExtManager() {
        IVibratorExt service;
        try {
            service = IVibratorExt.getService();
        } catch (NoSuchElementException | RemoteException e) {
            Log.w(TAG, "vibratorExt HAL is not found");
            service = null;
        }
        mService = service;
        mValidVibrationTypes = new ArrayList<>();
        mLevelRanges = new HashMap<>();

        if (mService != null) {
            for (int type : sAllVibrationTypes) {
                LevelRange range = null;
                try {
                    range = mService.getStrengthLevelRange(type);
                } catch (RemoteException e) {
                    Log.e(TAG, "Failed to get vibrator strength level range", e);
                }
                if (isStrengthLevelRangeLegal(range)) {
                    mValidVibrationTypes.add(type);
                    mLevelRanges.put(type, range);
                }
            }
        }
    }

    public boolean isSupported() {
        return mService != null;
    }

    public void initVibrator() {
        if (mService == null) {
            return;
        }
        try {
            mService.initVibrator();
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to calibrate vibrator", e);
        }
    }

    public long vibratorOn(int effectId, long duration) {
        if (mService == null) {
            return -1;
        }
        try {
            return mService.vibratorOn(effectId, duration);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to turn on vibrator for effectId: " + effectId +
                    ", duration: " + duration, e);
        }
        return -1;
    }

    public void vibratorOff() {
        if (mService == null) {
            return;
        }
        try {
            mService.vibratorOff();
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to turn off vibrator", e);
        }
    }

    public String vibrationTypeToSettings(int type) {
        switch (type) {
            case Type.ALARM_CALL:
                return VIBRATOR_EXT_ALARM_CALL_STRENGTH_LEVEL;
            case Type.HAPTIC:
                return VIBRATOR_EXT_HAPTIC_STRENGTH_LEVEL;
            case Type.NOTIFICATION:
                return VIBRATOR_EXT_NOTIFICAITON_STRENGTH_LEVEL;
            default:
                break;
        }
        return "";
    }

    public ArrayList<Integer> getValidVibrationTypes() {
        return mValidVibrationTypes;
    }

    public void setAmplitude(float amplitude) {
        if (mService == null) {
            return;
        }
        if (amplitude <= 0f || amplitude > 1f) {
            Log.w(TAG, "Invalid amplitude: " + amplitude + ", fallback to default amplitude");
            amplitude = 1f;
        }
        try {
            mService.setAmplitude(amplitude);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to set amplitude", e);
        }
    }

    public LevelRange getStrengthLevelRange(int type) {
        return mLevelRanges.getOrDefault(type, null);
    }

    public int getStrengthLevel(int type) {
        if (mService == null) {
            return -1;
        }
        if (!mValidVibrationTypes.contains(type)) {
            return -1;
        }
        try {
            return mService.getStrengthLevel(type);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to get vibrator strength level", e);
        }
        return -1;
    }

    public void setStrengthLevel(int type, int level) {
        setStrengthLevel(type, level, null);
    }

    public void setStrengthLevel(int type, int level, StrengthLevelChangedCallback callback) {
        if (mService == null) {
            return;
        }
        if (!mValidVibrationTypes.contains(type)) {
            return;
        }
        if (!isStrengthLevelLegal(type, level)) {
            Log.e(TAG, "Unable to set strength level: Strength level is illegal");
        }
        try {
            mService.setStrengthLevel(type, level);
            if (callback != null) {
                callback.onStrengthLevelChanged();
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to set vibrator strength level to " + level, e);
        }
    }

    public boolean isEffectSupported(int effectId) {
        if (mService == null) {
            return false;
        }
        try {
            return mService.isEffectSupported(effectId);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to get effect supported status", e);
        }
        return false;
    }

    private boolean isStrengthLevelRangeLegal(LevelRange range) {
        if (range == null) {
            return false;
        }
        return range.maxLevel > 1 && range.defaultLevel <= range.maxLevel;
    }

    private boolean isStrengthLevelLegal(int type, int level) {
        final LevelRange range = getStrengthLevelRange(type);
        if (range == null) {
            return false;
        }
        return level >= 1 && level <= range.maxLevel;
    }

    public interface StrengthLevelChangedCallback {
        void onStrengthLevelChanged();
    }
}
