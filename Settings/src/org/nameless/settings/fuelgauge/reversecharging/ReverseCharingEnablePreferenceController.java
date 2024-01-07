/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.settings.fuelgauge.reversecharging;

import android.content.Context;
import android.widget.Switch;

import androidx.preference.PreferenceScreen;

import com.android.internal.util.nameless.BatteryFeatureSettingsHelper;

import com.android.settings.core.TogglePreferenceController;

import com.android.settingslib.widget.MainSwitchPreference;
import com.android.settingslib.widget.OnMainSwitchChangeListener;

public class ReverseCharingEnablePreferenceController extends
        TogglePreferenceController implements OnMainSwitchChangeListener {

    private final Context mContext;

    private MainSwitchPreference mPreference;        

    public ReverseCharingEnablePreferenceController(Context context, String key) {
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
        mPreference.updateStatus(BatteryFeatureSettingsHelper.getReverseChargingEnabled(mContext));
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        final boolean enabled = isChecked();
        if (isChecked != enabled) {
            setChecked(isChecked);
        }
    }

    @Override
    public boolean isChecked() {
        return BatteryFeatureSettingsHelper.getReverseChargingEnabled(mContext);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        BatteryFeatureSettingsHelper.setReverseChargingEnabled(mContext, isChecked);
        return true;
    }
}
