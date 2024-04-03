/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.settings.system;

import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.os.Bundle;

import androidx.preference.Preference;

import com.android.internal.util.nameless.PopUpSettingsHelper;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import org.nameless.custom.preference.SystemSettingListPreference;
import org.nameless.custom.preference.SystemSettingSwitchPreference;

public class PopUpViewSettingsFragment extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String KEY_GESTURE = "system_tool_windowing_mode_gesture";
    private static final String KEY_MANAGE_APP = "pop_up_view_manage_apps";
    private static final String KEY_MORE_CIRCLES = "system_tool_more_circles";
    private static final String KEY_KEEP_MUTE = "pop_up_keep_mute_in_mini";
    private static final String KEY_SINGLE_TAP_ACTION = "pop_up_single_tap_action";
    private static final String KEY_DOUBLE_TAP_ACTION = "pop_up_double_tap_action";
    private static final String KEY_NOTIFICATION_PORTRAIT = "pop_up_notification_jump_portrait";
    private static final String KEY_NOTIFICATION_LANDSCAPE = "pop_up_notification_jump_landscape";
    private static final String KEY_NOTIFICATION_BLACKLIST = "pop_up_notification_blacklist";

    private SystemSettingSwitchPreference mGlobalGesture;
    private Preference mManageApps;
    private SystemSettingSwitchPreference mShowMoreCircles;
    private SystemSettingSwitchPreference mKeepMuteInMini;
    private SystemSettingListPreference mSingleTapAction;
    private SystemSettingListPreference mDoubleTapAction;
    private SystemSettingSwitchPreference mNotifPortrait;
    private SystemSettingSwitchPreference mNotifLandscape;
    private Preference mNotifBlacklist;

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        addPreferencesFromResource(R.xml.pop_up_view_settings);

        mGlobalGesture = (SystemSettingSwitchPreference) findPreference(KEY_GESTURE);
        mManageApps = (Preference) findPreference(KEY_MANAGE_APP);
        mShowMoreCircles = (SystemSettingSwitchPreference) findPreference(KEY_MORE_CIRCLES);
        mKeepMuteInMini = (SystemSettingSwitchPreference) findPreference(KEY_KEEP_MUTE);
        mSingleTapAction = (SystemSettingListPreference) findPreference(KEY_SINGLE_TAP_ACTION);
        mDoubleTapAction = (SystemSettingListPreference) findPreference(KEY_DOUBLE_TAP_ACTION);
        mNotifPortrait = (SystemSettingSwitchPreference) findPreference(KEY_NOTIFICATION_PORTRAIT);
        mNotifLandscape = (SystemSettingSwitchPreference) findPreference(KEY_NOTIFICATION_LANDSCAPE);
        mNotifBlacklist = (Preference) findPreference(KEY_NOTIFICATION_BLACKLIST);

        mGlobalGesture.setChecked(PopUpSettingsHelper.isGestureEnabled(getContext()));

        mManageApps.setOnPreferenceClickListener(p -> {
            final Intent intent = new Intent();
            intent.setClassName("org.nameless.systemtool",
                    "org.nameless.systemtool.windowmode.ManageAppsActivity");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_NO_HISTORY |
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

            try {
                getContext().startActivity(intent);
            } catch (Exception ignored) {}

            return true;
        });

        mShowMoreCircles.setChecked(PopUpSettingsHelper.shouldShowMoreCircles(getContext()));

        mKeepMuteInMini.setChecked(PopUpSettingsHelper.isKeepMuteInMiniEnabled(getContext()));

        final int singleTapAction = PopUpSettingsHelper.getSingleTapAction(getContext());
        mSingleTapAction.setValueIndex(singleTapAction);
        mSingleTapAction.setSummary(mSingleTapAction.getEntries()[singleTapAction]);
        mSingleTapAction.setOnPreferenceChangeListener(this);

        final int doubleTapAction = PopUpSettingsHelper.getDoubleTapAction(getContext());
        mDoubleTapAction.setValueIndex(doubleTapAction);
        mDoubleTapAction.setSummary(mDoubleTapAction.getEntries()[doubleTapAction]);
        mDoubleTapAction.setOnPreferenceChangeListener(this);

        mNotifPortrait.setChecked(PopUpSettingsHelper.isNotificationJumpEnabled(getContext(), false));
        mNotifPortrait.setOnPreferenceChangeListener(this);

        mNotifLandscape.setChecked(PopUpSettingsHelper.isNotificationJumpEnabled(getContext(), true));
        mNotifLandscape.setOnPreferenceChangeListener(this);

        mNotifBlacklist.setEnabled(mNotifPortrait.isChecked() || mNotifLandscape.isChecked());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mSingleTapAction) {
            mSingleTapAction.setSummary(mSingleTapAction.getEntries()[Integer.parseInt((String) newValue)]);
        } else if (preference == mDoubleTapAction) {
            mDoubleTapAction.setSummary(mDoubleTapAction.getEntries()[Integer.parseInt((String) newValue)]);
        } else if (preference == mNotifPortrait) {
            mNotifBlacklist.setEnabled((Boolean) newValue || mNotifLandscape.isChecked());
        } else if (preference == mNotifLandscape) {
            mNotifBlacklist.setEnabled(mNotifPortrait.isChecked() || (Boolean) newValue);
        }
        return true;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.PAGE_UNKNOWN;
    }
}
