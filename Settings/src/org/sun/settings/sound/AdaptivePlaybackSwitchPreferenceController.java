/*
 * Copyright (C) 2020-2022 Paranoid Android
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sun.settings.sound;

import static org.sun.provider.SettingsExt.System.ADAPTIVE_PLAYBACK_ENABLED;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings;
import android.widget.CompoundButton;

import androidx.preference.PreferenceScreen;

import com.android.settings.widget.SettingsMainSwitchPreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.widget.MainSwitchPreference;

public class AdaptivePlaybackSwitchPreferenceController extends
        SettingsMainSwitchPreferenceController implements LifecycleObserver, OnStart, OnStop {

    private MainSwitchPreference mPreference;
    private final SettingsObserver mSettingsObserver;

    public AdaptivePlaybackSwitchPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mSettingsObserver = new SettingsObserver(new Handler(Looper.getMainLooper()));
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean isChecked() {
        return Settings.System.getIntForUser(mContext.getContentResolver(),
                ADAPTIVE_PLAYBACK_ENABLED, 0, UserHandle.USER_CURRENT) != 0;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        Settings.System.putIntForUser(mContext.getContentResolver(),
                ADAPTIVE_PLAYBACK_ENABLED, isChecked ? 1 : 0,
                UserHandle.USER_CURRENT);
        return true;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void onStart() {
        mSettingsObserver.observe();
    }

    @Override
    public void onStop() {
        mContext.getContentResolver().unregisterContentObserver(mSettingsObserver);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Settings.System.putIntForUser(mContext.getContentResolver(),
                ADAPTIVE_PLAYBACK_ENABLED, isChecked ? 1 : 0,
                UserHandle.USER_CURRENT);
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return NO_RES;
    }

    private final class SettingsObserver extends ContentObserver {

        private final Uri ADAPTIVE_PLAYBACK = Settings.System.getUriFor(
                ADAPTIVE_PLAYBACK_ENABLED);

        public SettingsObserver(Handler handler) {
            super(handler);
        }

        public void observe() {
            mContext.getContentResolver().registerContentObserver(
                    ADAPTIVE_PLAYBACK, false, this, UserHandle.USER_ALL);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (ADAPTIVE_PLAYBACK.equals(uri)) {
                mPreference.setChecked(isChecked());
            }
        }
    }
}
