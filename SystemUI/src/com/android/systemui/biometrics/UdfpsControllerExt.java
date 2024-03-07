/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.biometrics;

import android.provider.Settings;

import com.android.systemui.res.R;
import com.android.systemui.util.concurrency.DelayableExecutor;

class UdfpsControllerExt {

    private static class InstanceHolder {
        private static UdfpsControllerExt INSTANCE = new UdfpsControllerExt();
    }

    static UdfpsControllerExt getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private UdfpsController mUdfpsController;

    private boolean mUseFrameworkDimming;
    private int[][] mBrightnessAlphaArray;
    private int mDimBrightnessMin;
    private int mDimBrightnessMax;
    private int mDimDelay;
    private int mUdfpsVendorCode;

    void init(UdfpsController controller) {
        mUdfpsController = controller;

        mUseFrameworkDimming = mUdfpsController.getContext().getResources().getBoolean(
                R.bool.config_udfpsFrameworkDimming);

        if (mUseFrameworkDimming) {
            mDimBrightnessMin = mUdfpsController.getContext().getResources().getInteger(
                    R.integer.config_udfpsDimmingBrightnessMin);
            mDimBrightnessMax = mUdfpsController.getContext().getResources().getInteger(
                    R.integer.config_udfpsDimmingBrightnessMax);
            mDimDelay = mUdfpsController.getContext().getResources().getInteger(
                    R.integer.config_udfpsDimmingDisableDelay);

            String[] array = mUdfpsController.getContext().getResources().getStringArray(
                    R.array.config_udfpsDimmingBrightnessAlphaArray);
            mBrightnessAlphaArray = new int[array.length][2];
            for (int i = 0; i < array.length; i++) {
                String[] s = array[i].split(",");
                mBrightnessAlphaArray[i][0] = Integer.parseInt(s[0]);
                mBrightnessAlphaArray[i][1] = Integer.parseInt(s[1]);
            }
        }

        mUdfpsVendorCode = mUdfpsController.getContext().getResources().getInteger(
                R.integer.config_udfps_vendor_code);
    }

    void onFingerUp(DelayableExecutor executor, long requestId) {
        // Add a delay to ensure that the dim amount is updated after the display has had chance
        // to switch out of HBM mode. The delay, in ms is stored in config_udfpsDimmingDisableDelay.
        // If the delay is 0, the dim amount will be updated immediately.
        final int delay = getDimDelay();
        if (delay > 0) {
            executor.executeDelayed(() -> {
                // A race condition exists where the overlay is destroyed before the dim amount
                // is updated. This check ensures that the overlay is still valid.
                if (mUdfpsController.mOverlay != null &&
                        mUdfpsController.mOverlay.matchesRequestId(requestId)) {
                    updateViewDimAmount();
                }
            }, delay);
        } else {
            updateViewDimAmount();
        }
    }

    void updateViewDimAmount() {
        if (mUdfpsController.mOverlay == null || !mUseFrameworkDimming) {
            return;
        }
        if (!mUdfpsController.isFingerDown()) {
            mUdfpsController.mOverlay.setDimAmount(0.0f);
            return;
        }
        final int curBrightness = getBrightness();
        int i, dimAmount;
        for (i = 0; i < mBrightnessAlphaArray.length; i++) {
            if (mBrightnessAlphaArray[i][0] >= curBrightness) break;
        }
        if (i == 0) {
            dimAmount = mBrightnessAlphaArray[i][1];
        } else if (i == mBrightnessAlphaArray.length) {
            dimAmount = mBrightnessAlphaArray[i-1][1];
        } else {
            dimAmount = interpolate(curBrightness,
                    mBrightnessAlphaArray[i][0], mBrightnessAlphaArray[i-1][0],
                    mBrightnessAlphaArray[i][1], mBrightnessAlphaArray[i-1][1]);
        }
        mUdfpsController.mOverlay.setDimAmount(dimAmount / 255.0f);
    }

    int getDimDelay() {
        return mDimDelay;
    }

    int getVendorCode() {
        return mUdfpsVendorCode;
    }

    private int getBrightness() {
        int brightness = Settings.System.getInt(
                mUdfpsController.getContext().getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS, 100);
        if (mDimBrightnessMax > 0) {
            brightness = interpolate(brightness, 0, 255, mDimBrightnessMin, mDimBrightnessMax);
        }
        return brightness;
    }

    private static int interpolate(int x, int xa, int xb, int ya, int yb) {
        return ya - (ya - yb) * (x - xa) / (xb - xa);
    }
}
