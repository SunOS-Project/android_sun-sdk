/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.settings.display;

import static org.nameless.provider.SettingsExt.System.NETWORK_TRAFFIC_AUTOHIDE_THRESHOLD;
import static org.nameless.provider.SettingsExt.System.NETWORK_TRAFFIC_MODE;
import static org.nameless.provider.SettingsExt.System.NETWORK_TRAFFIC_REFRESH_INTERVAL;
import static org.nameless.provider.SettingsExt.System.NETWORK_TRAFFIC_STATE;

import android.app.settings.SettingsEnums;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Switch;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import com.android.settingslib.widget.OnMainSwitchChangeListener;

import org.nameless.custom.preference.SystemSettingListPreference;
import org.nameless.custom.preference.SystemSettingSeekBarPreference;
import org.nameless.settings.preference.SystemSettingMainSwitchPreference;

public class NetworkTrafficFragment extends SettingsPreferenceFragment
        implements OnMainSwitchChangeListener {

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
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        mIndicatorMode.setEnabled(isChecked);
        mThreshold.setEnabled(isChecked);
        mInterval.setEnabled(isChecked);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.PAGE_UNKNOWN;
    }
}
