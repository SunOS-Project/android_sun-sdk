/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.internal.util.sun;

import static org.sun.provider.SettingsExt.System.OPTIMIZED_CHARGE_CEILING;
import static org.sun.provider.SettingsExt.System.OPTIMIZED_CHARGE_ENABLED;
import static org.sun.provider.SettingsExt.System.OPTIMIZED_CHARGE_FLOOR;
import static org.sun.provider.SettingsExt.System.OPTIMIZED_CHARGE_STATUS;
import static org.sun.provider.SettingsExt.System.OPTIMIZED_CHARGE_TIME;
import static org.sun.provider.SettingsExt.System.WIRELESS_CHARGING_ENABLED;
import static org.sun.provider.SettingsExt.System.WIRELESS_CHARGING_QUIET_MODE_ENABLED;
import static org.sun.provider.SettingsExt.System.WIRELESS_CHARGING_QUIET_MODE_STATUS;
import static org.sun.provider.SettingsExt.System.WIRELESS_CHARGING_QUIET_MODE_TIME;
import static org.sun.provider.SettingsExt.System.WIRELESS_REVERSE_CHARGING_ENABLED;
import static org.sun.provider.SettingsExt.System.WIRELESS_REVERSE_CHARGING_MIN_LEVEL;
import static org.sun.provider.SettingsExt.System.WIRELESS_REVERSE_CHARGING_SUSPENDED_STATUS;

import static vendor.sun.hardware.battery.Feature.SUSPEND_CHARGING;
import static vendor.sun.hardware.battery.Feature.WIRELESS_CHARGING_QUIET_MODE;
import static vendor.sun.hardware.battery.Feature.WIRELESS_CHARGING_RX;
import static vendor.sun.hardware.battery.Feature.WIRELESS_CHARGING_TX;

import android.content.ContentResolver;
import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;

import org.sun.os.BatteryFeatureManager;

/** @hide */
public class BatteryFeatureSettingsHelper {

    public static final int OPTIMIZED_CHARGE_SETTINGS_DISABLED = 0;
    public static final int OPTIMIZED_CHARGE_SETTINGS_ENABLED = 1;

    public static final int OPTIMIZED_CHARGE_ALWAYS = 0;
    public static final int OPTIMIZED_CHARGE_SCHEDULED = 1;

    public static final int OPTIMIZED_CHARGE_CEILING_DEFAULT = 80;
    public static final int OPTIMIZED_CHARGE_FLOOR_DEFAULT = 75;

    public static final String OPTIMIZED_CHARGE_TIME_DEFAULT = "22:00,07:00";

    public static final int CHARGING_DISABLED = 0;
    public static final int CHARGING_ENABLED = 1;

    public static final int REVERSE_CHARGING_DISABLED = 0;
    public static final int REVERSE_CHARGING_ENABLED = 1;

    public static final int REVERSE_CHARGING_UNSUSPENDED = 0;
    public static final int REVERSE_CHARGING_SUSPENDED_CHARGING = 1;
    public static final int REVERSE_CHARGING_SUSPENDED_LOW_POWER = 2;
    public static final int REVERSE_CHARGING_SUSPENDED_POWER_SAVE = 3;

    public static final int REVERSE_CHARGING_MIN_LEVEL_DEFAULT = 20;

    public static final int QUIET_MODE_DISABLED = 0;
    public static final int QUIET_MODE_ENABLED = 1;

    public static final int QUIET_MODE_ALWAYS = 0;
    public static final int QUIET_MODE_SCHEDULED = 1;

    public static final String QUIET_MODE_TIME_DEFAULT = "22:00,07:00";

    public static boolean getOptimizedChargingEnabled(Context context) {
        if (!BatteryFeatureManager.getInstance().hasFeature(SUSPEND_CHARGING)) {
            return false;
        }
        return Settings.System.getIntForUser(context.getContentResolver(),
                OPTIMIZED_CHARGE_ENABLED,
                OPTIMIZED_CHARGE_SETTINGS_DISABLED, UserHandle.USER_SYSTEM)
                == OPTIMIZED_CHARGE_SETTINGS_ENABLED;
    }

