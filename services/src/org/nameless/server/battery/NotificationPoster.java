/*
 * Copyright (C) 2022-2023 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.server.battery;

import static org.nameless.server.battery.BatteryFeatureController.logD;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;

import com.android.internal.R;
import com.android.internal.messages.SystemMessageExt;

class NotificationPoster {

    private static final String TAG = "NotificationPoster";
    private static final String CHANNEL_ID = "BatteryFeatureController";

    private final Context mContext;
    private final NotificationManager mNotificationManager;

    private Notification mNotificationOptimizedCharge;
    private Notification mNotificationWirelessTX;

    private boolean mNotifOptimizedChargePosted = false;
    private boolean mNotifWirelessTxPosted = false;

    NotificationPoster(Context context) {
        mContext = context;

        mNotificationManager = context.getSystemService(NotificationManager.class);
        final NotificationChannel ncOptimizedCharge = new NotificationChannel(CHANNEL_ID,
                context.getString(R.string.optimized_charge_notification_channel_label),
                NotificationManager.IMPORTANCE_MIN);
        final NotificationChannel ncWirelessTX = new NotificationChannel(CHANNEL_ID,
                context.getString(R.string.wireless_reverse_charging_notification_channel_label),
                NotificationManager.IMPORTANCE_MIN);
        mNotificationManager.createNotificationChannel(ncOptimizedCharge);
        mNotificationManager.createNotificationChannel(ncWirelessTX);

        buildNotifications();
    }

    private void buildNotifications() {
        Intent intent = new Intent().setComponent(new ComponentName(
                "com.android.settings",
                "com.android.settings.Settings$OptimizedChargeActivity"));
        PendingIntent pIntent = PendingIntent.getActivityAsUser(mContext, 0, intent,
                PendingIntent.FLAG_IMMUTABLE, null, UserHandle.SYSTEM);
        Notification.Builder builder = new Notification.Builder(mContext, CHANNEL_ID);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        builder.setContentTitle(
                mContext.getString(R.string.optimized_charge_notification_title));
        builder.setContentText(
                mContext.getString(R.string.tap_to_view_more_options));
        builder.setContentIntent(pIntent);
        builder.setSmallIcon(R.drawable.ic_battery);
        builder.setOnlyAlertOnce(true);
        mNotificationOptimizedCharge = builder.build();
        mNotificationOptimizedCharge.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;

        intent = new Intent().setComponent(new ComponentName(
                "com.android.settings",
                "com.android.settings.Settings$ReverseChargingActivity"));
        pIntent = PendingIntent.getActivityAsUser(mContext, 0, intent,
                PendingIntent.FLAG_IMMUTABLE, null, UserHandle.SYSTEM);
        builder = new Notification.Builder(mContext, CHANNEL_ID);
        builder.setContentTitle(
                mContext.getString(R.string.wireless_reverse_charging_notification_title));
        builder.setContentText(
                mContext.getString(R.string.tap_to_view_more_options));
        builder.setContentIntent(pIntent);
        builder.setSmallIcon(R.drawable.ic_wireless_reverse_charging);
        builder.setOnlyAlertOnce(true);
        mNotificationWirelessTX = builder.build();
        mNotificationWirelessTX.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
    }

    void postOptimizedChargeNotif() {
        if (mNotifOptimizedChargePosted) {
            return;
        }
        logD(TAG, "postOptimizedChargeNotif");
        mNotificationManager.notify(SystemMessageExt.NOTE_OPTIMIZED_CHARGE, mNotificationOptimizedCharge);
        mNotifOptimizedChargePosted = true;
    }

    void cancelOptimizedChargeNotif() {
        if (!mNotifOptimizedChargePosted) {
            return;
        }
        logD(TAG, "cancelOptimizedChargeNotif");
        mNotificationManager.cancel(SystemMessageExt.NOTE_OPTIMIZED_CHARGE);
        mNotifOptimizedChargePosted = false;
    }

    void postWirelessTxNotif() {
        if (mNotifWirelessTxPosted) {
            return;
        }
        logD(TAG, "postWirelessTxNotif");
        mNotificationManager.notify(SystemMessageExt.NOTE_WIRELESS_REVERSED_CHARGE, mNotificationWirelessTX);
        mNotifWirelessTxPosted = true;
    }

    void cancelWirelessTxNotif() {
        if (!mNotifWirelessTxPosted) {
            return;
        }
        logD(TAG, "cancelWirelessTxNotif");
        mNotificationManager.cancel(SystemMessageExt.NOTE_WIRELESS_REVERSED_CHARGE);
        mNotifWirelessTxPosted = false;
    }
}
