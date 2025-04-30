/*
 * Copyright (C) 2022 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.settings.fuelgauge;

import static org.sun.provider.SettingsExt.System.WIRELESS_CHARGING_QUIET_MODE_ENABLED;
import static org.sun.provider.SettingsExt.System.WIRELESS_CHARGING_QUIET_MODE_STATUS;
import static org.sun.provider.SettingsExt.System.WIRELESS_CHARGING_QUIET_MODE_TIME;

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

import com.android.internal.util.sun.BatteryFeatureSettingsHelper;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;

public class WirelessChargingQuietModeFragment extends DashboardFragment {

    private static final String TAG = "WirelessChargingQuietModeFragment";

    private static final String KEY_QUIET_MODE_TIME_START = "wireless_charging_quiet_mode_start_time";
    private static final String KEY_QUIET_MODE_TIME_END = "wireless_charging_quiet_mode_end_time";

    private static final int DIALOG_ID = 1998;

    private static final int DIALOG_START_TIME = 0;
    private static final int DIALOG_END_TIME = 1;

    private final SettingsObserver mObserver = new SettingsObserver();

    private final class SettingsObserver extends ContentObserver {

        private final Uri mEnabledUri = Settings.System.getUriFor(
                WIRELESS_CHARGING_QUIET_MODE_ENABLED);
        private final Uri mStatusUri = Settings.System.getUriFor(
                WIRELESS_CHARGING_QUIET_MODE_STATUS);
        private final Uri mTimeUri = Settings.System.getUriFor(
                WIRELESS_CHARGING_QUIET_MODE_TIME);

        SettingsObserver() {
            super(new Handler());
        }

        void register(ContentResolver cr) {
            cr.registerContentObserver(mEnabledUri, false, this);
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
            String[] times = BatteryFeatureSettingsHelper.getQuietModeTime(getContext()).split(",");
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
        if (KEY_QUIET_MODE_TIME_START.equals(preference.getKey())) {
            writePreferenceClickMetric(preference);
            showDialog(DIALOG_START_TIME);
            return true;
        } else if (KEY_QUIET_MODE_TIME_END.equals(preference.getKey())) {
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
        return R.xml.wireless_charging_quiet_mode;
    }

    private void updateTimeSetting(boolean since, int hour, int minute) {
        String[] times = BatteryFeatureSettingsHelper.getQuietModeTime(getContext()).split(",");
        String nHour = "";
        String nMinute = "";
        if (hour < 10) nHour += "0";
        if (minute < 10) nMinute += "0";
        nHour += String.valueOf(hour);
        nMinute += String.valueOf(minute);
        times[since ? 0 : 1] = nHour + ":" + nMinute;
        BatteryFeatureSettingsHelper.setQuietModeTime(getContext(), times[0] + "," + times[1]);
    }
}
