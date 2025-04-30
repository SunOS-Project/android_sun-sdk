/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.settings.applications;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;

import com.android.internal.R;

import java.util.Arrays;
import java.util.List;

public class ApplicationUtils {

    private static List<String> sNonCloneableUserApps = null;

    public static boolean isAppCloneable(Context context, String packageName) {
        try {
            final PackageInfo pi = context.getPackageManager().getPackageInfo(packageName, 0);
            if (pi.applicationInfo.isResourceOverlay()) {
                return false;
            }
            final boolean isSystemApp = pi.applicationInfo.isSystemApp();
            if (sNonCloneableUserApps == null) {
                sNonCloneableUserApps = Arrays.asList(
                        context.getResources().getStringArray(R.array.config_nonCloneableUserAppList));
            }
            if (!isSystemApp && !sNonCloneableUserApps.contains(packageName)) {
                return true;
            }
            return false;
        } catch (NameNotFoundException ignored) {
            return false;
        }
    }
}
