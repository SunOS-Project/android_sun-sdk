/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.server;

import static android.content.Intent.ACTION_BATTERY_CHANGED;
import static android.content.Intent.ACTION_PACKAGE_ADDED;
import static android.content.Intent.ACTION_PACKAGE_CHANGED;
import static android.content.Intent.ACTION_PACKAGE_FULLY_REMOVED;
import static android.content.Intent.ACTION_PACKAGE_REMOVED;
import static android.content.Intent.ACTION_PACKAGE_REPLACED;
import static android.content.Intent.ACTION_SCREEN_OFF;
import static android.content.Intent.ACTION_SCREEN_ON;
import static android.content.Intent.ACTION_SHUTDOWN;
import static android.content.Intent.ACTION_USER_PRESENT;
import static android.content.Intent.EXTRA_REPLACING;
import static android.os.PowerManager.ACTION_POWER_SAVE_MODE_CHANGED;
import static android.os.Process.THREAD_PRIORITY_DEFAULT;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.BatteryManagerInternal;
import android.os.Handler;
import android.os.PowerManager;

import com.android.internal.util.nameless.DeviceConfigUtils;

import com.android.server.LocalServices;
import com.android.server.ServiceThread;
import com.android.server.SystemService;
import com.android.server.wm.TopActivityRecorder;

import org.nameless.display.DisplayFeatureManager;
import org.nameless.os.BatteryFeatureManager;
import org.nameless.os.PocketManager;
import org.nameless.server.app.AppPropsController;
import org.nameless.server.app.GameModeController;
import org.nameless.server.battery.BatteryFeatureController;
import org.nameless.server.display.DisplayFeatureController;
import org.nameless.server.display.DisplayRefreshRateController;
import org.nameless.server.pm.LauncherStateController;
import org.nameless.server.policy.DozeController;
import org.nameless.server.policy.PocketModeController;
import org.nameless.server.sensors.SensorBlockController;
import org.nameless.server.vibrator.LinearmotorVibratorController;
import org.nameless.server.wm.DisplayResolutionController;
import org.nameless.server.wm.DisplayRotationController;

public class NamelessSystemExService extends SystemService {

    private static final String TAG = "NamelessSystemExService";

    private final ContentResolver mResolver;

    private final boolean mBatteryFeatureSupported =
            BatteryFeatureManager.getInstance().isSupported();
    private final boolean mDisplayFeatureSupported =
            DisplayFeatureManager.getInstance().isSupported();
    private final boolean mPocketModeSupported;

    private Handler mHandler;
    private ServiceThread mWorker;

    private BatteryManagerInternal mBatteryManagerInternal;

    private PackageRemovedListener mPackageRemovedListener;
    private PowerStateListener mPowerStateListener;
    private ScreenStateListener mScreenStateListener;
    private ShutdownListener mShutdownListener;

    private final Object mLock = new Object();

    private String mTopFullscreenPackage = "";

    private int mBatterLevel;
    private boolean mPlugged;
    private boolean mPowerSave;

    public NamelessSystemExService(Context context) {
        super(context);
        mResolver = context.getContentResolver();
        mPocketModeSupported = PocketManager.isSupported(context);
    }

    @Override
    public void onBootPhase(int phase) {
        if (phase == PHASE_SYSTEM_SERVICES_READY) {
            mBatteryManagerInternal = LocalServices.getService(BatteryManagerInternal.class);
            mPackageRemovedListener = new PackageRemovedListener();
            mPowerStateListener = new PowerStateListener();
            mScreenStateListener = new ScreenStateListener();
            mShutdownListener = new ShutdownListener();
            AppPropsController.getInstance().onSystemServicesReady();
            if (mBatteryFeatureSupported) {
                BatteryFeatureController.getInstance().onSystemServicesReady();
            }
            if (mDisplayFeatureSupported) {
                DisplayFeatureController.getInstance().onSystemServicesReady();
            }
            if (mPocketModeSupported) {
                PocketModeController.getInstance().onSystemServicesReady();
            }
            DozeController.getInstance().onSystemServicesReady();
            GameModeController.getInstance().onSystemServicesReady();
            LauncherStateController.getInstance().onSystemServicesReady();
            LinearmotorVibratorController.getInstance().onSystemServicesReady();
            return;
        }

        if (phase == PHASE_BOOT_COMPLETED) {
            mHandler.post(() -> DeviceConfigUtils.setDefaultProperties(null, null));
            if (mBatteryFeatureSupported) {
                BatteryFeatureController.getInstance().onBootCompleted();
            }
            if (mDisplayFeatureSupported) {
                DisplayFeatureController.getInstance().onBootCompleted();
            }
            if (mPocketModeSupported) {
                PocketModeController.getInstance().onBootCompleted();
            }
            DisplayRefreshRateController.getInstance().onBootCompleted();
            DisplayRotationController.getInstance().onBootCompleted();
            GameModeController.getInstance().onBootCompleted();
            LauncherStateController.getInstance().onBootCompleted();
            SensorBlockController.getInstance().onBootCompleted();
            mPackageRemovedListener.register();
            mPowerStateListener.register();
            mScreenStateListener.register();
            mShutdownListener.register();
            return;
        }
    }

