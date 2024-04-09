/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.os;

import android.os.VibrationAttributes;

/** @hide */
public class CustomVibrationAttributes {

    private CustomVibrationAttributes() {}

    public static final VibrationAttributes VIBRATION_ATTRIBUTES_BACK_GESTURE_DRAG =
            VibrationAttributes.createForUsage(VibrationAttributes.USAGE_CUSTOM_BACK_GESTURE_DRAG);
    public static final VibrationAttributes VIBRATION_ATTRIBUTES_FACE_UNLOCK =
            VibrationAttributes.createForUsage(VibrationAttributes.USAGE_CUSTOM_FACE_UNLOCK);
    public static final VibrationAttributes VIBRATION_ATTRIBUTES_FINGERPRINT_UNLOCK =
            VibrationAttributes.createForUsage(VibrationAttributes.USAGE_CUSTOM_FINGERPRINT_UNLOCK);
    public static final VibrationAttributes VIBRATION_ATTRIBUTES_MISC_SCENES =
            VibrationAttributes.createForUsage(VibrationAttributes.USAGE_CUSTOM_MISC_SCENES);
    public static final VibrationAttributes VIBRATION_ATTRIBUTES_OFF_SCREEN_GESTURE =
            VibrationAttributes.createForUsage(VibrationAttributes.USAGE_CUSTOM_OFF_SCREEN_GESTURE);
    public static final VibrationAttributes VIBRATION_ATTRIBUTES_PREVIEW_ALARM_CALL =
            VibrationAttributes.createForUsage(VibrationAttributes.USAGE_CUSTOM_PREVIEW_ALARM_CALL);
    public static final VibrationAttributes VIBRATION_ATTRIBUTES_PREVIEW_NOTIFICATION =
            VibrationAttributes.createForUsage(VibrationAttributes.USAGE_CUSTOM_PREVIEW_NOTIFICATION);
    public static final VibrationAttributes VIBRATION_ATTRIBUTES_QS_TILE =
            VibrationAttributes.createForUsage(VibrationAttributes.USAGE_CUSTOM_QS_TILE);
    public static final VibrationAttributes VIBRATION_ATTRIBUTES_SLIDER =
            VibrationAttributes.createForUsage(VibrationAttributes.USAGE_CUSTOM_SLIDER);
    public static final VibrationAttributes VIBRATION_ATTRIBUTES_SWITCH =
            VibrationAttributes.createForUsage(VibrationAttributes.USAGE_CUSTOM_SWITCH);
}
