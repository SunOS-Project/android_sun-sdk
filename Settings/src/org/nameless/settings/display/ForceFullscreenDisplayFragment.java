/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.settings.display;

import com.android.settings.R;

import java.util.List;

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
    protected boolean onSetChecked(String packageName, int uid, boolean checked) {
        mPackageManager.setForceFull(packageName, checked);
        return true;
    }

    @Override
    protected void onCheckedListUpdated(List<String> pkgList) {}
}
