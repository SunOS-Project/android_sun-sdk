/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.settings.fuelgauge.optimizedcharge;

import android.content.Context;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import androidx.preference.PreferenceScreen;

import com.android.internal.util.nameless.BatteryFeatureSettingsHelper;

import com.android.settings.core.TogglePreferenceController;

import com.android.settingslib.widget.MainSwitchPreference;

public class OptimizedChargeEnablePreferenceController extends
        TogglePreferenceController implements OnCheckedChangeListener {

    private final Context mContext;

    private MainSwitchPreference mPreference;        

    public OptimizedChargeEnablePreferenceController(Context context, String key) {
        super(context, key);
        mContext = context;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return 0;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mPreference = (MainSwitchPreference) screen.findPreference(getPreferenceKey());
        mPreference.addOnSwitchChangeListener(this);
        mPreference.updateStatus(BatteryFeatureSettingsHelper.getOptimizedChargingEnabled(mContext));
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        final boolean enabled = isChecked();
        if (isChecked != enabled) {
            setChecked(isChecked);
        }
    }

    @Override
    public boolean isChecked() {
        return BatteryFeatureSettingsHelper.getOptimizedChargingEnabled(mContext);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        BatteryFeatureSettingsHelper.setOptimizedChargingEnabled(mContext, isChecked);
        return true;
    }
}
