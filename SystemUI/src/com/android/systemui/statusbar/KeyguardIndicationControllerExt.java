/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.statusbar;

import static org.nameless.provider.SettingsExt.System.LOCKSCREEN_BATTERY_INFO;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.UserHandle;

import com.android.systemui.settings.UserTracker;
import com.android.systemui.util.settings.SystemSettings;

import java.util.concurrent.Executor;

import org.nameless.provider.SettingsExt;

class KeyguardIndicationControllerExt {

    private static class InstanceHolder {
        private static KeyguardIndicationControllerExt INSTANCE = new KeyguardIndicationControllerExt();
    }

    static KeyguardIndicationControllerExt getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private ContentObserver mSettingsObserver;
    private SystemSettings mSystemSettings;
    private UserTracker mUserTracker;
    private UserTracker.Callback mUserTrackerCallback;

    private boolean mShowChargingInfoUser;
    private boolean mShowChargingInfoSystem;
    private boolean mShowChargeRemainingTimeSystem;

    private int mChargingCurrent;
    private double mChargingVoltage;
    private float mTemperature;

    void init(Context context, Executor executor, Handler handler,
            SystemSettings systemSettings, UserTracker userTracker) {
        mSystemSettings = systemSettings;
        mUserTracker = userTracker;

        mShowChargingInfoSystem = context.getResources().getBoolean(
                com.android.internal.R.bool.config_enable_lockscreen_charging_info);
        mShowChargeRemainingTimeSystem = context.getResources().getBoolean(
                com.android.internal.R.bool.config_show_charging_remaining_time);

        mSettingsObserver = new ContentObserver(handler) {
            @Override
            public void onChange(boolean selfChange) {
                mShowChargingInfoUser = mSystemSettings.getIntForUser(LOCKSCREEN_BATTERY_INFO,
                        0, mUserTracker.getUserId()) == 1;
            }
        };
        mSystemSettings.registerContentObserverForUser(LOCKSCREEN_BATTERY_INFO,
                mSettingsObserver, UserHandle.USER_ALL);
        mSettingsObserver.onChange(false);

        mUserTrackerCallback = new UserTracker.Callback() {
            @Override
            public void onUserChanged(int newUser, Context userContext) {
                mSettingsObserver.onChange(false);
            }
        };
        mUserTracker.addCallback(mUserTrackerCallback, executor);
    }

    void destroy() {
        mUserTracker.removeCallback(mUserTrackerCallback);
        mSystemSettings.unregisterContentObserver(mSettingsObserver);
    }

    void onBatteryInfoUpdated(int chargingCurrent, double chargingVoltage, float temperature) {
        mChargingCurrent = chargingCurrent;
        mChargingVoltage = chargingVoltage;
        mTemperature = temperature;
    }

    String getBatteryInfo() {
        if (!mShowChargingInfoSystem || !mShowChargingInfoUser) {
            return "";
        }
        String batteryInfo = "";
        int current = 0;
        double voltage = 0;
        if (mChargingCurrent > 0) {
            current = (mChargingCurrent < 10 ? (mChargingCurrent * 1000)
                    : (mChargingCurrent < 4000 ? mChargingCurrent : (mChargingCurrent / 1000)));
            batteryInfo = batteryInfo + current + "mA";
        }
        if (mChargingVoltage > 0 && mChargingCurrent > 0) {
            voltage = (mChargingVoltage / 1000 / 1000);
            batteryInfo = (batteryInfo == "" ? "" : batteryInfo + " · ") +
                    String.format("%.1f" , ((double) current / 1000) * voltage) + "W";
        }
        if (mChargingVoltage > 0) {
            batteryInfo = (batteryInfo == "" ? "" : batteryInfo + " · ") +
                    String.format("%.1f" , voltage) + "V";
        }
        if (mTemperature > 0) {
            batteryInfo = (batteryInfo == "" ? "" : batteryInfo + " · ") +
                    mTemperature / 10 + "°C";
        }
        if (batteryInfo != "") {
            batteryInfo = "\n" + batteryInfo;
        }
        return batteryInfo;
    }

    boolean isChargingRemainingTimeDisabled() {
        return !mShowChargeRemainingTimeSystem;
    }
}
