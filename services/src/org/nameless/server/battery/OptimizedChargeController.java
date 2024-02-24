/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.server.battery;

import static android.content.Intent.ACTION_TIME_CHANGED;
import static android.content.Intent.ACTION_TIMEZONE_CHANGED;

import static com.android.internal.util.nameless.BatteryFeatureSettingsHelper.OPTIMIZED_CHARGE_ALWAYS;
import static com.android.internal.util.nameless.BatteryFeatureSettingsHelper.OPTIMIZED_CHARGE_SCHEDULED;
import static com.android.internal.util.nameless.BatteryFeatureSettingsHelper.optimizedChargingStatusToString;

import static org.nameless.server.battery.BatteryFeatureController.logD;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;

import com.android.internal.util.nameless.BatteryFeatureSettingsHelper;

import java.util.Calendar;

import org.nameless.os.BatteryFeatureManager;
import org.nameless.server.NamelessSystemExService;

class OptimizedChargeController {

    private static final String TAG = "OptimizedChargeController";

    private final AlarmManager mAlarmManager;
    private final BatteryFeatureManager mBatteryFeatureManager;
    private final ContentResolver mResolver;
    private final Context mContext;
    private final Handler mHandler;
    private final NamelessSystemExService mService;
    private final NotificationPoster mPoster;

    private final UpdateAlarm mUpdateAlarm = new UpdateAlarm();

    private boolean mEnabled;
    private int mStatus;
    private int mCeiling;
    private int mFloor;
    private String mScheduledTime;

    private boolean mInScheduledTime = false;
    private boolean mLastSuspended = false;
    private boolean mTimeRegistered = false;

    private final BroadcastReceiver mTimeChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ACTION_TIME_CHANGED:
                case ACTION_TIMEZONE_CHANGED:
                    logD(TAG, "User changed time/timezone, update alarm");
                    setUpdateAlarm();
                    break;
            }
        }
    };

    private final class UpdateAlarm implements AlarmManager.OnAlarmListener {
        @Override
        public void onAlarm() {
            setUpdateAlarm();
        }

        public void set(Calendar time) {
            set(time.getTimeInMillis());
        }

        public void set(long time) {
            cancel();
            mAlarmManager.setExact(AlarmManager.RTC_WAKEUP, time, TAG, this, mHandler);
            logD(TAG, "New alarm set, time: " + time);
        }

        public void cancel() {
            mAlarmManager.cancel(this);
        }
    }

    OptimizedChargeController(Handler handler, NamelessSystemExService service,
            BatteryFeatureManager batteryFeatureManager, NotificationPoster poster) {
        mHandler = handler;
        mService = service;
        mBatteryFeatureManager = batteryFeatureManager;
        mPoster = poster;
        mResolver = service.getContentResolver();
        mContext = service.getContext();
        mAlarmManager = mContext.getSystemService(AlarmManager.class);
    }

    void onBatteryStateChanged() {
        logD(TAG, "onBatteryStateChanged");
        updateState();
    }

    void onBootCompleted() {
        logD(TAG, "onBootCompleted");
        updateSettings();
    }

    void updateSettings() {
        mEnabled = BatteryFeatureSettingsHelper.getOptimizedChargingEnabled(mContext);
        mStatus = BatteryFeatureSettingsHelper.getOptimizedChargingStatus(mContext);
        mCeiling = BatteryFeatureSettingsHelper.getOptimizedChargingCeiling(mContext);
        mFloor = BatteryFeatureSettingsHelper.getOptimizedChargingFloor(mContext);
        mScheduledTime = BatteryFeatureSettingsHelper.getOptimizedChargingTime(mContext);
        logD(TAG, "updateSettings, mEnabled: " + mEnabled +
                ", mStatus: " + optimizedChargingStatusToString(mStatus) +
                ", mCeiling: " + mCeiling + ", mFloor: " + mFloor +
                ", mScheduledTime: " + mScheduledTime);

        mUpdateAlarm.cancel();  
        setTimeReceiver(false);

        if (mStatus == OPTIMIZED_CHARGE_SCHEDULED) {
            setTimeReceiver(true);
            setUpdateAlarm();
        } else {
            updateState();
        }
    }

    private void updateState() {
        if (!mService.isDevicePlugged()) {
            if (mLastSuspended) {
                logD(TAG, "updateState, unsuspend charging due to device unplugged");
                setChargingSuspended(false);
            }
            return;
        }
        if (!mEnabled) {
            if (mLastSuspended) {
                logD(TAG, "updateState, unsuspend charging due to feature is turned off");
                setChargingSuspended(false);
            }
            return;
        }
        if (mStatus == OPTIMIZED_CHARGE_SCHEDULED && !mInScheduledTime) {
            if (mLastSuspended) {
                logD(TAG, "updateState, unsuspend charging due to time not in schedule");
                setChargingSuspended(false);
            }
            return;
        }
        if (mService.getBatteryLevel() >= mCeiling) {
            if (!mLastSuspended) {
                logD(TAG, "updateState, suspend charging due to battery level higher than ceiling");
                setChargingSuspended(true);
            }
            return;
        }
        if (mService.getBatteryLevel() < mFloor) {
            if (mLastSuspended) {
                logD(TAG, "updateState, unsuspend charging due to battery level lower than floor");
                setChargingSuspended(false);
            }
            return;
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

    private void setUpdateAlarm() {
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
            mInScheduledTime = false;
            logD(TAG, "setUpdateAlarm, start time equals end time, set mInScheduledTime to false");
        } else if (currentTime.before(start)) {
            mInScheduledTime = false;
            mUpdateAlarm.set(start);
            logD(TAG, "setUpdateAlarm, current time is before start time, set mInScheduledTime to false");
        } else if (currentTime.compareTo(start) >= 0 && currentTime.before(end)) {
            mInScheduledTime = true;
            mUpdateAlarm.set(end);
            logD(TAG, "setUpdateAlarm, current time is in schedule, set mInScheduledTime to true");
        }

        updateState();
    }

    private void setChargingSuspended(boolean suspended) {
        logD(TAG, "setChargingSuspended, suspended: " + suspended);
        mBatteryFeatureManager.setChargingSuspended(suspended);
        mLastSuspended = suspended;
        if (suspended) {
            mPoster.postOptimizedChargeNotif();
        } else {
            mPoster.cancelOptimizedChargeNotif();
        }
    }
}
