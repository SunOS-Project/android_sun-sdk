/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.server.pm;

import static android.os.UserManager.USER_TYPE_PROFILE_CLONE;

import com.android.internal.R;

import com.android.server.pm.pkg.AndroidPackage;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

class UserSystemPackageInstallerExt {

    private final HashSet<String> mNonCloneableSystemApps = new HashSet<>();

    private static class InstanceHolder {
        private static final UserSystemPackageInstallerExt INSTANCE = new UserSystemPackageInstallerExt();
    }

    static UserSystemPackageInstallerExt getInstance() {
        return InstanceHolder.INSTANCE;
    }

    void init(UserManagerService ums) {
        final List<String> nonCloneableSystemApps = Arrays.asList(ums.getContext()
                .getResources().getStringArray(R.array.config_nonCloneableSystemAppList));
        for (String app : nonCloneableSystemApps) {
            mNonCloneableSystemApps.add(app);
        }
    }

    boolean interceptInstallPackage(AndroidPackage sysPkg, String userType) {
        if (USER_TYPE_PROFILE_CLONE.equals(userType)) {
            if (mNonCloneableSystemApps.contains(sysPkg.getPackageName())) {
                return true;
            }
        }
        return false;
    }
}
