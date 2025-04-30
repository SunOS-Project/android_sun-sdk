/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.settings.accessibility;

import android.content.Context;

import com.android.settings.core.BasePreferenceController;

import java.util.ArrayList;

import org.sun.os.VibratorExtManager;

public class VibrationStrengthCategoryPreferenceController extends BasePreferenceController {

    private final VibratorExtManager mVibratorExtManager;

    public VibrationStrengthCategoryPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mVibratorExtManager = VibratorExtManager.getInstance();
    }

    @Override
    public int getAvailabilityStatus() {
        final ArrayList<Integer> validTypes = mVibratorExtManager.getValidVibrationTypes();
        return validTypes.size() > 0 ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }
}
