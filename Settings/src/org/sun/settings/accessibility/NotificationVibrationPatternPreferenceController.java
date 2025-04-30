/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.settings.accessibility;

import static org.sun.os.CustomVibrationAttributes.VIBRATION_ATTRIBUTES_PREVIEW_NOTIFICATION;
import static org.sun.os.VibrationPatternManager.Type.NOTIFICATION;
import static org.sun.provider.SettingsExt.System.VIBRATION_PATTERN_NOTIFICATION;

import android.content.Context;
import android.os.VibrationAttributes;

public class NotificationVibrationPatternPreferenceController extends VibrationPatternPreferenceController {

    public NotificationVibrationPatternPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public String getSettings() {
        return VIBRATION_PATTERN_NOTIFICATION;
    }

    @Override
    public int getType() {
        return NOTIFICATION;
    }

    @Override
    public VibrationAttributes getAttribute() {
        return VIBRATION_ATTRIBUTES_PREVIEW_NOTIFICATION;
    }
}
