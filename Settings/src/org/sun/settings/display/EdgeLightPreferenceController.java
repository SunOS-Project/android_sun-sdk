/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.settings.display;

import static org.sun.provider.SettingsExt.System.EDGE_LIGHT_ENABLED;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.TogglePreferenceController;

import com.android.settingslib.PrimarySwitchPreference;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;	

public class EdgeLightPreferenceController extends TogglePreferenceController
        implements LifecycleObserver, OnStart, OnStop {

    private PrimarySwitchPreference mPreference;
    private SettingObserver mSettingObserver;

    public EdgeLightPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        mSettingObserver = new SettingObserver(mPreference);
    }

    @Override
    public boolean isChecked() {
        return Settings.System.getInt(mContext.getContentResolver(),
                EDGE_LIGHT_ENABLED, 0) == 1;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.System.putInt(mContext.getContentResolver(),
                EDGE_LIGHT_ENABLED, isChecked ? 1 : 0);
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

    private class SettingObserver extends ContentObserver {

        private final Uri mUri = Settings.System.getUriFor(EDGE_LIGHT_ENABLED);

        private final Preference mPreference;

        SettingObserver(Preference preference) {
            super(new Handler());
            mPreference = preference;
        }

        public void register(ContentResolver cr) {
            cr.registerContentObserver(mUri, false, this);
        }

        public void unregister(ContentResolver cr) {
            cr.unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange) {
            updateState(mPreference);
        }
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return 0;
    }
}
