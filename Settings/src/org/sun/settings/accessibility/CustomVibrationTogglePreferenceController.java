/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.settings.accessibility;

import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

/** Abstract preference controller for a custom vibration setting, that has only ON/OFF states */
public abstract class CustomVibrationTogglePreferenceController extends TogglePreferenceController
        implements LifecycleObserver, OnStart, OnStop {

    protected final CustomVibrationPreferenceConfig mPreferenceConfig;
    private final CustomVibrationPreferenceConfig.SettingObserver mSettingsContentObserver;
    private final int mDefaultValue;

    protected CustomVibrationTogglePreferenceController(Context context, String preferenceKey,
            CustomVibrationPreferenceConfig preferenceConfig) {
        this(context, preferenceKey, preferenceConfig, 1);
    }

    protected CustomVibrationTogglePreferenceController(Context context, String preferenceKey,
            CustomVibrationPreferenceConfig preferenceConfig, int defaultValue) {
        super(context, preferenceKey);
        mPreferenceConfig = preferenceConfig;
        mSettingsContentObserver = new CustomVibrationPreferenceConfig.SettingObserver(
                preferenceConfig);
        mDefaultValue = defaultValue;
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
        return mPreferenceConfig.isPreferenceEnabled() && mPreferenceConfig.readValue(mDefaultValue) == 1;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (!mPreferenceConfig.isPreferenceEnabled()) {
            // Ignore toggle updates when the preference is disabled.
            return false;
        }
        final boolean success = mPreferenceConfig.setValue(isChecked ? 1 : 0);

        if (success && isChecked) {
            mPreferenceConfig.playVibrationPreview();
        }

        return success;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_accessibility;
    }
}
