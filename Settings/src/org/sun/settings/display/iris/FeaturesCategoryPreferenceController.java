/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.settings.display.iris;

import static org.sun.settings.display.iris.FeaturesHolder.MEMC_FHD_SUPPORTED;
import static org.sun.settings.display.iris.FeaturesHolder.MEMC_QHD_SUPPORTED;
import static org.sun.settings.display.iris.FeaturesHolder.SDR2HDR_SUPPORTED;

import android.content.Context;

import com.android.settings.core.BasePreferenceController;

public class FeaturesCategoryPreferenceController extends BasePreferenceController {

    public FeaturesCategoryPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return supportAnyFeature() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    private boolean supportAnyFeature() {
        return MEMC_FHD_SUPPORTED || MEMC_QHD_SUPPORTED || SDR2HDR_SUPPORTED;
    }
}
