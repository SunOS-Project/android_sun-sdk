/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.server.notification;

import static org.sun.provider.SettingsExt.System.SILENT_NOTIFICATION_SCREEN_ON;
import static org.sun.provider.SettingsExt.System.VIBRATION_PATTERN_NOTIFICATION;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.UserHandle;
import android.os.VibrationEffect;
import android.provider.Settings;

import org.sun.os.VibrationPatternManager;
import org.sun.os.VibrationPatternManager.Type;
import org.sun.server.app.GameModeController;

class NotificationManagerServiceExt {

    private static class InstanceHolder {
        private static final NotificationManagerServiceExt INSTANCE = new NotificationManagerServiceExt();
    }

    static NotificationManagerServiceExt getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private static final Uri URI_SILENT_NOTIFICATION_SCREEN_ON =
            Settings.System.getUriFor(SILENT_NOTIFICATION_SCREEN_ON);
    private static final Uri URI_VIBRATION_PATTERN_NOTIFICATION =
            Settings.System.getUriFor(VIBRATION_PATTERN_NOTIFICATION);

    private ContentResolver mResolver;
    private NotificationManagerService mService;

    private boolean mSilentNotificationScreenOn;
    private VibrationEffect mCustomVibrationEffect;

    private boolean mNeedUpdateVibration = false;

    void init(NotificationManagerService service) {
        mService = service;
        mResolver = service.getContext().getContentResolver();
    }

    void observe(ContentObserver observer) {
        mResolver.registerContentObserver(URI_SILENT_NOTIFICATION_SCREEN_ON,
                false, observer, UserHandle.USER_ALL);
        mResolver.registerContentObserver(URI_VIBRATION_PATTERN_NOTIFICATION,
                false, observer, UserHandle.USER_ALL);
    }

    void updateSettings(Uri uri) {
        if (uri == null || URI_SILENT_NOTIFICATION_SCREEN_ON.equals(uri)) {
            updateSilentNotificationScreenOn();
        } 
        if (uri == null || URI_VIBRATION_PATTERN_NOTIFICATION.equals(uri)) {
            updateCustomVibrationEffect();
        }
    }

    boolean shouldSkipSoundVib() {
        if (GameModeController.getInstance().shouldSilentNotification()) {
            return true;
        }
        return mService.getScreenOn() && mSilentNotificationScreenOn;
    }

    void setNeedUpdateVibration() {
        mNeedUpdateVibration = true;
    }

    boolean checkNeedUpdateVibrationAndReset() {
        final boolean ret = mNeedUpdateVibration;
        mNeedUpdateVibration = false;
        return ret;
    }

    VibrationEffect getVibrationEffect() {
        return mCustomVibrationEffect;
    }

    private void updateSilentNotificationScreenOn() {
        mSilentNotificationScreenOn = Settings.System.getIntForUser(
                mResolver, SILENT_NOTIFICATION_SCREEN_ON,
                0, UserHandle.USER_CURRENT) == 1;
    }

    private void updateCustomVibrationEffect() {
        final int patternNumber = Settings.System.getIntForUser(
                mResolver, VIBRATION_PATTERN_NOTIFICATION,
                0, UserHandle.USER_CURRENT);
        mCustomVibrationEffect = VibrationPatternManager.getVibrationFromNumber(
                patternNumber, Type.NOTIFICATION);
    }
}
