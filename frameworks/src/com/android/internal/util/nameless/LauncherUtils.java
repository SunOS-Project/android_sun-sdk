/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.internal.util.nameless;

import static org.nameless.os.DebugConstants.DEBUG_LAUNCHER;

import android.content.Context;
import android.os.SystemProperties;
import android.util.Log;

import com.android.internal.R;

import java.util.ArrayList;

public class LauncherUtils {

    private static final String TAG = "LauncherUtils";

    private static final String DEFAULT_COMPONENT_NAME = "com.google.android.apps.nexuslauncher/com.android.quickstep.RecentsActivity";

    private static final String PROP_SELECTED_LAUNCHER = "persist.sys.nameless.launcher";
    private static final String PROP_SELECTED_LAUNCHER_CACHED = "sys.nameless.launcher_cached";

    private static ArrayList<String> sPackageList;
    private static ArrayList<String> sComponentList;

    private static String sResComponentName;

    private static String getComponentFromLauncher(Context context, String launcher) {
        if (sPackageList == null) {
            initLauncherList(context);
        }
        final int index = sPackageList.indexOf(launcher);
        if (index != -1) {
            return sComponentList.get(index);
        } else {
            return getResComponentName(context);
        }
    }

    private static String getLauncherFromComponent(String componentName) {
        final String[] splited = componentName.split("/");
        if (splited.length != 2) {
            return "";
        }
        return splited[0];
    }

    private static String getSelectedComponentName(Context context) {
        final String launcher = getSelectedLauncher();
        final String resComponentName = getResComponentName(context);
        if (resComponentName.equals(DEFAULT_COMPONENT_NAME) && !launcher.isEmpty()) {
            return getComponentFromLauncher(context, launcher);
        } else {
            return resComponentName;
        }
    }

    private static void initLauncherList(Context context) {
        sPackageList = new ArrayList<>();
        sComponentList = new ArrayList<>();

        final String[] components = context.getResources().getStringArray(
                R.array.config_launcherSwitcherComponents);
        if (components == null || components.length == 0) {
            Log.e(TAG, "Supported component list is empty");
            return;
        }

        for (final String component : components) {
            final String launcher = getLauncherFromComponent(component);
            if (launcher.isEmpty()) {
                Log.w(TAG, "Ignore invalid format " + component);
                continue;
            }
            sPackageList.add(launcher);
            sComponentList.add(component);
            if (DEBUG_LAUNCHER) {
                Log.d(TAG, "init, add component: " + component);
            }
        }
    }

    public static ArrayList<String> getLauncherList(Context context) {
        if (sPackageList == null) {
            initLauncherList(context);
        }
        return sPackageList;
    }

    public static String getComponentName(Context context) {
        final String cachedLauncher = getCachedLauncher();
        String componentName;
        if (!cachedLauncher.isEmpty()) {
            componentName = getComponentFromLauncher(context, cachedLauncher);
        } else {
            componentName = getSelectedComponentName(context);
            SystemProperties.set(PROP_SELECTED_LAUNCHER_CACHED, getLauncherFromComponent(componentName));
        }
        if (DEBUG_LAUNCHER) {
            Log.d(TAG, "getComponentName: " + componentName);
        }
        return componentName;
    }

    public static String getResComponentName(Context context) {
        if (sResComponentName == null) {
            sResComponentName = context.getString(R.string.config_recentsComponentName);
        }
        return sResComponentName;
    }

    public static String getCachedLauncher() {
        return SystemProperties.get(PROP_SELECTED_LAUNCHER_CACHED);
    }

    public static String getSelectedLauncher() {
        return SystemProperties.get(PROP_SELECTED_LAUNCHER);
    }

    public static void setSelectedLauncher(String launcher) {
        SystemProperties.set(PROP_SELECTED_LAUNCHER, launcher);
    }
}
