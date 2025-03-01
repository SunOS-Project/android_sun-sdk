/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.os;

import static org.nameless.provider.SettingsExt.System.VIBRATOR_EXT_ALARM_CALL_STRENGTH_LEVEL;
import static org.nameless.provider.SettingsExt.System.VIBRATOR_EXT_HAPTIC_STRENGTH_LEVEL;
import static org.nameless.provider.SettingsExt.System.VIBRATOR_EXT_NOTIFICAITON_STRENGTH_LEVEL;

import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.ArrayMap;
import android.util.Slog;

import com.android.internal.R;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import vendor.nameless.hardware.vibratorExt.IVibratorExt;
import vendor.nameless.hardware.vibratorExt.LevelRange;
import vendor.nameless.hardware.vibratorExt.Style;
import vendor.nameless.hardware.vibratorExt.Type;

/** @hide */
public class VibratorExtManager {

    private static final String TAG = "VibratorExtManager";

    private static final String SERVICE_NAME = "vendor.nameless.hardware.vibratorExt.IVibratorExt/default";

    // Whether device supoorts richtap
    public static final boolean RICHTAP_SUPPORT =
            SystemProperties.getBoolean("sys.nameless.feature.vibrator.richtap", false);
    // Whether to use richtap for aosp effects and duration vibration
    public static final boolean RICHTAP_TAKEOVER_CTL = RICHTAP_SUPPORT &&
            SystemProperties.getBoolean("sys.nameless.feature.vibrator.richtap_effect", false);

    private final IVibratorExt mService;

    private static final Set<Integer> sAllVibrationTypes = Set.of(
        Type.ALARM_CALL,
        Type.HAPTIC,
        Type.NOTIFICATION
    );

    private static final List<Integer> sAllHapticStyles = List.of(
        Style.CRISP,
        Style.GENTLE
    );

    private final ArrayList<Integer> mValidVibrationTypes;
    private final ArrayList<Integer> mValidHapticStyles;
    private final ArrayMap<Integer, LevelRange> mLevelRanges;
    private final ArrayMap<Integer, Integer> mHapticStyleSummary;

    private static class InstanceHolder {
        private static final VibratorExtManager INSTANCE = new VibratorExtManager();
    }

    public static VibratorExtManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private VibratorExtManager() {
        IVibratorExt service;
        try {
            service = IVibratorExt.Stub.asInterface(ServiceManager.getService(SERVICE_NAME));
        } catch (NoSuchElementException e) {
            Slog.w(TAG, "vibratorExt HAL is not found");
            service = null;
        }
        mService = service;
        mValidVibrationTypes = new ArrayList<>();
        mValidHapticStyles = new ArrayList<>();
        mLevelRanges = new ArrayMap<>();
        mHapticStyleSummary = new ArrayMap<>();

        mHapticStyleSummary.put(Style.CRISP, R.string.haptic_style_crisp);
        mHapticStyleSummary.put(Style.GENTLE, R.string.haptic_style_gentle);

        if (mService != null) {
            for (int type : sAllVibrationTypes) {
                LevelRange range = null;
                try {
                    range = mService.getStrengthLevelRange(type);
                } catch (RemoteException e) {
                    Slog.e(TAG, "Failed to get vibrator strength level range", e);
                }
                if (isStrengthLevelRangeLegal(range)) {
                    mValidVibrationTypes.add(type);
                    mLevelRanges.put(type, range);
                }
            }

            for (int style : sAllHapticStyles) {
                boolean isSupported = false;
                try {
                    isSupported = mService.isHapticStyleSupported(style);
                } catch (RemoteException e) {
                    Slog.e(TAG, "Failed to check if haptic style is supported", e);
                }
                if (isSupported) {
                    mValidHapticStyles.add(style);
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
            Slog.e(TAG, "Failed to calibrate vibrator", e);
        }
    }

    public long vibratorOn(int effectId, long duration) {
        if (mService == null) {
            return -1;
        }
        try {
            return mService.vibratorOn(effectId, duration);
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to turn on vibrator for effectId: " + effectId +
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
            Slog.e(TAG, "Failed to turn off vibrator", e);
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

    public ArrayList<Integer> getValidHapticStyles() {
        return mValidHapticStyles;
    }

    public String getHapticStyleSummary(Context context, int style) {
        if (!mValidHapticStyles.contains(style)) {
            Slog.e(TAG, "getHapticStyleSummary, invalid haptic style: " + style);
            return "";
        }
        return context.getString(mHapticStyleSummary.get(style));
    }

    public void setAmplitude(float amplitude) {
        if (mService == null) {
            return;
        }
        if (amplitude <= 0f || amplitude > 1f) {
            Slog.w(TAG, "Invalid amplitude: " + amplitude + ", fallback to default amplitude");
            amplitude = 1f;
        }
        try {
            mService.setAmplitude(amplitude);
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to set amplitude", e);
        }
    }

    public void setHapticStyle(int style) {
        setHapticStyle(style, null);
    }

    public void setHapticStyle(int style, HapticStyleChangedCallback callback) {
        if (mService == null) {
            return;
        }
        if (!mValidHapticStyles.contains(style)) {
            Slog.e(TAG, "setHapticStyle, invalid haptic style: " + style);
            return;
        }
        try {
            mService.setHapticStyle(style);
            if (callback != null) {
                callback.onHapticStyleChanged();
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to set haptic style", e);
        }
    }

    public LevelRange getStrengthLevelRange(int type) {
        return mLevelRanges.getOrDefault(type, null);
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
            Slog.e(TAG, "Unable to set strength level: Strength level is illegal");
        }
        try {
            mService.setStrengthLevel(type, level);
            if (callback != null) {
                callback.onStrengthLevelChanged();
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to set vibrator strength level to " + level, e);
        }
    }

    public boolean isEffectSupported(int effectId) {
        if (mService == null) {
            return false;
        }
        try {
            return mService.isEffectSupported(effectId);
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to get effect supported status", e);
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

    public interface HapticStyleChangedCallback {
        void onHapticStyleChanged();
    }
}
