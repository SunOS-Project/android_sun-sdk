/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.settings.accessibility;

import android.content.Context;
import android.os.Handler;
import android.os.UserHandle;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;

import java.util.ArrayList;
import java.util.Set;

import org.sun.os.VibrationPatternManager;

public abstract class VibrationPatternPreferenceController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener {

    private final Context mContext;
    private final Handler mHandler;
    private final Vibrator mVibrator;

    private final Runnable mVibrateRunnable =
        new Runnable() {
            @Override
            public void run() {
                final VibrationEffect effect = VibrationPatternManager.getPreviewVibrationFromNumber(
                        getPatternNumber(), getType());
                mVibrator.vibrate(effect, getAttribute());
            }
        };

    private ListPreference mPreference;

    public VibrationPatternPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mContext = context;
        mHandler = new Handler();
        mVibrator = context.getSystemService(Vibrator.class);
    }

    public abstract String getSettings();

    public abstract int getType();

    public abstract VibrationAttributes getAttribute();

    private int getPatternNumber() {
        final int ret = Settings.System.getIntForUser(
                mContext.getContentResolver(),
                getSettings(),
                0, UserHandle.USER_CURRENT);
        if (ret >= VibrationPatternManager.getPatternsSize(getType())) {
            return 0;
        }
        return ret;
    }

    private void setPatternNumber(int value) {
        Settings.System.putIntForUser(
                mContext.getContentResolver(),
                getSettings(),
                value, UserHandle.USER_CURRENT);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mPreference = screen.findPreference(getPreferenceKey());

        final Set<Integer> patternSet = VibrationPatternManager.getPatternsNameIdSet(getType());
        CharSequence[] entries = new CharSequence[patternSet.size()];
        int idx = 0;
        for (int patternId : patternSet) {
            entries[idx++] = mContext.getResources().getString(patternId);
        }

        CharSequence[] values = new CharSequence[patternSet.size()];
        for (int i = 0; i < patternSet.size(); ++i) {
            values[i] = String.valueOf(i);
        }

        mPreference.setEntries(entries);
        mPreference.setEntryValues(values);
        mPreference.setValue(String.valueOf(getPatternNumber()));
        mPreference.setSummary(mPreference.getEntry());
        mPreference.setOnPreferenceChangeListener(this);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public final boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mPreference) {
            final int value = Integer.parseInt(newValue.toString());
            setPatternNumber(value);
            mHandler.post(mVibrateRunnable);

            CharSequence[] entries = mPreference.getEntries();
            mPreference.setSummary(entries[value]);
        }
        return true;
    }
}
