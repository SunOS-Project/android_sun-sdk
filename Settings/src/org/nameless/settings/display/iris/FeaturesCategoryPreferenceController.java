/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.settings.display.iris;

import static org.nameless.settings.display.iris.FeaturesHolder.MEMC_FHD_SUPPORTED;
import static org.nameless.settings.display.iris.FeaturesHolder.MEMC_QHD_SUPPORTED;
import static org.nameless.settings.display.iris.FeaturesHolder.SDR2HDR_SUPPORTED;

import android.app.ActivityManager;
import android.content.Context;

import com.android.internal.util.nameless.CustomUtils;

import com.android.settings.core.BasePreferenceController;

public class FeaturesCategoryPreferenceController extends BasePreferenceController {

    private static final String IRIS_PACKAGE_NAME = "org.nameless.iris";
    private static final String IRIS_SERVICE_CLASS_NAME = "org.nameless.iris.service.IrisService";

    private final ActivityManager mActivityManager;
    private final Context mContext;

    public FeaturesCategoryPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mContext = context;
        mActivityManager = context.getSystemService(ActivityManager.class);
    }

    @Override
    public int getAvailabilityStatus() {
        return CustomUtils.isPackageInstalled(mContext, IRIS_PACKAGE_NAME)
                && isServiceRunning()
                && supportAnyFeature() ?
                AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    private boolean isServiceRunning() {
        for (ActivityManager.RunningServiceInfo info :
                mActivityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (IRIS_SERVICE_CLASS_NAME.equals(info.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private boolean supportAnyFeature() {
        return MEMC_FHD_SUPPORTED || MEMC_QHD_SUPPORTED || SDR2HDR_SUPPORTED;
    }
}
