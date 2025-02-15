/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.settings.accessibility;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import java.util.ArrayList;
import java.util.List;

import org.nameless.os.VibratorExtManager;
import org.nameless.os.VibratorExtManager.HapticStyleChangedCallback;
import org.nameless.provider.SettingsExt;

import vendor.nameless.hardware.vibratorExt.Style;

public class HapticStyleListPreferenceController extends BasePreferenceController
        implements OnPreferenceChangeListener, LifecycleObserver, OnStart, OnStop {

    private final VibratorExtManager mVibratorExtManager = VibratorExtManager.getInstance();

    private final CustomVibrationPreferenceConfig mPreferenceConfig;
    private final CustomVibrationPreferenceConfig.SettingObserver mSettingsContentObserver;

    private final HapticStyleChangedCallback mCallback = new HapticStyleChangedCallback() {
        @Override
        public void onHapticStyleChanged() {
            mPreferenceConfig.playVibrationPreview();
        }
    };

    private final ArrayList<Integer> mHapticStyles;

    private ListPreference mPreference;

    public HapticStyleListPreferenceController(Context context, String key) {
        super(context, key);
        mHapticStyles = mVibratorExtManager.getValidHapticStyles();
        mPreferenceConfig = new CustomVibrationPreferenceConfig(context,
                Settings.System.HAPTIC_FEEDBACK_ENABLED, SettingsExt.System.VIBRATOR_EXT_HAPTIC_STYLE);
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
        mPreference = screen.findPreference(getPreferenceKey());

        final List<String> entries = new ArrayList<>();
        final List<String> values = new ArrayList<>();
        for (int i = 0; i < mHapticStyles.size(); ++i) {
            entries.add(mVibratorExtManager.getHapticStyleSummary(mContext, mHapticStyles.get(i)));
            values.add(String.valueOf(mHapticStyles.get(i)));
        }
        mPreference.setEntries(entries.toArray(new String[entries.size()]));
        mPreference.setEntryValues(values.toArray(new String[values.size()]));

        mSettingsContentObserver.onDisplayPreference(this, mPreference);
        mPreference.setEnabled(mPreferenceConfig.isPreferenceEnabled());
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        final int style = mPreferenceConfig.readValue(Style.CRISP);
        final int index = mPreference.findIndexOfValue(String.valueOf(style));
        if (index != -1) {
            mPreference.setValueIndex(index);
            mPreference.setSummary(mPreference.getEntries()[index]);
        } else {
            mPreference.setSummary(mContext.getString(R.string.haptic_style_unknown));
        }
        mPreference.setEnabled(mPreferenceConfig.isPreferenceEnabled());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final int style = Integer.parseInt((String) newValue);
        mPreferenceConfig.setValue(style);
        updateState(preference);
        mVibratorExtManager.setHapticStyle(style, mCallback);
        return true;
    }

    @Override
    public int getAvailabilityStatus() {
        return mHapticStyles.size() > 1 ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }
}
