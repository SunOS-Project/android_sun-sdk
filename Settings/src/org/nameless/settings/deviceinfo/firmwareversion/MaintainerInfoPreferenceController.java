/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.settings.deviceinfo.firmwareversion;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.text.TextUtils;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class MaintainerInfoPreferenceController extends BasePreferenceController {

    private final String mMaintainerName;
    private final String mMaintainerLink;

    private final PackageManager mPackageManager;

    private Uri mIntentUri = null;

    public MaintainerInfoPreferenceController(Context context, String key) {
        super(context, key);

        mMaintainerName = context.getResources().getString(R.string.config_maintainer_name);
        mMaintainerLink = context.getResources().getString(R.string.config_maintainer_link);

        mPackageManager = context.getPackageManager();

        if (!TextUtils.isEmpty(mMaintainerLink)) {
            mIntentUri = Uri.parse(mMaintainerLink);
        }
    }

    public int getAvailabilityStatus() {
        return !TextUtils.isEmpty(mMaintainerName) && !TextUtils.isEmpty(mMaintainerLink)
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public CharSequence getSummary() {
        return mMaintainerName;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return false;
        }

        final Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(mIntentUri);
        if (mPackageManager.queryIntentActivities(intent, 0).isEmpty()) {
            return true;
        }

        try {
            mContext.startActivity(intent);
        } catch (Exception ignored) {}
        return true;
    }
}
