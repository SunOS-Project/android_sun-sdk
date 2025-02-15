/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.settings.accessibility;

import android.content.Context;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.SliderPreferenceController;
import com.android.settings.widget.LabeledSeekBarPreference;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import org.nameless.os.VibratorExtManager;
import org.nameless.os.VibratorExtManager.StrengthLevelChangedCallback;

import vendor.nameless.hardware.vibratorExt.LevelRange;

public abstract class VibrationStrengthSeekBarPreferenceController extends SliderPreferenceController
        implements LifecycleObserver, OnStart, OnStop {

    private static final VibrationAttributes ACCESSIBILITY_VIBRATION_ATTRIBUTES =
            new VibrationAttributes.Builder(
                    VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ACCESSIBILITY))
            .setFlags(VibrationAttributes.FLAG_BYPASS_USER_VIBRATION_INTENSITY_OFF)
            .build();

    private static final int AMPLITUDE_BASE = 30;
    private static final int AMPLITUDE_MAX = 255;

    private static final long VIBRATION_DURATION = 30L;

    private final CustomVibrationPreferenceConfig mPreferenceConfig;
    private final CustomVibrationPreferenceConfig.SettingObserver mSettingsContentObserver;

    private final LevelRange mLevelRange;
    private final VibratorExtManager mVibratorExtManager;

    protected final Vibrator mVibrator;

    private final StrengthLevelChangedCallback mCallback = new StrengthLevelChangedCallback() {
        @Override
        public void onStrengthLevelChanged() {
            final int amplitude = AMPLITUDE_BASE + (int)
                    ((float) (AMPLITUDE_MAX - AMPLITUDE_BASE) /
                    (getMax() - getMin()) * (getSliderPosition() - getMin()));
            mVibrator.vibrate(VibrationEffect.createOneShot(VIBRATION_DURATION, amplitude),
                    ACCESSIBILITY_VIBRATION_ATTRIBUTES);
        }
    };

    public VibrationStrengthSeekBarPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);

        mVibratorExtManager = VibratorExtManager.getInstance();
        mLevelRange = mVibratorExtManager.getStrengthLevelRange(getVibrationType());

        mPreferenceConfig = new CustomVibrationPreferenceConfig(
                context, Settings.System.VIBRATE_ON, mVibratorExtManager.vibrationTypeToSettings(getVibrationType()));
        mSettingsContentObserver = new CustomVibrationPreferenceConfig.SettingObserver(
                mPreferenceConfig);

        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    @Override
    public void onStart() {
        mSettingsContentObserver.register(mContext);
    }

    @Override
    public void onStop() {
        mSettingsContentObserver.unregister(mContext);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final LabeledSeekBarPreference preference = screen.findPreference(getPreferenceKey());
        preference.setContinuousUpdates(true);
        preference.setMax(getMax());
        preference.setMin(getMin());
        mSettingsContentObserver.onDisplayPreference(this, preference);
        preference.setEnabled(mPreferenceConfig.isPreferenceEnabled());
    }

    @Override
    public int getAvailabilityStatus() {
        return mVibratorExtManager.getValidVibrationTypes().contains(getVibrationType())
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setEnabled(mPreferenceConfig.isPreferenceEnabled());
    }

    @Override
    public int getSliderPosition() {
        if (mLevelRange != null) {
            return mPreferenceConfig.readValue(mLevelRange.defaultLevel);
        }
        return 0;
    }

    @Override
    public boolean setSliderPosition(int position) {
        mPreferenceConfig.setValue(position);
        mVibratorExtManager.setStrengthLevel(getVibrationType(), position, mCallback);
        return true;
    }

    @Override
    public int getMax() {
        if (mLevelRange != null) {
            return mLevelRange.maxLevel;
        }
        return 0;
    }

    @Override
    public int getMin() {
        return 1;
    }

    public abstract int getVibrationType();
}
