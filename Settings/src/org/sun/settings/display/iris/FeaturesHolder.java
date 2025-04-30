/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.settings.display.iris;

import static vendor.sun.hardware.displayfeature.Feature.MEMC_FHD;
import static vendor.sun.hardware.displayfeature.Feature.MEMC_QHD;
import static vendor.sun.hardware.displayfeature.Feature.SDR2HDR;

import org.sun.display.DisplayFeatureManager;

public class FeaturesHolder {

    private FeaturesHolder() {}

    public static final boolean MEMC_FHD_SUPPORTED =
            DisplayFeatureManager.getInstance().hasFeature(MEMC_FHD);
    public static final boolean MEMC_QHD_SUPPORTED =
            DisplayFeatureManager.getInstance().hasFeature(MEMC_QHD);
    public static final boolean SDR2HDR_SUPPORTED =
            DisplayFeatureManager.getInstance().hasFeature(SDR2HDR);
}
