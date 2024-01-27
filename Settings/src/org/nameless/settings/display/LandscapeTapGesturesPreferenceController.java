/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.settings.display;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;

import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import org.nameless.custom.preference.SystemSettingSwitchPreference;
import org.nameless.provider.SettingsExt;

public class LandscapeTapGesturesPreferenceController extends BasePreferenceController
        implements LifecycleObserver, OnStart, OnStop {

    private SettingObserver mSettingObserver;
    private SystemSettingSwitchPreference mPreference;

    public LandscapeTapGesturesPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        mSettingObserver = new SettingObserver();
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void onStart() {
        if (mSettingObserver != null) {
            mSettingObserver.register(mContext.getContentResolver());
            mSettingObserver.onChange(false);
        }
    }

    @Override
    public void onStop() {
        if (mSettingObserver != null) {
            mSettingObserver.unregister(mContext.getContentResolver());
        }
    }

    private void updateEnabledState() {
        final boolean doubleTapSleepStatusbar = Settings.System.getIntForUser(
                mContext.getContentResolver(),
                SettingsExt.System.DOUBLE_TAP_SLEEP_STATUSBAR, 1,
                UserHandle.USER_CURRENT) == 1;
        final boolean brightnessControl = Settings.System.getIntForUser(
                mContext.getContentResolver(),
                SettingsExt.System.STATUSBAR_BRIGHTNESS_CONTROL, 0,
                UserHandle.USER_CURRENT) == 1;
        mPreference.setEnabled(doubleTapSleepStatusbar || brightnessControl);
    }

    private class SettingObserver extends ContentObserver {

        private final Uri mDoubleTapGestureUri = Settings.System.getUriFor(
                SettingsExt.System.DOUBLE_TAP_SLEEP_STATUSBAR);
        private final Uri mBrightnessControlUri = Settings.System.getUriFor(
                SettingsExt.System.STATUSBAR_BRIGHTNESS_CONTROL);

        SettingObserver() {
            super(new Handler());
        }

        public void register(ContentResolver cr) {
            cr.registerContentObserver(mDoubleTapGestureUri, false, this);
            cr.registerContentObserver(mBrightnessControlUri, false, this);
        }

        public void unregister(ContentResolver cr) {
            cr.unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange) {
            updateEnabledState();
        }
    }
}
