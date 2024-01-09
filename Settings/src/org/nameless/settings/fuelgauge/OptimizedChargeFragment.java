/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.settings.fuelgauge;

import static org.nameless.provider.SettingsExt.System.OPTIMIZED_CHARGE_CEILING;
import static org.nameless.provider.SettingsExt.System.OPTIMIZED_CHARGE_ENABLED;
import static org.nameless.provider.SettingsExt.System.OPTIMIZED_CHARGE_FLOOR;
import static org.nameless.provider.SettingsExt.System.OPTIMIZED_CHARGE_STATUS;
import static org.nameless.provider.SettingsExt.System.OPTIMIZED_CHARGE_TIME;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.app.settings.SettingsEnums;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.format.DateFormat;

import androidx.preference.Preference;

import com.android.internal.util.nameless.BatteryFeatureSettingsHelper;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;

public class OptimizedChargeFragment extends DashboardFragment {

    private static final String TAG = "OptimizedChargeFragment";

    private static final String KEY_TIME_START = "optimized_charge_start_time";
    private static final String KEY_TIME_END = "optimized_charge_end_time";

    private static final int DIALOG_ID = 1999;

    private static final int DIALOG_START_TIME = 0;
    private static final int DIALOG_END_TIME = 1;

    private final SettingsObserver mObserver = new SettingsObserver();

    private final class SettingsObserver extends ContentObserver {

        private final Uri mCeilingUri = Settings.System.getUriFor(
                OPTIMIZED_CHARGE_CEILING);
        private final Uri mEnabledUri = Settings.System.getUriFor(
                OPTIMIZED_CHARGE_ENABLED);
        private final Uri mFloorUri = Settings.System.getUriFor(
                OPTIMIZED_CHARGE_FLOOR);
        private final Uri mStatusUri = Settings.System.getUriFor(
                OPTIMIZED_CHARGE_STATUS);
        private final Uri mTimeUri = Settings.System.getUriFor(
                OPTIMIZED_CHARGE_TIME);

        SettingsObserver() {
            super(new Handler());
        }

        void register(ContentResolver cr) {
            cr.registerContentObserver(mCeilingUri, false, this);
            cr.registerContentObserver(mEnabledUri, false, this);
            cr.registerContentObserver(mFloorUri, false, this);
            cr.registerContentObserver(mStatusUri, false, this);
            cr.registerContentObserver(mTimeUri, false, this);
        }

        void unregister(ContentResolver cr) {
            cr.unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            updatePreferenceStates();
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mObserver.register(getContext().getContentResolver());
    }

    @Override
    public void onDestroy() {
        mObserver.unregister(getContext().getContentResolver());
        super.onDestroy();
    }

    @Override
    public Dialog onCreateDialog(final int dialogId) {
        if (dialogId == DIALOG_START_TIME || dialogId == DIALOG_END_TIME) {
            String[] times = BatteryFeatureSettingsHelper.
                    getOptimizedChargingTime(getContext()).split(",");
            boolean isStart = dialogId == DIALOG_START_TIME;
            int hour, minute;
            TimePickerDialog.OnTimeSetListener listener = (view, hourOfDay, minute1) -> {
                updateTimeSetting(isStart, hourOfDay, minute1);
            };
            if (isStart) {
                String[] startValue = times[0].split(":", 0);
                hour = Integer.parseInt(startValue[0]);
                minute = Integer.parseInt(startValue[1]);
            } else {
                String[] endValue = times[1].split(":", 0);
                hour = Integer.parseInt(endValue[0]);
                minute = Integer.parseInt(endValue[1]);
            }
            return new TimePickerDialog(getContext(), listener,
                    hour, minute, DateFormat.is24HourFormat(getContext()));
        }
        return super.onCreateDialog(dialogId);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (KEY_TIME_START.equals(preference.getKey())) {
            writePreferenceClickMetric(preference);
            showDialog(DIALOG_START_TIME);
            return true;
        } else if (KEY_TIME_END.equals(preference.getKey())) {
            writePreferenceClickMetric(preference);
            showDialog(DIALOG_END_TIME);
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
        return DIALOG_ID;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.PAGE_UNKNOWN;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.optimized_charge;
    }

    private void updateTimeSetting(boolean since, int hour, int minute) {
        String[] times = BatteryFeatureSettingsHelper.
                getOptimizedChargingTime(getContext()).split(",");
        String nHour = "";
        String nMinute = "";
        if (hour < 10) nHour += "0";
        if (minute < 10) nMinute += "0";
        nHour += String.valueOf(hour);
        nMinute += String.valueOf(minute);
        times[since ? 0 : 1] = nHour + ":" + nMinute;
        BatteryFeatureSettingsHelper.setOptimizedChargingTime(
                getContext(), times[0] + "," + times[1]);
    }
}
