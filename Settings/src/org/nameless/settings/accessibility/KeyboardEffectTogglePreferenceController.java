/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.settings.accessibility;

import static vendor.nameless.hardware.vibratorExt.Effect.KEYBOARD_PRESS;

import android.content.Context;
import android.os.VibrationEffect;
import android.os.VibrationExtInfo;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import org.nameless.os.VibratorExtManager;
import org.nameless.provider.SettingsExt;

public class KeyboardEffectTogglePreferenceController extends TogglePreferenceController
        implements LifecycleObserver, OnStart, OnStop {

    private final CustomVibrationPreferenceConfig mPreferenceConfig;
    private final CustomVibrationPreferenceConfig.SettingObserver mSettingsContentObserver;

    public KeyboardEffectTogglePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mPreferenceConfig = new CustomVibrationPreferenceConfig(
                context, Settings.System.HAPTIC_FEEDBACK_ENABLED, SettingsExt.System.IME_KEYBOARD_PRESS_EFFECT) {
            @Override
            public void playVibrationPreview() {
                if (readValue(0) == 1) {
                    mVibrator.vibrateExt(new VibrationExtInfo.Builder()
                            .setEffectId(KEYBOARD_PRESS)
                            .build());
                } else {
                    mVibrator.vibrate(PREVIEW_VIBRATION_EFFECT);
                }
            }
        };
        mSettingsContentObserver = new CustomVibrationPreferenceConfig.SettingObserver(
                mPreferenceConfig);
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
        final Preference preference = screen.findPreference(getPreferenceKey());
        mSettingsContentObserver.onDisplayPreference(this, preference);
        preference.setEnabled(mPreferenceConfig.isPreferenceEnabled());
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (preference != null) {
            preference.setEnabled(mPreferenceConfig.isPreferenceEnabled());
        }
    }

    @Override
    public boolean isChecked() {
        return mPreferenceConfig.isPreferenceEnabled() && mPreferenceConfig.readValue(0) == 1;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (!mPreferenceConfig.isPreferenceEnabled()) {
            // Ignore toggle updates when the preference is disabled.
            return false;
        }
        final boolean success = mPreferenceConfig.setValue(isChecked ? 1 : 0);

        if (success) {
            mPreferenceConfig.playVibrationPreview();
        }

        return success;
    }

    @Override
    public int getAvailabilityStatus() {
        return VibratorExtManager.getInstance().isEffectSupported(KEYBOARD_PRESS)
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_accessibility;
    }
}
