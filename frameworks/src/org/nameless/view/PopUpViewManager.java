/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.view;

import android.os.SystemProperties;

import java.util.HashSet;

/** @hide */
public class PopUpViewManager {

    public static final boolean FEATURE_SUPPORTED = SystemProperties.getBoolean(
        "ro.nameless.feature.pop_up_view", false
    );

    /** TODO: Get rid of these dirty blacklist stuff. Maybe move them to local config file. */

    private static final HashSet<String> SETTINGS_CALLER_BLACKLIST = new HashSet<>();
    private static final HashSet<String> SHARE_BLACKLIST = new HashSet<>();
    private static final HashSet<String> SYSTEM_NOTIFICATION_BLACKLIST = new HashSet<>();

    public static final String ACTION_START_MINI_WINDOW = "org.nameless.intent.START_MINI_WINDOW";
    public static final String ACTION_START_PINNED_WINDOW = "org.nameless.intent.START_PINNED_WINDOW";
    public static final String ACTION_PIN_CURRENT_APP = "org.nameless.intent.PIN_CURRENT_APP";

    public static final String EXTRA_PACKAGE_NAME = "packageName";
    public static final String EXTRA_ACTIVITY_NAME = "activityName";

    public static final String MI_FREEFORM_API_INTENT = "com.sunshine.freeform.start_freeform";
    public static final String MI_FREEFORM_PACKAGE_NAME = "com.sunshine.freeform";

    public static final int TAP_ACTION_PIN_WINDOW = 0;
    public static final int TAP_ACTION_EXIT = 1;
    public static final int TAP_ACTION_NOTHING = 2;

    private PopUpViewManager() {}

    static {
        SETTINGS_CALLER_BLACKLIST.add("com.android.launcher3");
        SETTINGS_CALLER_BLACKLIST.add("com.android.permissioncontroller");

        SHARE_BLACKLIST.add("com.google.android.gms");
        SHARE_BLACKLIST.add("com.whatsapp");

        SYSTEM_NOTIFICATION_BLACKLIST.add("android");
        SYSTEM_NOTIFICATION_BLACKLIST.add("com.android.chrome");
        SYSTEM_NOTIFICATION_BLACKLIST.add("com.android.packageinstaller");
        SYSTEM_NOTIFICATION_BLACKLIST.add("com.google.android.gms");
        SYSTEM_NOTIFICATION_BLACKLIST.add("com.google.android.packageinstaller");
    }

    public static boolean inSettingsCallerBlacklist(String packageName) {
        return SETTINGS_CALLER_BLACKLIST.contains(packageName) ||
                (packageName != null && packageName.startsWith("com.google.android"));
    }

    public static boolean inShareBlacklist(String packageName) {
        return SHARE_BLACKLIST.contains(packageName);
    }

    public static boolean inSystemNotificationBlacklist(String packageName) {
        return SYSTEM_NOTIFICATION_BLACKLIST.contains(packageName);
    }
}
