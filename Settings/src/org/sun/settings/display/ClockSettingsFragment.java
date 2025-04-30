/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.settings.display;

import static org.sun.provider.SettingsExt.System.STATUSBAR_CLOCK;
import static org.sun.provider.SettingsExt.System.STATUSBAR_CLOCK_AM_PM_STYLE;
import static org.sun.provider.SettingsExt.System.STATUSBAR_CLOCK_AUTO_HIDE;
import static org.sun.provider.SettingsExt.System.STATUSBAR_CLOCK_AUTO_HIDE_HDURATION;
import static org.sun.provider.SettingsExt.System.STATUSBAR_CLOCK_AUTO_HIDE_SDURATION;
import static org.sun.provider.SettingsExt.System.STATUSBAR_CLOCK_DATE_DISPLAY;
import static org.sun.provider.SettingsExt.System.STATUSBAR_CLOCK_DATE_FORMAT;
import static org.sun.provider.SettingsExt.System.STATUSBAR_CLOCK_DATE_POSITION;
import static org.sun.provider.SettingsExt.System.STATUSBAR_CLOCK_DATE_STYLE;
import static org.sun.provider.SettingsExt.System.STATUSBAR_CLOCK_SECONDS;
import static org.sun.provider.SettingsExt.System.STATUSBAR_CLOCK_STYLE;

import android.app.AlertDialog;
import android.app.settings.SettingsEnums;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.widget.EditText;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.Date;

