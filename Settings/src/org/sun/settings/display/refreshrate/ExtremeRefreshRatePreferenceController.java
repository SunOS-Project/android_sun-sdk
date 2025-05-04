/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.settings.display.refreshrate;

import static org.sun.provider.SettingsExt.System.EXTREME_REFRESH_RATE;
import static org.sun.settings.display.iris.FeaturesHolder.MEMC_FHD_SUPPORTED;
import static org.sun.settings.display.iris.FeaturesHolder.MEMC_QHD_SUPPORTED;
import static org.sun.settings.display.iris.FeaturesHolder.SDR2HDR_SUPPORTED;

import android.content.Context;
import android.content.DialogInterface;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

import org.sun.display.RefreshRateManager;

public class ExtremeRefreshRatePreferenceController extends TogglePreferenceController {

    private final RefreshRateManager mRefreshRateManager;

    public ExtremeRefreshRatePreferenceController(Context context, String key) {
        super(context, key);
        mRefreshRateManager = context.getSystemService(RefreshRateManager.class);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean isChecked() {
        return Settings.System.getIntForUser(mContext.getContentResolver(),
                EXTREME_REFRESH_RATE, 0, UserHandle.USER_CURRENT) == 1;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (!isChecked) {
            mRefreshRateManager.setExtremeRefreshRateEnabled(false);
            return true;
        }

        final boolean irisSupported = supportIrisFeature();

        new AlertDialog.Builder(mContext)
                .setTitle(mContext.getText(R.string.confirm_before_enable_title))
                .setMessage(mContext.getText(irisSupported ?
                        R.string.extreme_refresh_rate_warning_iris :
                        R.string.extreme_refresh_rate_warning))
                .setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mRefreshRateManager.setExtremeRefreshRateEnabled(true);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
        return isChecked();
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return 0;
    }

    private boolean supportIrisFeature() {
        return MEMC_FHD_SUPPORTED || MEMC_QHD_SUPPORTED || SDR2HDR_SUPPORTED;
    }
}
