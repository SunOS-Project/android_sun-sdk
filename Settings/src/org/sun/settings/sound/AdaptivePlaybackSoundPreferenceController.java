/*
 * Copyright (C) 2020-2021 Paranoid Android
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
import static org.sun.provider.SettingsExt.System.ADAPTIVE_PLAYBACK_TIMEOUT;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

public class AdaptivePlaybackSoundPreferenceController extends BasePreferenceController
        implements SelectorWithWidgetPreference.OnClickListener, LifecycleObserver, OnStart, OnStop {

    private static final String KEY_NO_TIMEOUT = "adaptive_playback_timeout_none";
    private static final String KEY_30_SECS = "adaptive_playback_timeout_30_secs";
    private static final String KEY_1_MIN = "adaptive_playback_timeout_1_min";
    private static final String KEY_2_MIN = "adaptive_playback_timeout_2_min";
    private static final String KEY_5_MIN = "adaptive_playback_timeout_5_min";
    private static final String KEY_10_MIN = "adaptive_playback_timeout_10_min";

    static final int ADAPTIVE_PLAYBACK_TIMEOUT_NONE = 0;
    static final int ADAPTIVE_PLAYBACK_TIMEOUT_30_SECS = 30000;
    static final int ADAPTIVE_PLAYBACK_TIMEOUT_1_MIN = 60000;
    static final int ADAPTIVE_PLAYBACK_TIMEOUT_2_MIN = 120000;
    static final int ADAPTIVE_PLAYBACK_TIMEOUT_5_MIN = 300000;
    static final int ADAPTIVE_PLAYBACK_TIMEOUT_10_MIN = 600000;

    private boolean mAdaptivePlaybackEnabled;
    private int mAdaptivePlaybackTimeout;

    private PreferenceCategory mPreferenceCategory;
    private SelectorWithWidgetPreference mTimeoutNonePref;
    private SelectorWithWidgetPreference mTimeout30SecPref;
    private SelectorWithWidgetPreference mTimeout1MinPref;
    private SelectorWithWidgetPreference mTimeout2MinPref;
    private SelectorWithWidgetPreference mTimeout5MinPref;
    private SelectorWithWidgetPreference mTimeout10MinPref;

    private final SettingObserver mSettingObserver;

    public AdaptivePlaybackSoundPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);

        mSettingObserver = new SettingObserver(new Handler(Looper.getMainLooper()));
        mAdaptivePlaybackEnabled = Settings.System.getIntForUser(mContext.getContentResolver(),
                ADAPTIVE_PLAYBACK_ENABLED, 0, UserHandle.USER_CURRENT) != 0;
        mAdaptivePlaybackTimeout = Settings.System.getIntForUser(mContext.getContentResolver(),
                ADAPTIVE_PLAYBACK_TIMEOUT, ADAPTIVE_PLAYBACK_TIMEOUT_30_SECS,
                UserHandle.USER_CURRENT);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mPreferenceCategory = screen.findPreference(getPreferenceKey());
        mTimeoutNonePref = makeSelectorPreference(KEY_NO_TIMEOUT,
                R.string.adaptive_playback_timeout_none);
        mTimeout30SecPref = makeSelectorPreference(KEY_30_SECS,
                R.string.adaptive_playback_timeout_30_secs);
        mTimeout1MinPref = makeSelectorPreference(KEY_1_MIN, R.string.adaptive_playback_timeout_1_min);
        mTimeout2MinPref = makeSelectorPreference(KEY_2_MIN, R.string.adaptive_playback_timeout_2_min);
        mTimeout5MinPref = makeSelectorPreference(KEY_5_MIN, R.string.adaptive_playback_timeout_5_min);
        mTimeout10MinPref = makeSelectorPreference(KEY_10_MIN,
                R.string.adaptive_playback_timeout_10_min);
        updateState(null);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE_UNSEARCHABLE;
    }

    @Override
    public void onRadioButtonClicked(SelectorWithWidgetPreference preference) {
        final int adaptivePlaybackTimeout = keyToSetting(preference.getKey());
        if (adaptivePlaybackTimeout != Settings.System.getIntForUser(
                mContext.getContentResolver(),
                ADAPTIVE_PLAYBACK_TIMEOUT, ADAPTIVE_PLAYBACK_TIMEOUT_30_SECS,
                UserHandle.USER_CURRENT)) {
            Settings.System.putIntForUser(mContext.getContentResolver(),
                    ADAPTIVE_PLAYBACK_TIMEOUT, adaptivePlaybackTimeout,
                    UserHandle.USER_CURRENT);
        }
    }

    @Override
    public void updateState(Preference preference) {
        final boolean isTimeoutNone = mAdaptivePlaybackEnabled
                && mAdaptivePlaybackTimeout == ADAPTIVE_PLAYBACK_TIMEOUT_NONE;
        final boolean isTimeout30Sec = mAdaptivePlaybackEnabled
                && mAdaptivePlaybackTimeout == ADAPTIVE_PLAYBACK_TIMEOUT_30_SECS;
        final boolean isTimeout1Min = mAdaptivePlaybackEnabled
                && mAdaptivePlaybackTimeout == ADAPTIVE_PLAYBACK_TIMEOUT_1_MIN;
        final boolean isTimeout2Min = mAdaptivePlaybackEnabled
                && mAdaptivePlaybackTimeout == ADAPTIVE_PLAYBACK_TIMEOUT_2_MIN;
        final boolean isTimeout5Min = mAdaptivePlaybackEnabled
                && mAdaptivePlaybackTimeout == ADAPTIVE_PLAYBACK_TIMEOUT_5_MIN;
        final boolean isTimeout10Min = mAdaptivePlaybackEnabled
                && mAdaptivePlaybackTimeout == ADAPTIVE_PLAYBACK_TIMEOUT_10_MIN;
        if (mTimeoutNonePref != null && mTimeoutNonePref.isChecked() != isTimeoutNone) {
            mTimeoutNonePref.setChecked(isTimeoutNone);
        }
        if (mTimeout30SecPref != null && mTimeout30SecPref.isChecked() != isTimeout30Sec) {
            mTimeout30SecPref.setChecked(isTimeout30Sec);
        }
        if (mTimeout1MinPref != null && mTimeout1MinPref.isChecked() != isTimeout1Min) {
            mTimeout1MinPref.setChecked(isTimeout1Min);
        }
        if (mTimeout2MinPref != null && mTimeout2MinPref.isChecked() != isTimeout2Min) {
            mTimeout2MinPref.setChecked(isTimeout2Min);
        }
        if (mTimeout5MinPref != null && mTimeout5MinPref.isChecked() != isTimeout5Min) {
            mTimeout5MinPref.setChecked(isTimeout5Min);
        }
        if (mTimeout10MinPref != null && mTimeout10MinPref.isChecked() != isTimeout10Min) {
            mTimeout10MinPref.setChecked(isTimeout10Min);
        }

        mPreferenceCategory.setEnabled(mAdaptivePlaybackEnabled);
        mTimeoutNonePref.setEnabled(mAdaptivePlaybackEnabled);
        mTimeout30SecPref.setEnabled(mAdaptivePlaybackEnabled);
        mTimeout1MinPref.setEnabled(mAdaptivePlaybackEnabled);
        mTimeout2MinPref.setEnabled(mAdaptivePlaybackEnabled);
        mTimeout5MinPref.setEnabled(mAdaptivePlaybackEnabled);
        mTimeout10MinPref.setEnabled(mAdaptivePlaybackEnabled);
    }

    @Override
    public void onStart() {
        mSettingObserver.observe();
    }

    @Override
    public void onStop() {
        mContext.getContentResolver().unregisterContentObserver(mSettingObserver);
    }

    private static int keyToSetting(String key) {
        switch (key) {
            case KEY_NO_TIMEOUT:
                return ADAPTIVE_PLAYBACK_TIMEOUT_NONE;
            case KEY_1_MIN:
                return ADAPTIVE_PLAYBACK_TIMEOUT_1_MIN;
            case KEY_2_MIN:
                return ADAPTIVE_PLAYBACK_TIMEOUT_2_MIN;
            case KEY_5_MIN:
                return ADAPTIVE_PLAYBACK_TIMEOUT_5_MIN;
            case KEY_10_MIN:
                return ADAPTIVE_PLAYBACK_TIMEOUT_10_MIN;
            default:
                return ADAPTIVE_PLAYBACK_TIMEOUT_30_SECS;
        }
    }

    private SelectorWithWidgetPreference makeSelectorPreference(String key, int titleId) {
        final SelectorWithWidgetPreference pref = new SelectorWithWidgetPreference(mPreferenceCategory.getContext());
        pref.setKey(key);
        pref.setTitle(titleId);
        pref.setOnClickListener(this);
        mPreferenceCategory.addPreference(pref);
        return pref;
    }

    private final class SettingObserver extends ContentObserver {

        private final Uri ADAPTIVE_PLAYBACK_URI = Settings.System.getUriFor(
                ADAPTIVE_PLAYBACK_ENABLED);
        private final Uri ADAPTIVE_PLAYBACK_TIMEOUT_URI = Settings.System.getUriFor(
                ADAPTIVE_PLAYBACK_TIMEOUT);

        public SettingObserver(Handler handler) {
            super(handler);
        }

        public void observe() {
            final ContentResolver cr = mContext.getContentResolver();
            cr.registerContentObserver(ADAPTIVE_PLAYBACK_URI, false, this, UserHandle.USER_ALL);
            cr.registerContentObserver(ADAPTIVE_PLAYBACK_TIMEOUT_URI, false, this, UserHandle.USER_ALL);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (ADAPTIVE_PLAYBACK_URI.equals(uri) || ADAPTIVE_PLAYBACK_TIMEOUT_URI.equals(uri)) {
                mAdaptivePlaybackEnabled = Settings.System.getIntForUser(
                        mContext.getContentResolver(),
                        ADAPTIVE_PLAYBACK_ENABLED, 0,
                        UserHandle.USER_CURRENT) != 0;
                mAdaptivePlaybackTimeout = Settings.System.getIntForUser(
                        mContext.getContentResolver(),
                        ADAPTIVE_PLAYBACK_TIMEOUT, ADAPTIVE_PLAYBACK_TIMEOUT_30_SECS,
                        UserHandle.USER_CURRENT);
                updateState(null);
            }
        }
    }
}
