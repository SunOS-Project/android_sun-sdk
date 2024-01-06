/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.internal.util.nameless;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;

import com.android.internal.R;
import com.android.internal.util.ArrayUtils;

public class CustomUtils {

    public static boolean isPackageInstalled(Context context, String pkg) {
        return isPackageInstalled(context, pkg, true);
    }

    public static boolean isPackageInstalled(Context context, String pkg, boolean ignoreState) {
        if (pkg != null) {
            try {
                final PackageInfo pi = context.getPackageManager().getPackageInfo(pkg, 0);
                if (!pi.applicationInfo.enabled && !ignoreState) {
                    return false;
                }
            } catch (NameNotFoundException e) {
                return false;
            }
        }
        return true;
    }

    public static boolean isUdfpsAvailable(Context context) {
        final int[] udfpsProps = context.getResources().getIntArray(
                R.array.config_udfps_sensor_props);
        return !ArrayUtils.isEmpty(udfpsProps);
    }
}
