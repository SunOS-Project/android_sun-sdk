/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.internal.util.nameless;

import static android.app.ActivityTaskManager.INVALID_TASK_ID;
import static android.app.WindowConfiguration.WINDOWING_MODE_FREEFORM;
import static android.app.WindowConfiguration.WINDOWING_MODE_PINNED_WINDOW_EXT;
import static android.app.WindowConfiguration.WINDOWING_MODE_MINI_WINDOW_EXT;

import static org.nameless.view.PopUpViewManager.ACTION_PIN_CURRENT_APP;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.ActivityOptions;
import android.app.IActivityManager;
import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.text.TextUtils;
import android.view.IWindowManager;
import android.view.WindowManagerGlobal;
import android.widget.Toast;

import com.android.internal.R;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.CollectionUtils;

import java.util.List;

/** @hide */
public class CustomUtils {

    private static final String PACKAGE_SYSTEMUI = "com.android.systemui";

    public static final String INTENT_RESET_CLONE_USER_ID = "org.nameless.intent.RESET_CLONE_USER_ID";

    private CustomUtils() {}

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

    public static String getDefaultLauncher(Context context) {
        final RoleManager roleManager = context.getSystemService(RoleManager.class);
        final String packageName = CollectionUtils.firstOrNull(
                roleManager.getRoleHolders(RoleManager.ROLE_HOME));
        return packageName != null ? packageName : "";
    }

    public static void forceStopDefaultLauncher(Context context) {
        final ActivityManager activityManager = context.getSystemService(ActivityManager.class);
        try {
            activityManager.forceStopPackageAsUser(getDefaultLauncher(context), UserHandle.USER_CURRENT);
        } catch (Exception ignored) {}
    }

