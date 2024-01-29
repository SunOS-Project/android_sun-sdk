/*
 * Copyright (C) 2023 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.settings.preference;

import android.content.Context;
import android.util.AttributeSet;

import com.android.settingslib.widget.MainSwitchPreference;

import org.nameless.custom.preference.GlobalSettingsStore;

public class GlobalSettingMainSwitchPreference extends MainSwitchPreference {

    public GlobalSettingMainSwitchPreference(Context context) {
        super(context);
        setPreferenceDataStore(new GlobalSettingsStore(context.getContentResolver()));
    }

    public GlobalSettingMainSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setPreferenceDataStore(new GlobalSettingsStore(context.getContentResolver()));
    }

    public GlobalSettingMainSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setPreferenceDataStore(new GlobalSettingsStore(context.getContentResolver()));
    }


    public GlobalSettingMainSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setPreferenceDataStore(new GlobalSettingsStore(context.getContentResolver()));
    }
}
