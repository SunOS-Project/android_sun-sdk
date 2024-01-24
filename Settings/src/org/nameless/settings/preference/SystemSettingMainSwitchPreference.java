/*
 * Copyright (C) 2022 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.settings.preference;

import android.content.Context;
import android.util.AttributeSet;

import com.android.settingslib.widget.MainSwitchPreference;

import org.nameless.custom.preference.SystemSettingsStore;

public class SystemSettingMainSwitchPreference extends MainSwitchPreference {

    public SystemSettingMainSwitchPreference(Context context) {
        super(context);
        setPreferenceDataStore(new SystemSettingsStore(context.getContentResolver()));
    }

    public SystemSettingMainSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setPreferenceDataStore(new SystemSettingsStore(context.getContentResolver()));
    }

    public SystemSettingMainSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setPreferenceDataStore(new SystemSettingsStore(context.getContentResolver()));
    }


    public SystemSettingMainSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setPreferenceDataStore(new SystemSettingsStore(context.getContentResolver()));
    }
}
