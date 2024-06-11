/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.server.pm;

import static org.nameless.os.DebugConstants.DEBUG_PMS;

import android.content.ComponentName;
import android.os.UserHandle;
import android.util.ArraySet;
import android.util.Slog;

import com.android.internal.R;

import com.android.server.pm.parsing.pkg.AndroidPackageUtils;

class PackageManagerServiceExt {

    private static final String TAG = "PackageManagerServiceExt";

    private final ArraySet<ComponentName> mDisabledComponentsList = new ArraySet<>();
    private final ArraySet<ComponentName> mForceEnabledComponentsList = new ArraySet<>();

    private PackageManagerService mPackageManagerService;

    private static class InstanceHolder {
        private static final PackageManagerServiceExt INSTANCE = new PackageManagerServiceExt();
    }

    static PackageManagerServiceExt getInstance() {
        return InstanceHolder.INSTANCE;
    }

    void init(PackageManagerService pms) {
        mPackageManagerService = pms;
        enableComponents(pms.mContext.getResources().getStringArray(
                R.array.config_deviceDisabledComponents), false);
        enableComponents(pms.mContext.getResources().getStringArray(
                R.array.config_globallyDisabledComponents), false);
        enableComponents(pms.mContext.getResources().getStringArray(
                R.array.config_forceEnabledComponents), true);
    }

    boolean isComponentDisabled(ComponentName component) {
        return mDisabledComponentsList.contains(component);
    }

    boolean isComponentForceEnabled(ComponentName component) {
        return mForceEnabledComponentsList.contains(component);
    }

    private void enableComponents(String[] components, boolean enable) {
        for (String name : components) {
            final ComponentName cn = ComponentName.unflattenFromString(name);
            if (!enable && !mDisabledComponentsList.contains(cn)) {
                mDisabledComponentsList.add(cn);
            }
            if (enable && !mForceEnabledComponentsList.contains(cn)) {
                mForceEnabledComponentsList.add(cn);
            }
            if (DEBUG_PMS) {
                Slog.d(TAG, "Changing enabled state of " + name + " to " + enable);
            }
            final String className = cn.getClassName();
            final PackageSetting pkgSetting =
                    mPackageManagerService.mSettings.mPackages.get(cn.getPackageName());
            if (pkgSetting == null || pkgSetting.getPkg() == null
                    || !AndroidPackageUtils.hasComponentClassName(pkgSetting.getPkg(), className)) {
                Slog.w(TAG, "Unable to change enabled state of " + name + " to " + enable);
                continue;
            }
            if (enable) {
                pkgSetting.enableComponentLPw(className, UserHandle.USER_OWNER);
            } else {
                pkgSetting.disableComponentLPw(className, UserHandle.USER_OWNER);
            }
        }
    }
}
