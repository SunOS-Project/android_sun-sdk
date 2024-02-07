/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.internal.util.nameless;

import static org.nameless.provider.SettingsExt.System.QQS_LAYOUT_CUSTOM;
import static org.nameless.provider.SettingsExt.System.QS_TILE_LABEL_HIDE;
import static org.nameless.provider.SettingsExt.System.QS_TILE_VERTICAL_LAYOUT;
import static org.nameless.provider.SettingsExt.System.QS_LAYOUT_CUSTOM;

import android.content.Context;
import android.content.om.IOverlayManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings;

public class TileUtils {

    private static final String[] OVERLAY_PKG = {
        "org.nameless.qs.portrait.layout_%sx%s",
        "org.nameless.qqs.portrait.layout_%sx%s",
    };

    public static boolean getQSTileLabelHide(Context context) {
        return Settings.System.getIntForUser(
                context.getContentResolver(),
                QS_TILE_LABEL_HIDE,
                0, UserHandle.USER_CURRENT) != 0;
    }

    public static boolean getQSTileVerticalLayout(Context context) {
        return Settings.System.getIntForUser(
                context.getContentResolver(),
                QS_TILE_VERTICAL_LAYOUT,
                0, UserHandle.USER_CURRENT) != 0;
    }

    public static boolean updateLayout(Context context) {
        final IOverlayManager overlayManager = IOverlayManager.Stub.asInterface(
                ServiceManager.getService(Context.OVERLAY_SERVICE));
        final int layout_qs = Settings.System.getIntForUser(
                context.getContentResolver(),
                QS_LAYOUT_CUSTOM,
                42, UserHandle.USER_SYSTEM);
        final int layout_qqs = Settings.System.getIntForUser(
                context.getContentResolver(),
                QQS_LAYOUT_CUSTOM,
                22, UserHandle.USER_SYSTEM);
        final int row_qs = ensureValidValue(layout_qs / 10, 2, 6);
        final int col_qs = ensureValidValue(layout_qs % 10, 2, 6);
        final int row_qqs = ensureValidValue(layout_qqs / 10, 1, 5);
        for (int i = 0; i < 2; ++i) {
            String pkgName;
            if (i == 0) {
                pkgName = String.format(OVERLAY_PKG[0], Integer.toString(row_qs), Integer.toString(col_qs));
            } else {
                pkgName = String.format(OVERLAY_PKG[1], Integer.toString(row_qqs), Integer.toString(col_qs));
            }
            try {
                overlayManager.setEnabledExclusiveInCategory(pkgName, UserHandle.USER_SYSTEM);
            } catch (RemoteException e) {
                return false;
            }
        }
        return true;
    }

    private static int ensureValidValue(int val, int min, int max) {
        if (val < min) {
            return min;
        }
        if (val > max) {
            return max;
        }
        return val;
    }
}
