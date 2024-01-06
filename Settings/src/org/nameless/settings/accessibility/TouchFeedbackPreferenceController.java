/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.settings.accessibility;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

public class TouchFeedbackPreferenceController extends BasePreferenceController
        implements LifecycleObserver, OnStart, OnStop {

    private Preference mPreference;
    private SettingObserver mSettingObserver;

    public TouchFeedbackPreferenceController(Context context, String preferenceKey) {
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
            updatePreference();
        }
    }

    @Override
    public void onStop() {
        if (mSettingObserver != null) {
            mSettingObserver.unregister(mContext.getContentResolver());
        }
    }

    private void updatePreference() {
        final boolean vibrateOn = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.VIBRATE_ON, 1) == 1;
        final boolean touchFeedback = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) == 1;
        mPreference.setEnabled(vibrateOn);
        mPreference.setSummary(vibrateOn && touchFeedback
                ? R.string.accessibility_vibration_settings_state_on
                : R.string.accessibility_vibration_settings_state_off);
    }

    private final class SettingObserver extends ContentObserver {
        private final Uri mMainSettingUri = Settings.System.getUriFor(Settings.System.VIBRATE_ON);
        private final Uri mTouchFeedbackUri = Settings.System.getUriFor(Settings.System.HAPTIC_FEEDBACK_ENABLED);

        SettingObserver() {
            super(new Handler());
        }

        public void register(ContentResolver cr) {
            cr.registerContentObserver(mMainSettingUri, false, this);
            cr.registerContentObserver(mTouchFeedbackUri, false, this);
        }

        public void unregister(ContentResolver cr) {
            cr.unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            updatePreference();
        }
    }
}
