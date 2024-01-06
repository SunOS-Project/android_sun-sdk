/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.settings.accessibility;

import static org.nameless.os.CustomVibrationAttributes.VIBRATION_ATTRIBUTES_PREVIEW_ALARM_CALL;
import static org.nameless.os.VibrationPatternManager.Type.RINGTONE;
import static org.nameless.provider.SettingsExt.System.VIBRATION_PATTERN_RINGTONE;

import android.content.Context;
import android.os.VibrationAttributes;

public class RingtoneVibrationPatternPreferenceController extends VibrationPatternPreferenceController {

    public RingtoneVibrationPatternPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public String getSettings() {
        return VIBRATION_PATTERN_RINGTONE;
    }

    @Override
    public int getType() {
        return RINGTONE;
    }

    @Override
    public VibrationAttributes getAttribute() {
        return VIBRATION_ATTRIBUTES_PREVIEW_ALARM_CALL;
    }
}
