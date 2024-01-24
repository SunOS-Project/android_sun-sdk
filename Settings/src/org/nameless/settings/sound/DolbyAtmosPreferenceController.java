/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.settings.sound;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import androidx.preference.Preference;

import com.android.internal.util.nameless.CustomUtils;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class DolbyAtmosPreferenceController extends BasePreferenceController {

    private final Context mContext;

    private final String mPackageName;
    private final String mClassName;

    public DolbyAtmosPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mContext = context;

        String config = context.getResources().getString(R.string.config_dolbyAtmosPackage);
        if (!TextUtils.isEmpty(config)) {
            String[] splited = config.split("/");
            if (splited.length == 2) {
                mPackageName = splited[0];
                mClassName = splited[1];
                return;
            }
        }
        mPackageName = "";
        mClassName = "";
    }

    @Override
    public int getAvailabilityStatus() {
        return !TextUtils.isEmpty(mPackageName) &&
                CustomUtils.isPackageInstalled(mContext, mPackageName, false)
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return false;
        }
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(mPackageName, mClassName));
            mContext.startActivity(intent);
        } catch (Exception e) {
        }
        return true;
    }
}