    public static void setOptimizedChargingEnabled(Context context, boolean enabled) {
        Settings.System.putIntForUser(context.getContentResolver(),
                OPTIMIZED_CHARGE_ENABLED, enabled ?
                OPTIMIZED_CHARGE_SETTINGS_ENABLED : OPTIMIZED_CHARGE_SETTINGS_DISABLED,
                UserHandle.USER_SYSTEM);
    }

    public static int getOptimizedChargingStatus(Context context) {
        return Settings.System.getIntForUser(context.getContentResolver(),
                OPTIMIZED_CHARGE_STATUS,
                OPTIMIZED_CHARGE_ALWAYS, UserHandle.USER_SYSTEM);
    }

    public static String optimizedChargingStatusToString(int status) {
        switch (status) {
            case OPTIMIZED_CHARGE_ALWAYS:
                return "always";
            case OPTIMIZED_CHARGE_SCHEDULED:
                return "scheduled";
            default:
                return "unknown";
        }
    }

    public static String getOptimizedChargingTime(Context context) {
        final String time = Settings.System.getStringForUser(context.getContentResolver(),
                OPTIMIZED_CHARGE_TIME, UserHandle.USER_SYSTEM);
        if (TextUtils.isEmpty(time)) {
            return OPTIMIZED_CHARGE_TIME_DEFAULT;
        }
        return time;
    }

    public static void setOptimizedChargingTime(Context context, String time) {
        Settings.System.putStringForUser(context.getContentResolver(),
                OPTIMIZED_CHARGE_TIME,
                time, UserHandle.USER_SYSTEM);
    }

    public static int getOptimizedChargingCeiling(Context context) {
        return Settings.System.getIntForUser(context.getContentResolver(),
                OPTIMIZED_CHARGE_CEILING,
                OPTIMIZED_CHARGE_CEILING_DEFAULT, UserHandle.USER_SYSTEM);
    }

    public static void setOptimizedChargingCeiling(Context context, int level) {
        Settings.System.putIntForUser(context.getContentResolver(),
                OPTIMIZED_CHARGE_CEILING,
                level, UserHandle.USER_SYSTEM);
    }

    public static int getOptimizedChargingFloor(Context context) {
        return Settings.System.getIntForUser(context.getContentResolver(),
                OPTIMIZED_CHARGE_FLOOR,
                OPTIMIZED_CHARGE_FLOOR_DEFAULT, UserHandle.USER_SYSTEM);
    }

    public static void setOptimizedChargingFloor(Context context, int level) {
        Settings.System.putIntForUser(context.getContentResolver(),
                OPTIMIZED_CHARGE_FLOOR,
                level, UserHandle.USER_SYSTEM);
    }

    public static boolean getWirelessChargingEnabled(Context context) {
        if (!BatteryFeatureManager.getInstance().hasFeature(WIRELESS_CHARGING_RX)) {
            return false;
        }
        return Settings.System.getIntForUser(context.getContentResolver(),
                WIRELESS_CHARGING_ENABLED,
                CHARGING_ENABLED, UserHandle.USER_SYSTEM) == CHARGING_ENABLED;
    }

    public static void setWirelessChargingEnabled(Context context, boolean enabled) {
        Settings.System.putIntForUser(context.getContentResolver(),
                WIRELESS_CHARGING_ENABLED,
                enabled ? CHARGING_ENABLED : CHARGING_DISABLED,
                UserHandle.USER_SYSTEM);
    }

    public static boolean getReverseChargingEnabled(Context context) {
        if (!BatteryFeatureManager.getInstance().hasFeature(WIRELESS_CHARGING_TX)) {
            return false;
        }
        return Settings.System.getIntForUser(context.getContentResolver(),
                WIRELESS_REVERSE_CHARGING_ENABLED,
                REVERSE_CHARGING_DISABLED, UserHandle.USER_SYSTEM) == REVERSE_CHARGING_ENABLED;
    }