public class ClockSettingsFragment extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final int AM_PM_STYLE_GONE = 2;

    private static final int CLOCK_DATE_DISPLAY_GONE = 0;

    private static final int CLOCK_DATE_STYLE_NORMAL = 0;
    private static final int CLOCK_DATE_STYLE_LOWERCASE = 1;
    private static final int CLOCK_DATE_STYLE_UPPERCASE = 2;

    private static final int CLOCK_STYLE_LEFT = 0;
    private static final int CLOCK_STYLE_CENTER = 1;

    private static final int DEFAULT_CLOCK_DATE_FORMAT_INDEX = 9;
    private static final int CUSTOM_CLOCK_DATE_FORMAT_INDEX = 18;

    private ListPreference mStatusBarClockStyle;
    private ListPreference mStatusBarAmPm;
    private ListPreference mClockDateDisplay;
    private ListPreference mClockDateStyle;
    private ListPreference mClockDateFormat;
    private ListPreference mClockDatePosition;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.statusbar_clock);

        final ContentResolver resolver = getActivity().getContentResolver();
        final Resources res = getActivity().getResources();

        mStatusBarClockStyle = (ListPreference) findPreference(STATUSBAR_CLOCK_STYLE);
        mStatusBarAmPm = (ListPreference) findPreference(STATUSBAR_CLOCK_AM_PM_STYLE);
        mClockDateDisplay = (ListPreference) findPreference(STATUSBAR_CLOCK_DATE_DISPLAY);
        mClockDateStyle = (ListPreference) findPreference(STATUSBAR_CLOCK_DATE_STYLE);
        mClockDateFormat = (ListPreference) findPreference(STATUSBAR_CLOCK_DATE_FORMAT);
        mClockDatePosition = (ListPreference) findPreference(STATUSBAR_CLOCK_DATE_POSITION);

        final boolean hasCenteredCutout = res.getBoolean(
                com.android.internal.R.bool.config_hasCenteredCutout);
        final CharSequence[] entries;
        final CharSequence[] entryValues;
        if (hasCenteredCutout) {
            entries = res.getTextArray(R.array.status_bar_clock_style_centered_cutout_entries);
            entryValues = res.getTextArray(R.array.status_bar_clock_style_centered_cutout_values);
        } else {
            entries = res.getTextArray(R.array.status_bar_clock_style_entries);
            entryValues = res.getTextArray(R.array.status_bar_clock_style_values);
        }
        mStatusBarClockStyle.setEntries(entries);
        mStatusBarClockStyle.setEntryValues(entryValues);
        int style = Settings.System.getIntForUser(resolver,
                STATUSBAR_CLOCK_STYLE, CLOCK_STYLE_LEFT,
                UserHandle.USER_CURRENT);
        if (style == CLOCK_STYLE_CENTER && hasCenteredCutout) {
            style = CLOCK_STYLE_LEFT;
        }
        mStatusBarClockStyle.setValue(String.valueOf(style));
        mStatusBarClockStyle.setSummary(mStatusBarClockStyle.getEntry());
        mStatusBarClockStyle.setOnPreferenceChangeListener(this);

        if (DateFormat.is24HourFormat(getActivity())) {
            mStatusBarAmPm.setEnabled(false);
            mStatusBarAmPm.setSummary(R.string.status_bar_am_pm_info);
        }

        mClockDateDisplay.setOnPreferenceChangeListener(this);

        mClockDateStyle.setOnPreferenceChangeListener(this);

        String clockFormat = Settings.System.getStringForUser(resolver,
                STATUSBAR_CLOCK_DATE_FORMAT, UserHandle.USER_CURRENT);
        if (TextUtils.isEmpty(clockFormat)) {
            clockFormat = "EEE";
        }
        final int index = mClockDateFormat.findIndexOfValue((String) clockFormat);
        if (index == -1 || index == CUSTOM_CLOCK_DATE_FORMAT_INDEX) {
            mClockDateFormat.setValueIndex(CUSTOM_CLOCK_DATE_FORMAT_INDEX);
        } else {
            mClockDateFormat.setValue(clockFormat);
        }
        mClockDateFormat.setSummary(clockFormat);
        mClockDateFormat.setOnPreferenceChangeListener(this);

        parseClockDateFormats(Settings.System.getIntForUser(resolver,
                STATUSBAR_CLOCK_DATE_STYLE, CLOCK_DATE_STYLE_NORMAL,
                UserHandle.USER_CURRENT));
        setDateOptions(Settings.System.getIntForUser(resolver,
                STATUSBAR_CLOCK_DATE_DISPLAY, CLOCK_DATE_DISPLAY_GONE,
                UserHandle.USER_CURRENT) != CLOCK_DATE_DISPLAY_GONE);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mStatusBarClockStyle) {
            final int style = Integer.parseInt((String) newValue);
            final int idx = mStatusBarClockStyle.findIndexOfValue((String) newValue);
            Settings.System.putIntForUser(getActivity().getContentResolver(),
                    STATUSBAR_CLOCK_STYLE, style, UserHandle.USER_CURRENT);
            mStatusBarClockStyle.setSummary(mStatusBarClockStyle.getEntries()[idx]);
            return true;
        }
        if (preference == mClockDateDisplay) {
            setDateOptions(Integer.parseInt((String) newValue) != CLOCK_DATE_DISPLAY_GONE);
            return true;
        }
        if (preference == mClockDateStyle) {
            parseClockDateFormats(Integer.parseInt((String) newValue));
            return true;
        }
        if (preference == mClockDateFormat) {
            final int index = mClockDateFormat.findIndexOfValue((String) newValue);
            if (index == CUSTOM_CLOCK_DATE_FORMAT_INDEX) {
                final EditText input = new EditText(getActivity());
                final String oldText = Settings.System.getStringForUser(
                        getActivity().getContentResolver(),
                        STATUSBAR_CLOCK_DATE_FORMAT,
                        UserHandle.USER_CURRENT);
                if (oldText != null) {
                    input.setText(oldText);
                }

                new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.clock_date_string_edittext_title)
                    .setMessage(R.string.clock_date_string_edittext_summary)
                    .setView(input)
                    .setPositiveButton(R.string.menu_save, (dialog, which) -> {
                        String clockFormat = input.getText().toString();
                        if (TextUtils.isEmpty(clockFormat)) {
                            clockFormat = "EEE";
                        }
                        Settings.System.putStringForUser(getActivity().getContentResolver(),
                                STATUSBAR_CLOCK_DATE_FORMAT, clockFormat,
                                UserHandle.USER_CURRENT);
                        mClockDateFormat.setSummary(clockFormat);
                    })
                    .setNegativeButton(R.string.menu_cancel, (dialog, which) -> {
                        String clockFormat = Settings.System.getStringForUser(
                                getActivity().getContentResolver(),
                                STATUSBAR_CLOCK_DATE_FORMAT, UserHandle.USER_CURRENT);
                        if (TextUtils.isEmpty(clockFormat)) {
                            clockFormat = "EEE";
                        }
                        mClockDateFormat.setSummary(clockFormat);
                    })
                    .create()
                    .show();
            } else {
                Settings.System.putStringForUser(getActivity().getContentResolver(),
                        STATUSBAR_CLOCK_DATE_FORMAT, (String) newValue,
                        UserHandle.USER_CURRENT);
            }
            mClockDateFormat.setSummary((String) newValue);
            return true;
        }
        return false;
    }

    private void parseClockDateFormats(int dateStyle) {
        final String[] dateEntries = getResources().getStringArray(R.array.clock_date_format_entries_values);
        final CharSequence parsedDateEntries[] = new CharSequence[dateEntries.length];
        for (int i = 0; i < dateEntries.length; i++) {
            if (i == dateEntries.length - 1) {
                parsedDateEntries[i] = dateEntries[i];
            } else {
                final CharSequence dateString = DateFormat.format(dateEntries[i], new Date());
                final String newDate;
                if (dateStyle == CLOCK_DATE_STYLE_LOWERCASE) {
                    newDate = dateString.toString().toLowerCase();
                } else if (dateStyle == CLOCK_DATE_STYLE_UPPERCASE) {
                    newDate = dateString.toString().toUpperCase();
                } else {
                    newDate = dateString.toString();
                }
                parsedDateEntries[i] = newDate;
            }
        }
        mClockDateFormat.setEntries(parsedDateEntries);
    }

    private void setDateOptions(boolean showDate) {
        mClockDateStyle.setEnabled(showDate);
        mClockDateFormat.setEnabled(showDate);
        mClockDatePosition.setEnabled(showDate);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.PAGE_UNKNOWN;
    }
}
