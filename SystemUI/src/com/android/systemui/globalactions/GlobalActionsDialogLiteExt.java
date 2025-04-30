/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.globalactions;

import static org.sun.provider.SettingsExt.Secure.ADVANCED_REBOOT;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.SystemProperties;
import android.os.UserHandle;

import com.android.systemui.settings.UserTracker;
import com.android.systemui.util.settings.SecureSettings;

import java.util.concurrent.Executor;

class GlobalActionsDialogLiteExt {

    private static final String GLOBAL_ACTION_KEY_REBOOT_RECOVERY = "reboot_recovery";
    private static final String GLOBAL_ACTION_KEY_REBOOT_BOOTLOADER = "reboot_bootloader";
    private static final String GLOBAL_ACTION_KEY_REBOOT_SYSUI = "reboot_sysui";

    private static class InstanceHolder {
        private static GlobalActionsDialogLiteExt INSTANCE = new GlobalActionsDialogLiteExt();
    }

    static GlobalActionsDialogLiteExt getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private ContentObserver mSettingsObserver;
    private SecureSettings mSecureSettings;
    private UserTracker mUserTracker;
    private UserTracker.Callback mUserTrackerCallback;

    private String[] mDefaultMenuActions;
    private String[] mRebootMenuActions;
    private String[] mCurrentMenuActions;

    private boolean mAdvancedReboot = false;
    private boolean mRebootMenu = false;
    private boolean mUsePowerOptions = false;

    void init(Context context, Handler handler, Executor executor,
            SecureSettings secureSettings, UserTracker userTracker) {
        mDefaultMenuActions = context.getResources().getStringArray(
                com.android.internal.R.array.config_globalActionsList);
        mRebootMenuActions = context.getResources().getStringArray(
                com.android.internal.R.array.config_rebootActionsList);

        mSecureSettings = secureSettings;
        mUserTracker = userTracker;
        mSettingsObserver = new ContentObserver(handler) {
            @Override
            public void onChange(boolean selfChange) {
                mAdvancedReboot = mSecureSettings.getIntForUser(ADVANCED_REBOOT, 0,
                        mUserTracker.getUserId()) == 1;
            }
        };
        mSecureSettings.registerContentObserverForUserSync(
                ADVANCED_REBOOT,
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

    void onDestroy() {
        mUserTracker.removeCallback(mUserTrackerCallback);
        mSecureSettings.unregisterContentObserverSync(mSettingsObserver);
    }

    void onShowOrHideDialog() {
        mCurrentMenuActions = mDefaultMenuActions;
        mRebootMenu = false;
    }

    void onRestartPress() {
        mCurrentMenuActions = mRebootMenuActions;
        mRebootMenu = true;
    }

    String[] getCurrentMenuActions() {
        return mCurrentMenuActions;
    }

    String[] getRebootMenuActions() {
        return mRebootMenuActions;
    }

    void setUsePowerOptions(boolean use) {
        mUsePowerOptions = use;
    }

    boolean isAdvancedRebootEnabled() {
        return mAdvancedReboot;
    }

    boolean isRebootMenu() {
        return mRebootMenu;
    }

    boolean isUsePowerOptions() {
        return mUsePowerOptions;
    }

    boolean shouldAddBootloaderAction(String actionKey) {
        return isAdvancedRebootEnabled() && GLOBAL_ACTION_KEY_REBOOT_BOOTLOADER.equals(actionKey);
    }

    boolean shouldAddRecoveryAction(String actionKey) {
        return isAdvancedRebootEnabled() && GLOBAL_ACTION_KEY_REBOOT_RECOVERY.equals(actionKey);
    }

    boolean shouldAddSystemUIAction(String actionKey) {
        return isAdvancedRebootEnabled() && GLOBAL_ACTION_KEY_REBOOT_SYSUI.equals(actionKey);
    }

    boolean isFastbootAvailable() {
        final boolean dynamic = SystemProperties.getBoolean("ro.boot.dynamic_partitions", false);
        final boolean retrofit = SystemProperties.getBoolean("ro.boot.dynamic_partitions_retrofit", false);
        final boolean overrideFastboot = SystemProperties.getBoolean("ro.fastbootd.available", false);
        return dynamic || retrofit || overrideFastboot;
    }
}