    @Override
    public void onStart() {
        mWorker = new ServiceThread(TAG, THREAD_PRIORITY_DEFAULT, false);
        mWorker.start();
        mHandler = new Handler(mWorker.getLooper());

        AppPropsController.getInstance().initSystemExService(this);
        TopActivityRecorder.getInstance().initSystemExService(this);
        if (mBatteryFeatureSupported) {
            BatteryFeatureController.getInstance().initSystemExService(this);
        }
        if (mDisplayFeatureSupported) {
            DisplayFeatureController.getInstance().initSystemExService(this);
        }
        if (mPocketModeSupported) {
            PocketModeController.getInstance().initSystemExService(this);
        }
        DisplayRefreshRateController.getInstance().initSystemExService(this);
        DisplayResolutionController.getInstance().initSystemExService(this);
        DisplayRotationController.getInstance().initSystemExService(this);
        DozeController.getInstance().initSystemExService(this);
        GameModeController.getInstance().initSystemExService(this);
        LauncherStateController.getInstance().initSystemExService(this);
        LinearmotorVibratorController.getInstance().initSystemExService(this);
        SensorBlockController.getInstance().initSystemExService(this);
    }

    @Override
    public void onUserStarting(TargetUser user) {
        final int userId = user.getUserIdentifier();
        LauncherStateController.getInstance().onUserStarting(userId);
    }

    @Override
    public void onUserSwitching(TargetUser from, TargetUser to) {
        final int newUserId = to.getUserIdentifier();
        DisplayRefreshRateController.getInstance().onUserSwitching(newUserId);
        DisplayRotationController.getInstance().onUserSwitching(newUserId);
        DozeController.getInstance().onUserSwitching(newUserId);
        GameModeController.getInstance().onUserSwitching(newUserId);
        PocketModeController.getInstance().onUserSwitching(newUserId);
        SensorBlockController.getInstance().onUserSwitching(newUserId);
    }

    private void onPackageRemoved(String packageName) {
        DisplayRefreshRateController.getInstance().onPackageRemoved(packageName);
        DisplayRotationController.getInstance().onPackageRemoved(packageName);
        GameModeController.getInstance().onPackageRemoved(packageName);
        SensorBlockController.getInstance().onPackageRemoved(packageName);
    }

    private void onScreenOff() {
        DisplayRefreshRateController.getInstance().onScreenOff();
        DisplayRotationController.getInstance().onScreenOff();
        GameModeController.getInstance().onScreenOff();
        DozeController.getInstance().onScreenOff();
    }

    private void onScreenOn() {
        DisplayRefreshRateController.getInstance().onScreenOn();
        DozeController.getInstance().onScreenOn();
    }

    private void onScreenUnlocked() {
        onTopFullscreenPackageChanged(mTopFullscreenPackage);
    }

    public void onTopFullscreenPackageChanged(String packageName) {
        synchronized (mLock) {
            mTopFullscreenPackage = packageName;
            DisplayRefreshRateController.getInstance().updateRefreshRate(packageName);
            DisplayRotationController.getInstance().updateAutoRotate(packageName);
            GameModeController.getInstance().updateGameModeState(packageName, false);
            SensorBlockController.getInstance().updateTopPackage(packageName);
        }
    }

    private void onBatteryStateChanged() {
        if (mBatteryFeatureSupported) {
            BatteryFeatureController.getInstance().onBatteryStateChanged();
        }
    }

    private void onPowerSaveChanged() {
        if (mBatteryFeatureSupported) {
            BatteryFeatureController.getInstance().onPowerSaveChanged();
        }
    }

    private void onShutdown() {
        DisplayRotationController.getInstance().onShutdown();
    }

    public ContentResolver getContentResolver() {
        return mResolver;
    }

