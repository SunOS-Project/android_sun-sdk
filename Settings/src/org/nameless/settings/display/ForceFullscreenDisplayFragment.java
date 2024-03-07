/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.settings.display;

import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;

import java.util.List;

import org.nameless.custom.preference.SwitchPreferenceCompat;
import org.nameless.settings.fragment.PerAppSwitchConfigFragment;

public class ForceFullscreenDisplayFragment extends PerAppSwitchConfigFragment {

    @Override
    protected int getAllowedSystemAppListResId() {
        return R.array.config_fullscreenDisplayAllowedSystemApps;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.force_fullscreen_display;
    }

    @Override
    protected int getTopInfoResId() {
        return R.string.app_force_fullscreen_info;
    }

    @Override
    protected boolean isChecked(String packageName, int uid) {
        return mPackageManager.isForceFull(packageName);
    }

    @Override
    protected boolean onSetChecked(SwitchPreferenceCompat pref, String packageName, int uid, boolean checked) {
        new AlertDialog.Builder(mContext)
                .setTitle(mContext.getText(R.string.switch_fullscreen_display_warn_title))
                .setMessage(mContext.getText(R.string.switch_fullscreen_display_warn_content))
                .setPositiveButton(R.string.okay, (dialog, which) -> {
                    mPackageManager.setForceFull(packageName, checked);
                    pref.setChecked(checked);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
        return false;
    }

    @Override
    protected void onCheckedListUpdated(List<String> pkgList) {}
}