    public static void takeScreenshot(boolean fullscreen) {
        final IWindowManager windowManager = WindowManagerGlobal.getWindowManagerService();
        try {
            windowManager.takeScreenshotExt(fullscreen);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public static void toggleCameraFlash() {
        final IStatusBarService service = IStatusBarService.Stub.asInterface(
                ServiceManager.getService(Context.STATUS_BAR_SERVICE));
        try {
            service.toggleCameraFlash();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public static Drawable getAppIcon(Context context, String pkgName) {
        return getAppIcon(context, pkgName, false);
    }

    public static Drawable getAppIcon(Context context, String pkgName, boolean useDefault) {
        return getAppIcon(context, pkgName, false, -1);
    }

    public static Drawable getAppIcon(Context context, String pkgName, boolean useDefault, int uid) {
        final PackageManager pm = context.getPackageManager();
        Drawable loadIcon = null;
        if (pkgName != null) {
            try {
                loadIcon = pm.getApplicationIcon(pkgName);
            } catch (NameNotFoundException e) {
            }
        }
        if (loadIcon != null && uid != -1) {
            loadIcon = pm.getUserBadgedIcon(loadIcon, UserHandle.getUserHandleForUid(uid));
        }
        if (loadIcon != null) {
            return loadIcon;
        }
        return useDefault ? pm.getDefaultActivityIcon() : null;
    }

    public static String getAppName(Context context, String pkgName) {
        if (pkgName != null) {
            try {
                final PackageManager pm = context.getPackageManager();
                final PackageInfo pi = pm.getPackageInfo(pkgName, 0);
                return pi.applicationInfo.loadLabel(pm).toString();
            } catch (NameNotFoundException e) {
                return null;
            }
        }
        return null;
    }

    public static boolean isSystemApp(Context context, String pkgName) {
        if (pkgName == null) {
            return false;
        }
        try {
            final PackageManager pm = context.getPackageManager();
            final PackageInfo pi = pm.getPackageInfo(pkgName, 0);
            final int flags = pi.applicationInfo.flags;
            return (flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
        } catch (NameNotFoundException e) {
            return false;
        }
    }

    public static void killForegroundApp(Context context) {
        final ActivityManager activityManager = context.getSystemService(ActivityManager.class);
        final ActivityManager.RunningTaskInfo topTaskInfo = getTopFullscreenTask(activityManager);
        if (topTaskInfo == null || topTaskInfo.topActivity == null) {
            return;
        }
        final String packageName = topTaskInfo.topActivity.getPackageName();
        final int taskId = topTaskInfo.taskId;
        if (taskId == INVALID_TASK_ID) {
            return;
        }

        final RoleManager roleManager = context.getSystemService(RoleManager.class);
        final List<String> launchers = roleManager.getRoleHolders(RoleManager.ROLE_HOME);
        if (PACKAGE_SYSTEMUI.equals(packageName) || launchers.contains(packageName)) {
            return;
        }

        final IActivityManager iam = ActivityManagerNative.getDefault();
        boolean killed = false;
        try {
            iam.forceStopPackage(packageName, UserHandle.USER_CURRENT);
            iam.removeTask(taskId);
            killed = true;
        } catch (RemoteException e) {
            // do nothing.
        }
        if (killed) {
            final String appName = getAppName(context, packageName);
            final String msg;
            if (TextUtils.isEmpty(appName)) {
                msg = context.getString(R.string.empty_app_killed);
            } else {
                msg = context.getString(R.string.app_killed, appName);
            }
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
        }
    }

    public static void pinCurrentAppIntoWindow(Context context) {
        context.sendBroadcastAsUser(new Intent(ACTION_PIN_CURRENT_APP), UserHandle.SYSTEM);
    }

    public static void switchToLastApp(Context context) {
        final ActivityManager activityManager = context.getSystemService(ActivityManager.class);
        final ActivityManager.RunningTaskInfo lastTask = getLastTask(context, activityManager);
        if (lastTask != null) {
            activityManager.moveTaskToFront(lastTask.id, ActivityManager.MOVE_TASK_NO_USER_ACTION,
                    ActivityOptions.makeCustomAnimation(context,
                            R.anim.custom_app_in,
                            R.anim.custom_app_out).toBundle());
        }
    }

    public static ActivityManager.RunningTaskInfo getTopFullscreenTask(ActivityManager am) {
        final List<ActivityManager.RunningTaskInfo> infoList = am.getRunningTasks(5);
        if (infoList == null || infoList.size() <= 0) {
            return null;
        }
        for (ActivityManager.RunningTaskInfo info : infoList) {
            if (info.getWindowingMode() == WINDOWING_MODE_FREEFORM ||
                    info.getWindowingMode() == WINDOWING_MODE_MINI_WINDOW_EXT ||
                    info.getWindowingMode() == WINDOWING_MODE_PINNED_WINDOW_EXT) {
                continue;
            }
            return info;
        }
        return null;
    }

    private static ActivityManager.RunningTaskInfo getLastTask(Context context, ActivityManager am) {
        final RoleManager roleManager = context.getSystemService(RoleManager.class);
        final List<String> launchers = roleManager.getRoleHolders(RoleManager.ROLE_HOME);
        final List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(5);
        for (int i = 1; i < tasks.size(); i++) {
            final String packageName = tasks.get(i).topActivity.getPackageName();
            if (!packageName.equals(context.getPackageName())
                    && !packageName.equals(PACKAGE_SYSTEMUI)
                    && !launchers.contains(packageName)) {
                return tasks.get(i);
            }
        }
        return null;
    }

    public static void toggleNotificationPanel() {
        final IStatusBarService service = IStatusBarService.Stub.asInterface(
                ServiceManager.getService(Context.STATUS_BAR_SERVICE));
        try {
            service.togglePanel();
        } catch (RemoteException e) {
            // do nothing.
        }
    }

    public static void toggleQSPanel() {
        final IStatusBarService service = IStatusBarService.Stub.asInterface(
                ServiceManager.getService(Context.STATUS_BAR_SERVICE));
        try {
            service.toggleQSPanel();
        } catch (RemoteException e) {
            // do nothing.
        }
    }

    public static void toggleVolumePanel(Context context) {
        final AudioManager audioManager = context.getSystemService(AudioManager.class);
        audioManager.adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI);
    }

    public static void turnScreenOff(Context context) {
        final PowerManager powerManager = context.getSystemService(PowerManager.class);
        powerManager.goToSleep(SystemClock.uptimeMillis());
    }

    public static void startActivityDismissingKeyguard(Intent intent) {
        if (intent == null) {
            return;
        }
        final IStatusBarService service = IStatusBarService.Stub.asInterface(
                ServiceManager.getService(Context.STATUS_BAR_SERVICE));
        try {
            service.startActivityDismissingKeyguard(intent);
        } catch (RemoteException e) {
            // do nothing.
        }
    }

    public static void restartSystemUi(Context context) {
        new RestartSystemUiTask(context).execute();
    }

    private static class RestartSystemUiTask extends AsyncTask<Void, Void, Void> {

        private final Context mContext;

        public RestartSystemUiTask(Context context) {
            super();
            mContext = context;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                final ActivityManager am = mContext.getSystemService(ActivityManager.class);
                final IActivityManager ams = ActivityManager.getService();
                for (ActivityManager.RunningAppProcessInfo app : am.getRunningAppProcesses()) {
                    if ("com.android.systemui".equals(app.processName)) {
                        ams.killApplicationProcess(app.processName, app.uid);
                        break;
                    }
                }
            } catch (RemoteException e) {
                // do nothing.
            }
            return null;
        }
    }
}
