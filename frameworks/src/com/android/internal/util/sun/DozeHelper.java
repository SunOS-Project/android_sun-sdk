/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.internal.util.sun;

import static org.sun.provider.SettingsExt.System.DOZE_PICK_UP_ACTION;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;

import com.android.internal.R;

/** @hide */
public class DozeHelper {

    public static final int DOZE_ACTION_NONE = 0;
    public static final int DOZE_ACTION_SHOW_AMBINET = 1;
    public static final int DOZE_ACTION_SHOW_LOCKSCREEN = 2;

    public static boolean isDozeActionValid(int action) {
        return action == DOZE_ACTION_NONE ||
                action == DOZE_ACTION_SHOW_AMBINET ||
                action == DOZE_ACTION_SHOW_LOCKSCREEN;
    }

    public static boolean isPickUpSupported(Context context) {
        return (useNativePickUpSensor(context) ||
                !TextUtils.isEmpty(getPickUpSensorType(context))) &&
                getPickUpSensorValue(context) >= 0.0f;
    }

    public static boolean useNativePickUpSensor(Context context) {
        return context.getResources().getBoolean(R.bool.config_doze_useNativePickUpSensor);
    }

    public static int getPickUpAction(Context context) {
        return getPickUpAction(context, UserHandle.USER_CURRENT);
    }

    public static int getPickUpAction(Context context, int userId) {
        return Settings.System.getIntForUser(context.getContentResolver(),
                DOZE_PICK_UP_ACTION, DOZE_ACTION_NONE, userId);
    }

    public static String getPickUpSensorType(Context context) {
        return context.getResources().getString(R.string.config_doze_pickUpSensorType);
    }

    public static float getPickUpSensorValue(Context context) {
        return context.getResources().getFloat(R.dimen.config_doze_pickUpSensorValue);
    }

    public static String dozeActionToString(int action) {
        switch (action) {
            case DOZE_ACTION_NONE:
                return "none";
            case DOZE_ACTION_SHOW_AMBINET:
                return "show_ambient";
            case DOZE_ACTION_SHOW_LOCKSCREEN:
                return "show_lockscreen";
            default:
                return "unknown";
        }
    }
}
