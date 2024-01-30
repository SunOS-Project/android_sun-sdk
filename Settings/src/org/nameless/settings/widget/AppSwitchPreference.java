/*
 * Copyright (C) 2023 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.settings.widget;

import android.content.Context;
import android.util.AttributeSet;

import com.android.settings.R;

import org.nameless.custom.preference.SwitchPreference;

/**
 * The SwitchPreference for the pages need to show apps icon.
*/
public class AppSwitchPreference extends SwitchPreference {

    public AppSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(R.layout.preference_app);
    }

    public AppSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayoutResource(R.layout.preference_app);
    }

    public AppSwitchPreference(Context context) {
        super(context);
        setLayoutResource(R.layout.preference_app);
    }

    public AppSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.preference_app);
    }
}
