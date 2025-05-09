/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.server.battery;

import static android.os.Process.THREAD_PRIORITY_DEFAULT;

import static org.sun.os.DebugConstants.DEBUG_BATTERY_FEATURE;
import static org.sun.provider.SettingsExt.System.OPTIMIZED_CHARGE_CEILING;
import static org.sun.provider.SettingsExt.System.OPTIMIZED_CHARGE_ENABLED;
import static org.sun.provider.SettingsExt.System.OPTIMIZED_CHARGE_FLOOR;
import static org.sun.provider.SettingsExt.System.OPTIMIZED_CHARGE_STATUS;
import static org.sun.provider.SettingsExt.System.OPTIMIZED_CHARGE_TIME;
import static org.sun.provider.SettingsExt.System.WIRELESS_CHARGING_ENABLED;
import static org.sun.provider.SettingsExt.System.WIRELESS_CHARGING_QUIET_MODE_ENABLED;
import static org.sun.provider.SettingsExt.System.WIRELESS_CHARGING_QUIET_MODE_STATUS;
import static org.sun.provider.SettingsExt.System.WIRELESS_CHARGING_QUIET_MODE_TIME;
import static org.sun.provider.SettingsExt.System.WIRELESS_REVERSE_CHARGING_ENABLED;
import static org.sun.provider.SettingsExt.System.WIRELESS_REVERSE_CHARGING_MIN_LEVEL;

import static vendor.sun.hardware.battery.Feature.SUSPEND_CHARGING;
import static vendor.sun.hardware.battery.Feature.WIRELESS_CHARGING_QUIET_MODE;
import static vendor.sun.hardware.battery.Feature.WIRELESS_CHARGING_RX;
import static vendor.sun.hardware.battery.Feature.WIRELESS_CHARGING_TX;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Slog;

import com.android.internal.R;

import com.android.server.ServiceThread;

import org.sun.os.BatteryFeatureManager;
import org.sun.server.SunSystemExService;

public class BatteryFeatureController {

    private static final String TAG = "BatteryFeatureController";

    private final Handler mHandler;
    private final ServiceThread mServiceThread;

    private BatteryFeatureManager mBatteryFeatureManager;
    private SunSystemExService mSystemExService;

    private NotificationPoster mNotificationPoster;

    private OptimizedChargeController mOptimizedChargeController;
    private QuietModeController mQuietModeController;
    private WirelessChargeController mWirelessChargeController;

    private SettingsObserver mSettingsObserver;

    private static class InstanceHolder {
        private static BatteryFeatureController INSTANCE = new BatteryFeatureController();
    }

