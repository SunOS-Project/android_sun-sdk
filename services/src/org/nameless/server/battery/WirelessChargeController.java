/*
 * Copyright (C) 2022-2023 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.server.battery;

import static com.android.internal.util.nameless.BatteryFeatureSettingsHelper.REVERSE_CHARGING_MIN_LEVEL_DEFAULT;
import static com.android.internal.util.nameless.BatteryFeatureSettingsHelper.REVERSE_CHARGING_SUSPENDED_CHARGING;
import static com.android.internal.util.nameless.BatteryFeatureSettingsHelper.REVERSE_CHARGING_SUSPENDED_LOW_POWER;
import static com.android.internal.util.nameless.BatteryFeatureSettingsHelper.REVERSE_CHARGING_SUSPENDED_POWER_SAVE;
import static com.android.internal.util.nameless.BatteryFeatureSettingsHelper.REVERSE_CHARGING_UNSUSPENDED;
import static com.android.internal.util.nameless.BatteryFeatureSettingsHelper.reverseChargingSuspendedStatusToString;

import static org.nameless.provider.SettingsExt.System.WIRELESS_CHARGING_ENABLED;
import static org.nameless.provider.SettingsExt.System.WIRELESS_REVERSE_CHARGING_ENABLED;
import static org.nameless.provider.SettingsExt.System.WIRELESS_REVERSE_CHARGING_MIN_LEVEL;
import static org.nameless.provider.SettingsExt.System.WIRELESS_REVERSE_CHARGING_SUSPENDED_STATUS;

import static org.nameless.server.battery.BatteryFeatureController.logD;

import static vendor.nameless.hardware.battery.Feature.WIRELESS_CHARGING_RX;
import static vendor.nameless.hardware.battery.Feature.WIRELESS_CHARGING_TX;

import android.content.ContentResolver;
import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;

import com.android.internal.util.nameless.BatteryFeatureSettingsHelper;

import org.nameless.os.BatteryFeatureManager;
import org.nameless.server.NamelessSystemExService;

class WirelessChargeController {

    private static final String TAG = "WirelessChargeController";

    private final BatteryFeatureManager mBatteryFeatureManager;
    private final ContentResolver mResolver;
    private final Context mContext;
    private final NamelessSystemExService mService;
    private final NotificationPoster mPoster;

    private boolean mWirelessChargingEnabled = false;

    private boolean mReverseChargingEnabled = false;
    private int mReverseChargingSuspendedStatus = REVERSE_CHARGING_UNSUSPENDED;
    private int mReverseChargingMinLevel = REVERSE_CHARGING_MIN_LEVEL_DEFAULT;

    WirelessChargeController(NamelessSystemExService service,
            BatteryFeatureManager batteryFeatureManager, NotificationPoster poster) {
        mService = service;
        mBatteryFeatureManager = batteryFeatureManager;
        mPoster = poster;
        mResolver = service.getContentResolver();
        mContext = service.getContext();
    }

    void onBatteryStateChanged() {
        logD(TAG, "onBatteryStateChanged");
        updateReverseChargingState();
    }

    void onPowerSaveChanged() {
        logD(TAG, "onPowerSaveChanged");
        updateReverseChargingState();
    }

    void onBootCompleted() {
        logD(TAG, "onBootCompleted");
        updateWirelessChargingSettings();
        if (!updateReverseChargingState()) {
            updateReverseChargingSettings();
        }
    }

    void updateWirelessChargingSettings() {
        final boolean enabled = BatteryFeatureSettingsHelper.getWirelessChargingEnabled(mContext);
        if (enabled == mWirelessChargingEnabled) {
            return;
        }
        mWirelessChargingEnabled = enabled;
        logD(TAG, "updateWirelessChargingSettings, mWirelessChargingEnabled: " + enabled);
        setWirelessCharging(mWirelessChargingEnabled);
    }

    void updateReverseChargingSettings() {
        mReverseChargingEnabled = BatteryFeatureSettingsHelper.getReverseChargingEnabled(mContext);
        mReverseChargingMinLevel = BatteryFeatureSettingsHelper.getReverseChargingMinLevel(mContext);
        logD(TAG, "updateReverseChargingSettings, mReverseChargingEnabled: " +
                mReverseChargingEnabled + ", mReverseChargingMinLevel: " + mReverseChargingMinLevel);
        if (mService.getBatteryLevel() < mReverseChargingMinLevel
                && mReverseChargingSuspendedStatus == REVERSE_CHARGING_UNSUSPENDED) {
            updateReverseChargingSuspendedSettings(REVERSE_CHARGING_SUSPENDED_LOW_POWER);
        } else if (mService.getBatteryLevel() >= mReverseChargingMinLevel
                && mReverseChargingSuspendedStatus != REVERSE_CHARGING_UNSUSPENDED
                && !mService.isDevicePlugged() && !mService.isPowerSaveMode()) {
            updateReverseChargingSuspendedSettings(REVERSE_CHARGING_UNSUSPENDED);
        }
        setReverseCharging(mReverseChargingEnabled
                && mReverseChargingSuspendedStatus == REVERSE_CHARGING_UNSUSPENDED);
    }

    private void updateReverseChargingSuspendedSettings(int status) {
        mReverseChargingSuspendedStatus = status;
        Settings.System.putIntForUser(mResolver,
                WIRELESS_REVERSE_CHARGING_SUSPENDED_STATUS,
                mReverseChargingSuspendedStatus, UserHandle.USER_SYSTEM);
        logD(TAG, "mReverseChargingSuspendedStatus changed to " +
                reverseChargingSuspendedStatusToString(mReverseChargingSuspendedStatus));
    }

    private boolean updateReverseChargingState() {
        int suspendedStatus = REVERSE_CHARGING_UNSUSPENDED;
        if (mService.getBatteryLevel() < mReverseChargingMinLevel) {
            suspendedStatus = REVERSE_CHARGING_SUSPENDED_LOW_POWER;
        }
        if (mService.isPowerSaveMode()) {
            suspendedStatus = REVERSE_CHARGING_SUSPENDED_POWER_SAVE;
        }
        if (mService.isDevicePlugged()) {
            suspendedStatus = REVERSE_CHARGING_SUSPENDED_CHARGING;
        }
        if (suspendedStatus != mReverseChargingSuspendedStatus) {
            updateReverseChargingSuspendedSettings(suspendedStatus);
            updateReverseChargingSettings();
            return true;
        }
        return false;
    }

    private void setWirelessCharging(boolean enabled) {
        logD(TAG, "setWirelessCharging, enabled: " + enabled);
        mBatteryFeatureManager.setFeatureEnabled(WIRELESS_CHARGING_RX, enabled);
    }

    private void setReverseCharging(boolean enabled) {
        logD(TAG, "setReverseCharging, enabled: " + enabled);
        mBatteryFeatureManager.setFeatureEnabled(WIRELESS_CHARGING_TX, enabled);
        if (enabled) {
            mPoster.postWirelessTxNotif();
        } else {
            mPoster.cancelWirelessTxNotif();
        }
    }
}
