/*
 * Copyright (C) 2023 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.systemui.cpuinfo;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;

public class CPUInfoHelper {

    public static boolean isServiceRunning(Context context) {
        final ActivityManager am = context.getSystemService(ActivityManager.class);
        for (ActivityManager.RunningServiceInfo info :
                am.getRunningServices(Integer.MAX_VALUE)) {
            if (CPUInfoService.class.getName().equals(info.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static void startService(Context context) {
        final Intent cpuinfo = new Intent(context, CPUInfoService.class);
        context.startServiceAsUser(cpuinfo, UserHandle.CURRENT);
    }

    public static void stppService(Context context) {
        final Intent cpuinfo = new Intent(context, CPUInfoService.class);
        context.stopServiceAsUser(cpuinfo, UserHandle.CURRENT);
    }
}
