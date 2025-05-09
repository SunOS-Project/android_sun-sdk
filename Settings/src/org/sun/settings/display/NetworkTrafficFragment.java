/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.settings.display;

import static org.sun.provider.SettingsExt.System.NETWORK_TRAFFIC_AUTOHIDE_THRESHOLD;
import static org.sun.provider.SettingsExt.System.NETWORK_TRAFFIC_MODE;
import static org.sun.provider.SettingsExt.System.NETWORK_TRAFFIC_REFRESH_INTERVAL;
import static org.sun.provider.SettingsExt.System.NETWORK_TRAFFIC_STATE;

import android.app.settings.SettingsEnums;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import org.sun.custom.preference.SystemSettingListPreference;
import org.sun.custom.preference.SystemSettingSeekBarPreference;
import org.sun.settings.preference.SystemSettingMainSwitchPreference;

public class NetworkTrafficFragment extends SettingsPreferenceFragment
        implements OnCheckedChangeListener {

    private SystemSettingListPreference mIndicatorMode;
    private SystemSettingSeekBarPreference mThreshold;
    private SystemSettingSeekBarPreference mInterval;

    private SystemSettingMainSwitchPreference mSwitchBar;

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        addPreferencesFromResource(R.xml.network_traffic);

        final boolean enabled = Settings.System.getInt(getContentResolver(),
                NETWORK_TRAFFIC_STATE, 0) == 1;

        mSwitchBar = (SystemSettingMainSwitchPreference) findPreference(NETWORK_TRAFFIC_STATE);
        mSwitchBar.addOnSwitchChangeListener(this);

        mIndicatorMode = (SystemSettingListPreference) findPreference(NETWORK_TRAFFIC_MODE);
        mIndicatorMode.setEnabled(enabled);

        mThreshold = (SystemSettingSeekBarPreference) findPreference(NETWORK_TRAFFIC_AUTOHIDE_THRESHOLD);
        mThreshold.setEnabled(enabled);

        mInterval = (SystemSettingSeekBarPreference) findPreference(NETWORK_TRAFFIC_REFRESH_INTERVAL);
        mInterval.setEnabled(enabled);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        mIndicatorMode.setEnabled(isChecked);
        mThreshold.setEnabled(isChecked);
        mInterval.setEnabled(isChecked);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.PAGE_UNKNOWN;
    }
}
