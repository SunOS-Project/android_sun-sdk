/*
 * Copyright (C) 2022-2023 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.server.battery;

import static android.content.Intent.ACTION_TIME_CHANGED;
import static android.content.Intent.ACTION_TIMEZONE_CHANGED;

import static com.android.internal.util.nameless.BatteryFeatureSettingsHelper.QUIET_MODE_ALWAYS;
import static com.android.internal.util.nameless.BatteryFeatureSettingsHelper.QUIET_MODE_SCHEDULED;
import static com.android.internal.util.nameless.BatteryFeatureSettingsHelper.quietModeStatusToString;

import static org.nameless.server.battery.BatteryFeatureController.logD;

import static vendor.nameless.hardware.battery.Feature.WIRELESS_CHARGING_QUIET_MODE;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;

import com.android.internal.util.nameless.BatteryFeatureSettingsHelper;

import java.util.Calendar;

import org.nameless.os.BatteryFeatureManager;
import org.nameless.server.NamelessSystemExService;

class QuietModeController {

    private static final String TAG = "QuietModeController";

    private final AlarmManager mAlarmManager;
    private final BatteryFeatureManager mBatteryFeatureManager;
    private final Context mContext;
    private final Handler mHandler;
    private final NotificationPoster mPoster;

    private final QuietModeAlarm mQuietModeAlarm = new QuietModeAlarm();

    private boolean mEnabled;
    private int mStatus;
    private String mScheduledTime;

    private boolean mNextEnable = false;
    private boolean mTimeRegistered = false;

    private final BroadcastReceiver mTimeChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ACTION_TIME_CHANGED:
                case ACTION_TIMEZONE_CHANGED:
                    logD(TAG, "User changed time/timezone, update alarm");
                    maybeEnableQuietMode(true);
                    break;
            }
        }
    };

    private final class QuietModeAlarm implements AlarmManager.OnAlarmListener {
        @Override
        public void onAlarm() {
            setQuietMode(mNextEnable);
            maybeEnableQuietMode(false);
        }

        void set(Calendar time) {
            set(time.getTimeInMillis());
        }

        void set(long time) {
            cancel();
            mAlarmManager.setExact(AlarmManager.RTC_WAKEUP, time, TAG, this, mHandler);
            logD(TAG, "New alarm set, time: " + time
                    + ", mNextEnable: " + mNextEnable);
        }

        void cancel() {
            mAlarmManager.cancel(this);
        }
    }

    QuietModeController(Handler handler, NamelessSystemExService service,
            BatteryFeatureManager batteryFeatureManager, NotificationPoster poster) {
        mHandler = handler;
        mBatteryFeatureManager = batteryFeatureManager;
        mPoster = poster;
        mContext = service.getContext();
        mAlarmManager = mContext.getSystemService(AlarmManager.class);
    }

    void onBootCompleted() {
        logD(TAG, "onBootCompleted");
        updateSettings();
    }

    void updateSettings() {
        mEnabled = BatteryFeatureSettingsHelper.getQuietModeEnabled(mContext);
        mStatus = BatteryFeatureSettingsHelper.getQuietModeStatus(mContext);
        mScheduledTime = BatteryFeatureSettingsHelper.getQuietModeTime(mContext);
        logD(TAG, "updateSettings, mEnabled: " + mEnabled +
                ", mStatus: " + quietModeStatusToString(mStatus) +
                ", mScheduledTime: " + mScheduledTime);

        mQuietModeAlarm.cancel();
        setTimeReceiver(false);

        if (!mEnabled) {
            setQuietMode(false);
            return;
        }
        switch (mStatus) {
            case QUIET_MODE_ALWAYS:
                setQuietMode(true);
                break;
            case QUIET_MODE_SCHEDULED:
                setTimeReceiver(true);
                maybeEnableQuietMode(true);
                break;
        }
    }

    private void setTimeReceiver(boolean register) {
        if (register && !mTimeRegistered) {
            final IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_TIME_CHANGED);
            filter.addAction(ACTION_TIMEZONE_CHANGED);
            mContext.registerReceiver(mTimeChangedReceiver, filter, null, mHandler);
            mTimeRegistered = true;
        } else if (!register && mTimeRegistered) {
            mContext.unregisterReceiver(mTimeChangedReceiver);
            mTimeRegistered = false;
        }
    }

    private void maybeEnableQuietMode(boolean setEnable) {
        Calendar currentTime = Calendar.getInstance();
        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();
        String[] times = mScheduledTime.split(",", 0);
        String[] startValue = times[0].split(":", 0);
        String[] endValue = times[1].split(":", 0);
        start.set(Calendar.HOUR_OF_DAY, Integer.parseInt(startValue[0]));
        start.set(Calendar.MINUTE, Integer.parseInt(startValue[1]));
        start.set(Calendar.SECOND, 0);
        end.set(Calendar.HOUR_OF_DAY, Integer.parseInt(endValue[0]));
        end.set(Calendar.MINUTE, Integer.parseInt(endValue[1]));
        end.set(Calendar.SECOND, 0);

        if (currentTime.before(start) && currentTime.before(end) && end.compareTo(start) < 0) {
            start.add(Calendar.DATE, -1);
        }
        if (start.after(end)) {
            end.add(Calendar.DATE, 1);
        }
        if (currentTime.after(start) && currentTime.compareTo(end) >= 0) {
            start.add(Calendar.DATE, 1);
            end.add(Calendar.DATE, 1);
        }

        if (start.compareTo(end) == 0) {
            logD(TAG, "maybeEnableQuietMode, start time equals end time, return early");
            return;
        }

        mNextEnable = currentTime.before(start);
        logD(TAG, "maybeEnableQuietMode, set mNextEnable to " + mNextEnable);
        mQuietModeAlarm.set(mNextEnable ? start : end);

        if (setEnable) {
            setQuietMode(currentTime.compareTo(start) >= 0 && currentTime.before(end));
        }
    }

    private void setQuietMode(boolean enabled) {
        logD(TAG, "setQuietMode, enabled: " + enabled);
        mBatteryFeatureManager.setFeatureEnabled(WIRELESS_CHARGING_QUIET_MODE, enabled);
    }
}
