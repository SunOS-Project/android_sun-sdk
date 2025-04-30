/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.audio;

import static android.media.AudioManager.RINGER_MODE_NORMAL;
import static android.media.AudioManager.RINGER_MODE_SILENT;
import static android.media.AudioManager.RINGER_MODE_VIBRATE;

import static org.sun.provider.SettingsExt.Global.ALERT_SLIDER_STATE;

import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.KeyEvent;

import com.android.internal.util.sun.FileUtils;

/** @hide */
public class AlertSliderManager {

    private static final String TAG = "AlertSliderManager";

    private static final String OPLUS_ALERT_SLIDER_STATE_NODE = "/proc/tristatekey/tri_state";

    private static final int OPLUS_ALERT_SLIDER_KEY = 133;

    public static final int STATE_UNKNOWN = -1;
    public static final int STATE_TOP = 0;
    public static final int STATE_MIDDLE = 1;
    public static final int STATE_BOTTOM = 2;

    public static int getFileState() {
        final String state = FileUtils.readOneLine(OPLUS_ALERT_SLIDER_STATE_NODE);
        if (TextUtils.isEmpty(state)) {
            return STATE_UNKNOWN;
        }
        switch (state.trim()) {
            case "1":
                return STATE_TOP;
            case "2":
                return STATE_MIDDLE;
            case "3":
                return STATE_BOTTOM;
            default:
                return STATE_UNKNOWN;
        }
    }

    public static int getSettingsState(Context context) {
        return Settings.Global.getInt(context.getContentResolver(),
                ALERT_SLIDER_STATE, STATE_UNKNOWN);
    }

    public static boolean hasAlertSlider(Context context) {
        return context.getResources().getBoolean(
                com.android.internal.R.bool.config_hasAlertSlider);
    }

    public static boolean maybeNotifyUpdate(Context context, int keyCode, boolean down) {
        if (keyCode != OPLUS_ALERT_SLIDER_KEY) {
            return false;
        }
        if (!down) {
            return false;
        }
        final int state = getFileState();
        if (state == STATE_UNKNOWN) {
            return false;
        }
        Settings.Global.putInt(context.getContentResolver(),
                ALERT_SLIDER_STATE, state);
        return true;
    }
}