    public Handler getHandler() {
        return mHandler;
    }

    public String getTopFullscreenPackage() {
        synchronized (mLock) {
            return mTopFullscreenPackage;
        }
    }

    public int getBatteryLevel() {
        return mBatterLevel;
    }

    public boolean isDevicePlugged() {
        return mPlugged;
    }

    public boolean isPowerSaveMode() {
        return mPowerSave;
    }

    private final class PackageRemovedListener extends BroadcastReceiver {

        private static final String EXTRA_UID = "EXTRA_UID";

        @Override
        public void onReceive(Context context, Intent intent) {
            final String packageName = getPackageName(intent);
            if (packageName == null) {
                return;
            }
            switch (intent.getAction()) {
                case ACTION_PACKAGE_FULLY_REMOVED:
                case ACTION_PACKAGE_REMOVED:
                    if (!intent.getBooleanExtra(EXTRA_REPLACING, false)) {
                        onPackageRemoved(packageName);
                    }
                    break;
            }
        }

        public void register() {
            final IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_PACKAGE_FULLY_REMOVED);
            filter.addAction(ACTION_PACKAGE_REMOVED);
            filter.addDataScheme("package");
            getContext().registerReceiverForAllUsers(this, filter, null, mHandler);
        }

        private static String getPackageName(Intent intent) {
            final Uri uri = intent.getData();
            return uri != null ? uri.getSchemeSpecificPart() : null;
        }
    }

    private final class PowerStateListener extends BroadcastReceiver {

        private final PowerManager mPowerManager;

        public PowerStateListener() {
            mPowerManager = getContext().getSystemService(PowerManager.class);
            mBatterLevel = mBatteryManagerInternal.getBatteryLevel();
            mPlugged = mBatteryManagerInternal.isPowered(BatteryManager.BATTERY_PLUGGED_ANY);
            mPowerSave = mPowerManager.isPowerSaveMode();
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ACTION_BATTERY_CHANGED:
                    final int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                    final int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                    final int status = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
                    if (level < 0 || scale < 0) {
                        break;
                    }
                    final int newLevel = (int) (level * 100 / (float) scale);
                    final boolean newPlugged = status != 0;

                    boolean shouldUpdate = false;

                    if (newLevel != mBatterLevel) {
                        mBatterLevel = newLevel;
                        shouldUpdate = true;
                    }
                    if (newPlugged != mPlugged) {
                        mPlugged = newPlugged;
                        shouldUpdate = true;
                    }
                    if (shouldUpdate) {
                        onBatteryStateChanged();
                    }
                    break;
                case ACTION_POWER_SAVE_MODE_CHANGED:
                    mPowerSave = mPowerManager.isPowerSaveMode();
                    onPowerSaveChanged();
                    break;
            }
        }

        public void register() {
            final IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_BATTERY_CHANGED);
            filter.addAction(ACTION_POWER_SAVE_MODE_CHANGED);
            getContext().registerReceiverForAllUsers(this, filter, null, mHandler);
        }
    }

    private final class ScreenStateListener extends BroadcastReceiver {

        private final KeyguardManager mKeyguardManager;
        private final PowerManager mPowerManager;

        private boolean mHandledUnlock = false;

        public ScreenStateListener() {
            mKeyguardManager = getContext().getSystemService(KeyguardManager.class);
            mPowerManager = getContext().getSystemService(PowerManager.class);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ACTION_SCREEN_OFF:
                    mHandledUnlock = false;
                    onScreenOff();
                    break;
                case ACTION_SCREEN_ON:
                    if (!mHandledUnlock && !mKeyguardManager.isKeyguardLocked()) {
                        onScreenUnlocked();
                    } else {
                        onScreenOn();
                    }
                    break;
                case ACTION_USER_PRESENT:
                    mHandledUnlock = true;
                    onScreenUnlocked();
                    break;
            }
        }

        public void register() {
            final IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_SCREEN_OFF);
            filter.addAction(ACTION_SCREEN_ON);
            filter.addAction(ACTION_USER_PRESENT);
            getContext().registerReceiverForAllUsers(this, filter, null, mHandler);
        }
    }

    private final class ShutdownListener extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ACTION_SHUTDOWN:
                    onShutdown();
                    break;
            }
        }

        public void register() {
            final IntentFilter filter = new IntentFilter(ACTION_SHUTDOWN);
            getContext().registerReceiverForAllUsers(this, filter, null, mHandler);
        }
    }
}
