/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.settings.fuelgauge.reversecharging;

import static com.android.internal.util.nameless.BatteryFeatureSettingsHelper.REVERSE_CHARGING_SUSPENDED_CHARGING;
import static com.android.internal.util.nameless.BatteryFeatureSettingsHelper.REVERSE_CHARGING_SUSPENDED_LOW_POWER;
import static com.android.internal.util.nameless.BatteryFeatureSettingsHelper.REVERSE_CHARGING_SUSPENDED_POWER_SAVE;
import static com.android.internal.util.nameless.BatteryFeatureSettingsHelper.REVERSE_CHARGING_UNSUSPENDED;

import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.util.nameless.BatteryFeatureSettingsHelper;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.widget.CardPreference;

public class ReverseCharingSuspendPreferenceController extends BasePreferenceController {

    private final Context mContext;

    private CardPreference mPreference;

    public ReverseCharingSuspendPreferenceController(Context context, String key) {
        super(context, key);
        mContext = context;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public final void updateState(Preference preference) {
        final int status = BatteryFeatureSettingsHelper.getReverseChargingSuspendedStatus(mContext);
        mPreference.setVisible(BatteryFeatureSettingsHelper.getReverseChargingEnabled(mContext) &&
                status != REVERSE_CHARGING_UNSUSPENDED);
        switch (status) {
            case REVERSE_CHARGING_SUSPENDED_CHARGING:
                mPreference.setTitle(R.string.wireless_reverse_charging_suspended_charging);
                break;
            case REVERSE_CHARGING_SUSPENDED_LOW_POWER:
                mPreference.setTitle(R.string.wireless_reverse_charging_suspended_low_level);
                break;
            case REVERSE_CHARGING_SUSPENDED_POWER_SAVE:
                mPreference.setTitle(R.string.wireless_reverse_charging_suspended_power_save);
                break;
            default:
                break;
        }
    }
}
