/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.settings.sound;

import static org.sun.provider.SettingsExt.System.VOLUME_PANEL_POSITION_LAND;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;

import org.sun.custom.preference.SystemSettingListPreference;

public class VolumePanelPosLandPreferenceController extends BasePreferenceController {

    private SystemSettingListPreference mPreference;

    public VolumePanelPosLandPreferenceController(Context context, String key) {
        super(context, key);
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
    public void updateState(Preference preference) {
        final int posLandDefault = mContext.getResources().getInteger(
                com.android.internal.R.integer.config_volumePanelPosLandDefault);
        mPreference.setValue(String.valueOf(Settings.System.getIntForUser(
                mContext.getContentResolver(),
                VOLUME_PANEL_POSITION_LAND, posLandDefault, UserHandle.USER_CURRENT)));
        mPreference.setSummary(mPreference.getEntry());
    }
}
