/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.globalactions;

import static org.nameless.provider.SettingsExt.Secure.ADVANCED_REBOOT;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.UserHandle;

import com.android.systemui.settings.UserTracker;
import com.android.systemui.util.settings.SecureSettings;

import java.util.concurrent.Executor;

public class GlobalActionsDialogLiteExt {

    private static final String GLOBAL_ACTION_KEY_REBOOT_RECOVERY = "reboot_recovery";
    private static final String GLOBAL_ACTION_KEY_REBOOT_BOOTLOADER = "reboot_bootloader";
    private static final String GLOBAL_ACTION_KEY_REBOOT_SYSUI = "reboot_sysui";

    private static class InstanceHolder {
        private static GlobalActionsDialogLiteExt INSTANCE = new GlobalActionsDialogLiteExt();
    }

    public static GlobalActionsDialogLiteExt getInstance() {
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

    public void init(Context context, Handler handler, Executor executor,
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
        mSecureSettings.registerContentObserverForUser(
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

    public void onDestroy() {
        mUserTracker.removeCallback(mUserTrackerCallback);
        mSecureSettings.unregisterContentObserver(mSettingsObserver);
    }

    public void onShowOrHideDialog() {
        mCurrentMenuActions = mDefaultMenuActions;
        mRebootMenu = false;
    }

    public void onRestartPress() {
        mCurrentMenuActions = mRebootMenuActions;
        mRebootMenu = true;
    }

    public String[] getCurrentMenuActions() {
        return mCurrentMenuActions;
    }

    public String[] getRebootMenuActions() {
        return mRebootMenuActions;
    }

    public void setUsePowerOptions(boolean use) {
        mUsePowerOptions = use;
    }

    public boolean isAdvancedRebootEnabled() {
        return mAdvancedReboot;
    }

    public boolean isRebootMenu() {
        return mRebootMenu;
    }

    public boolean isUsePowerOptions() {
        return mUsePowerOptions;
    }

    public boolean shouldAddBootloaderAction(String actionKey) {
        return isAdvancedRebootEnabled() && GLOBAL_ACTION_KEY_REBOOT_BOOTLOADER.equals(actionKey);
    }

    public boolean shouldAddRecoveryAction(String actionKey) {
        return isAdvancedRebootEnabled() && GLOBAL_ACTION_KEY_REBOOT_RECOVERY.equals(actionKey);
    }

    public boolean shouldAddSystemUIAction(String actionKey) {
        return isAdvancedRebootEnabled() && GLOBAL_ACTION_KEY_REBOOT_SYSUI.equals(actionKey);
    }
}