    public static BatteryFeatureController getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private final class SettingsObserver extends ContentObserver {

        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            if (mOptimizedChargeController != null) {
                mSystemExService.getContentResolver().registerContentObserver(
                        Settings.System.getUriFor(OPTIMIZED_CHARGE_CEILING),
                        false, this, UserHandle.USER_ALL);
                mSystemExService.getContentResolver().registerContentObserver(
                        Settings.System.getUriFor(OPTIMIZED_CHARGE_FLOOR),
                        false, this, UserHandle.USER_ALL);
                mSystemExService.getContentResolver().registerContentObserver(
                        Settings.System.getUriFor(OPTIMIZED_CHARGE_ENABLED),
                        false, this, UserHandle.USER_ALL);
                mSystemExService.getContentResolver().registerContentObserver(
                        Settings.System.getUriFor(OPTIMIZED_CHARGE_STATUS),
                        false, this, UserHandle.USER_ALL);
                mSystemExService.getContentResolver().registerContentObserver(
                        Settings.System.getUriFor(OPTIMIZED_CHARGE_TIME),
                        false, this, UserHandle.USER_ALL);
            }
            if (mQuietModeController != null) {
                mSystemExService.getContentResolver().registerContentObserver(
                        Settings.System.getUriFor(WIRELESS_CHARGING_QUIET_MODE_ENABLED),
                        false, this, UserHandle.USER_ALL);
                mSystemExService.getContentResolver().registerContentObserver(
                        Settings.System.getUriFor(WIRELESS_CHARGING_QUIET_MODE_STATUS),
                        false, this, UserHandle.USER_ALL);
                mSystemExService.getContentResolver().registerContentObserver(
                        Settings.System.getUriFor(WIRELESS_CHARGING_QUIET_MODE_TIME),
                        false, this, UserHandle.USER_ALL);
            }
            if (mWirelessChargeController != null) {
                mSystemExService.getContentResolver().registerContentObserver(
                        Settings.System.getUriFor(WIRELESS_CHARGING_ENABLED),
                        false, this, UserHandle.USER_ALL);
                mSystemExService.getContentResolver().registerContentObserver(
                        Settings.System.getUriFor(WIRELESS_REVERSE_CHARGING_ENABLED),
                        false, this, UserHandle.USER_ALL);
                mSystemExService.getContentResolver().registerContentObserver(
                        Settings.System.getUriFor(WIRELESS_REVERSE_CHARGING_MIN_LEVEL),
                        false, this, UserHandle.USER_ALL);
            }
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            switch (uri.getLastPathSegment()) {
                case OPTIMIZED_CHARGE_CEILING:
                case OPTIMIZED_CHARGE_ENABLED:
                case OPTIMIZED_CHARGE_FLOOR:
                case OPTIMIZED_CHARGE_STATUS:
                case OPTIMIZED_CHARGE_TIME:
                    mOptimizedChargeController.updateSettings();
                    break;
                case WIRELESS_CHARGING_QUIET_MODE_ENABLED:
                case WIRELESS_CHARGING_QUIET_MODE_STATUS:
                case WIRELESS_CHARGING_QUIET_MODE_TIME:
                    mQuietModeController.updateSettings();
                    break;
                case WIRELESS_CHARGING_ENABLED:
                    mWirelessChargeController.updateWirelessChargingSettings();
                    break;
                case WIRELESS_REVERSE_CHARGING_ENABLED:
                case WIRELESS_REVERSE_CHARGING_MIN_LEVEL:
                    mWirelessChargeController.updateReverseChargingSettings();
                    break;
            }
        }
    }

    private BatteryFeatureController() {
        mServiceThread = new ServiceThread(TAG, THREAD_PRIORITY_DEFAULT, false);
        mServiceThread.start();
        mHandler = new Handler(mServiceThread.getLooper());
    }

    public void initSystemExService(SunSystemExService service) {
        mSystemExService = service;
        mBatteryFeatureManager = BatteryFeatureManager.getInstance();
    }

    public void onSystemServicesReady() {
        mHandler.post(() -> {
            logD("onSystemServicesReady");

            mNotificationPoster = new NotificationPoster(mSystemExService.getContext());

            if (mBatteryFeatureManager.hasFeature(SUSPEND_CHARGING)) {
                mOptimizedChargeController = new OptimizedChargeController(mHandler,
                        mSystemExService, mBatteryFeatureManager, mNotificationPoster);
            } else {
                mOptimizedChargeController = null;
                logD("OptimizedChargeController is not supported");
            }

            if (mBatteryFeatureManager.hasFeature(WIRELESS_CHARGING_QUIET_MODE)) {
                mQuietModeController = new QuietModeController(mHandler,
                        mSystemExService, mBatteryFeatureManager, mNotificationPoster);
            } else {
                mQuietModeController = null;
                logD("QuietModeController is not supported");
            }

            if (mBatteryFeatureManager.hasFeature(WIRELESS_CHARGING_RX) ||
                    mBatteryFeatureManager.hasFeature(WIRELESS_CHARGING_TX)) {
                mWirelessChargeController = new WirelessChargeController(
                        mSystemExService, mBatteryFeatureManager, mNotificationPoster);
            } else {
                mWirelessChargeController = null;
                logD("WirelessChargeController is not supported");
            }

            mSettingsObserver = new SettingsObserver(mHandler);
            mSettingsObserver.observe();
        });
    }

    public void onBootCompleted() {
        mHandler.post(() -> {
            logD("onBootCompleted");
            if (mOptimizedChargeController != null) {
                mOptimizedChargeController.onBootCompleted();
            }
            if (mQuietModeController != null) {
                mQuietModeController.onBootCompleted();
            }
            if (mWirelessChargeController != null) {
                mWirelessChargeController.onBootCompleted();
            }
        });
    }

    public void onBatteryStateChanged() {
        mHandler.post(() -> {
            if (mOptimizedChargeController != null) {
                mOptimizedChargeController.onBatteryStateChanged();
            }
            if (mWirelessChargeController != null) {
                mWirelessChargeController.onBatteryStateChanged();
            }
        });
    }

    public void onPowerSaveChanged() {
        mHandler.post(() -> {
            if (mWirelessChargeController != null) {
                mWirelessChargeController.onPowerSaveChanged();
            }
        });
    }

    private static void logD(String msg) {
        if (DEBUG_BATTERY_FEATURE) {
            Slog.d(TAG, msg);
        }
    }

    static void logD(String tag, String msg) {
        if (DEBUG_BATTERY_FEATURE) {
            Slog.d(TAG + "::" + tag, msg);
        }
    }
}
