/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.server;

import static android.app.ActivityTaskManager.INVALID_TASK_ID;
import static android.content.Intent.ACTION_BATTERY_CHANGED;
import static android.content.Intent.ACTION_SHUTDOWN;
import static android.os.PowerManager.ACTION_POWER_SAVE_MODE_CHANGED;
import static android.os.Process.THREAD_PRIORITY_DEFAULT;
import static android.os.UserManager.USER_TYPE_PROFILE_CLONE;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManagerInternal;
import android.content.pm.LauncherApps;
import android.content.pm.UserInfo;
import android.os.BatteryManager;
import android.os.BatteryManagerInternal;
import android.os.Binder;
import android.os.Handler;
import android.os.PowerManager;
import android.os.UserHandle;

import com.android.internal.util.nameless.CustomUtils;
import com.android.internal.util.nameless.DeviceConfigUtils;
import com.android.internal.util.nameless.ScreenStateListener;

import com.android.server.LocalServices;
import com.android.server.ServiceThread;
import com.android.server.SystemService;
import com.android.server.pm.UserManagerInternal;
import com.android.server.wm.TopActivityRecorder;

import java.util.List;

import org.nameless.display.DisplayFeatureManager;
import org.nameless.os.BatteryFeatureManager;
import org.nameless.server.app.AppPropsController;
import org.nameless.server.app.GameModeController;
import org.nameless.server.battery.BatteryFeatureController;
import org.nameless.server.content.OnlineConfigController;
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

    private static final String ACTION_REBOOT = "org.nameless.intent.REBOOT_NOW";

    private final ContentResolver mResolver;

    private final boolean mBatteryFeatureSupported =
            BatteryFeatureManager.getInstance().isSupported();
    private final boolean mDisplayFeatureSupported =
            DisplayFeatureManager.getInstance().isSupported();
    private final boolean mPocketModeSupported;

    private Handler mHandler;
    private ServiceThread mWorker;

    private BatteryManagerInternal mBatteryManagerInternal;
    private PackageManagerInternal mPackageManagerInternal;
    private UserManagerInternal mUserManagerInternal;

    private PackageRemovedListener mPackageRemovedListener;
    private PowerStateListener mPowerStateListener;
    private RebootListener mRebootListener;
    private ScreenStateListener mScreenStateListener;
    private ShutdownListener mShutdownListener;

    private String mTopFullscreenPackage = "";
    private int mTopFullscreenTaskId = INVALID_TASK_ID;

    private int mBatterLevel;
    private boolean mPlugged;
    private boolean mPowerSave;

    public NamelessSystemExService(Context context) {
        super(context);
        mResolver = context.getContentResolver();
        mPocketModeSupported = context.getResources().getBoolean(
                com.android.internal.R.bool.config_pocketModeSupported);
    }

    @Override
    public void onBootPhase(int phase) {
        if (phase == PHASE_SYSTEM_SERVICES_READY) {
            mBatteryManagerInternal = LocalServices.getService(BatteryManagerInternal.class);
            mPackageManagerInternal = LocalServices.getService(PackageManagerInternal.class);
            mUserManagerInternal = LocalServices.getService(UserManagerInternal.class);
            mPackageRemovedListener = new PackageRemovedListener();
            mPowerStateListener = new PowerStateListener();
            mRebootListener = new RebootListener();
            mScreenStateListener = new ScreenStateListener(getContext(), mHandler) {
                @Override
                public void onScreenOff() {
                    NamelessSystemExService.this.onScreenOff();
                }

                @Override
                public void onScreenOn() {
                    NamelessSystemExService.this.onScreenOn();
                }

                @Override
                public void onScreenUnlocked() {
                    NamelessSystemExService.this.onScreenUnlocked();
                }
            };
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
            DisplayRotationController.getInstance().onSystemServicesReady();
            DozeController.getInstance().onSystemServicesReady();
            GameModeController.getInstance().onSystemServicesReady();
            LauncherStateController.getInstance().onSystemServicesReady();
            LinearmotorVibratorController.getInstance().onSystemServicesReady();
            SensorBlockController.getInstance().onSystemServicesReady();
            return;
        }

        if (phase == PHASE_BOOT_COMPLETED) {
            mHandler.post(() -> {
                DeviceConfigUtils.setDefaultProperties(null, null);
                final int clonedUserId = getCloneUserId();
                if (clonedUserId != -1) {
                    maybeCleanClonedUser(clonedUserId);
                }
            });
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
            LauncherStateController.getInstance().onBootCompleted();
            mPackageRemovedListener.register();
            mRebootListener.register();
            mPowerStateListener.register();
            mScreenStateListener.setListening(true);
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
        OnlineConfigController.getInstance().initSystemExService(this);
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
        DozeController.getInstance().onScreenOff();
        GameModeController.getInstance().onScreenOff();
        PocketModeController.getInstance().onScreenOff();
    }

    private void onScreenOn() {
        if (mDisplayFeatureSupported) {
            DisplayFeatureController.getInstance().onScreenOn();
        }
        DisplayRefreshRateController.getInstance().onScreenOn();
        DozeController.getInstance().onScreenOn();
        PocketModeController.getInstance().onScreenOn();
    }

    private void onScreenUnlocked() {
        onTopFullscreenPackageChanged(mTopFullscreenPackage, mTopFullscreenTaskId);
        PocketModeController.getInstance().onScreenUnlocked();
    }

    public void onTopFullscreenPackageChanged(String packageName, int taskId) {
        mHandler.post(() -> {
            mTopFullscreenPackage = packageName;
            mTopFullscreenTaskId = taskId;
            DisplayRefreshRateController.getInstance().onTopFullscreenPackageChanged(packageName);
            DisplayRotationController.getInstance().onTopFullscreenPackageChanged(packageName);
            GameModeController.getInstance().onTopFullscreenPackageChanged(packageName, taskId);
            SensorBlockController.getInstance().onTopFullscreenPackageChanged(packageName);
        });
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

    public String getTopFullscreenPackage() {
        return mTopFullscreenPackage;
    }

    public int getTopFullscreenTaskId() {
        return mTopFullscreenTaskId;
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

    public PendingIntent getRebootPendingIntent() {
        return PendingIntent.getBroadcast(getContext(), 0, new Intent(ACTION_REBOOT), PendingIntent.FLAG_IMMUTABLE);
    }

    private int getCloneUserId() {
        for (UserInfo userInfo : mUserManagerInternal.getUsers(false)) {
            if (USER_TYPE_PROFILE_CLONE.equals(userInfo.userType)) {
                return userInfo.id;
            }
        }
        return -1;
    }

    private void maybeCleanClonedUser(int userId) {
        final List<ApplicationInfo> packages = mPackageManagerInternal.getInstalledApplicationsCrossUser(
                0, userId, Binder.getCallingUid());
        boolean hasUserApp = false;
        for (ApplicationInfo info : packages) {
            if ((info.flags &
                    (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) == 0) {
                hasUserApp = true;
                break;
            }
        }
        if (!hasUserApp) {
            mUserManagerInternal.removeUserEvenWhenDisallowed(userId);
            CustomUtils.forceStopDefaultLauncher(getContext());
            getContext().sendBroadcastAsUser(new Intent(
                    CustomUtils.INTENT_RESET_CLONE_USER_ID), UserHandle.SYSTEM);
        }
    }

    private final class PackageRemovedListener extends LauncherApps.Callback {

        private final LauncherApps mLauncherApps;

        public PackageRemovedListener() {
            mLauncherApps = getContext().getSystemService(LauncherApps.class);
        }

        @Override
        public void onPackageAdded(String packageName, UserHandle user) {
            // Do nothing
        }

        @Override
        public void onPackageChanged(String packageName, UserHandle user) {
            // Do nothing
        }

        @Override
        public void onPackageRemoved(String packageName, UserHandle user) {
            final UserInfo userInfo = mUserManagerInternal.getUserInfo(user.getIdentifier());
            if (userInfo != null && USER_TYPE_PROFILE_CLONE.equals(userInfo.userType)) {
                maybeCleanClonedUser(user.getIdentifier());
                return;
            }

            NamelessSystemExService.this.onPackageRemoved(packageName);
        }

        @Override
        public void onPackagesAvailable(String[] packageNames, UserHandle user, boolean replacing) {
            // Do nothing
        }

        @Override
        public void onPackagesUnavailable(String[] packageNames, UserHandle user, boolean replacing) {
            // Do nothing
        }

        public void register() {
            mLauncherApps.registerCallback(this, mHandler);
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

    private final class RebootListener extends BroadcastReceiver {

        private final PowerManager mPowerManager;

        public RebootListener() {
            mPowerManager = getContext().getSystemService(PowerManager.class);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ACTION_REBOOT:
                    mPowerManager.reboot(null);
                    break;
            }
        }

        public void register() {
            final IntentFilter filter = new IntentFilter(ACTION_REBOOT);
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