    public static void setReverseChargingEnabled(Context context, boolean enabled) {
        Settings.System.putIntForUser(context.getContentResolver(),
                WIRELESS_REVERSE_CHARGING_ENABLED,
                enabled ? REVERSE_CHARGING_ENABLED : REVERSE_CHARGING_DISABLED,
                UserHandle.USER_SYSTEM);
    }

    public static int getReverseChargingSuspendedStatus(Context context) {
        return Settings.System.getIntForUser(context.getContentResolver(),
                WIRELESS_REVERSE_CHARGING_SUSPENDED_STATUS,
                REVERSE_CHARGING_UNSUSPENDED, UserHandle.USER_SYSTEM);
    }

    public static String reverseChargingSuspendedStatusToString(int status) {
        switch (status) {
            case REVERSE_CHARGING_UNSUSPENDED:
                return "unsuspended";
            case REVERSE_CHARGING_SUSPENDED_CHARGING:
                return "charging";
            case REVERSE_CHARGING_SUSPENDED_LOW_POWER:
                return "low_power";
            case REVERSE_CHARGING_SUSPENDED_POWER_SAVE:
                return "power_save";
            default:
                return "unknown";
        }
    }

    public static int getReverseChargingMinLevel(Context context) {
        return Settings.System.getIntForUser(context.getContentResolver(),
                WIRELESS_REVERSE_CHARGING_MIN_LEVEL,
                REVERSE_CHARGING_MIN_LEVEL_DEFAULT, UserHandle.USER_SYSTEM);
    }

    public static void setReverseChargingMinLevel(Context context, int level) {
        Settings.System.putIntForUser(context.getContentResolver(),
                WIRELESS_REVERSE_CHARGING_MIN_LEVEL,
                level, UserHandle.USER_SYSTEM);
    }

    public static boolean getQuietModeEnabled(Context context) {
        if (!BatteryFeatureManager.getInstance().hasFeature(WIRELESS_CHARGING_QUIET_MODE)) {
            return false;
        }
        return Settings.System.getIntForUser(context.getContentResolver(),
                WIRELESS_CHARGING_QUIET_MODE_ENABLED,
                QUIET_MODE_DISABLED, UserHandle.USER_SYSTEM) == QUIET_MODE_ENABLED;
    }

    public static void setQuietModeEnabled(Context context, boolean enabled) {
        Settings.System.putIntForUser(context.getContentResolver(),
                WIRELESS_CHARGING_QUIET_MODE_ENABLED,
                enabled ? QUIET_MODE_ENABLED : QUIET_MODE_DISABLED,
                UserHandle.USER_SYSTEM);
    }

    public static int getQuietModeStatus(Context context) {
        return Settings.System.getIntForUser(context.getContentResolver(),
                WIRELESS_CHARGING_QUIET_MODE_STATUS,
                QUIET_MODE_ALWAYS, UserHandle.USER_SYSTEM);
    }

    public static void setQuietModeStatus(Context context, int status) {
        Settings.System.putIntForUser(context.getContentResolver(),
                WIRELESS_CHARGING_QUIET_MODE_STATUS,
                status, UserHandle.USER_SYSTEM);
    }

    public static String quietModeStatusToString(int status) {
        switch (status) {
            case QUIET_MODE_ALWAYS:
                return "always";
            case QUIET_MODE_SCHEDULED:
                return "scheduled";
            default:
                return "unknown";
        }
    }

    public static String getQuietModeTime(Context context) {
        final String time = Settings.System.getStringForUser(context.getContentResolver(),
                WIRELESS_CHARGING_QUIET_MODE_TIME, UserHandle.USER_SYSTEM);
        if (TextUtils.isEmpty(time)) {
            return QUIET_MODE_TIME_DEFAULT;
        }
        return time;
    }

    public static void setQuietModeTime(Context context, String time) {
        Settings.System.putStringForUser(context.getContentResolver(),
                WIRELESS_CHARGING_QUIET_MODE_TIME,
                time, UserHandle.USER_SYSTEM);
    }
}
