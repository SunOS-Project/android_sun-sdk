/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.settings.accessibility;

import static com.android.settings.accessibility.AccessibilityUtil.State.ON;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settingslib.core.AbstractPreferenceController;

public class CustomVibrationPreferenceConfig {

    private static final VibrationAttributes ACCESSIBILITY_VIBRATION_ATTRIBUTES =
            new VibrationAttributes.Builder(
                    VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ACCESSIBILITY))
            .setFlags(VibrationAttributes.FLAG_BYPASS_USER_VIBRATION_INTENSITY_OFF)
            .build();
    public static final VibrationEffect PREVIEW_VIBRATION_EFFECT =
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK);

    protected final ContentResolver mContentResolver;
    protected final Vibrator mVibrator;
    private final String mMainSettingKey;
    private final String mSettingKey;

    public CustomVibrationPreferenceConfig(Context context, String mainSettingKey, String settingKey) {
        mContentResolver = context.getContentResolver();
        mVibrator = context.getSystemService(Vibrator.class);
        mMainSettingKey = mainSettingKey;
        mSettingKey = settingKey;
    }

    /** Returns the main setting key for this setting preference. */
    public String getMainSettingKey() {
        return mMainSettingKey;
    }

    /** Returns the setting key for this setting preference. */
    public String getSettingKey() {
        return mSettingKey;
    }

    /** Returns true if this setting preference is enabled for user update. */
    public boolean isPreferenceEnabled() {
        return Settings.System.getInt(mContentResolver, mMainSettingKey, ON) == ON;
    }

    /** Reads setting value for corresponding {@link CustomVibrationPreferenceConfig} */
    public int readValue(int defaultValue) {
        return Settings.System.getInt(mContentResolver, mSettingKey, defaultValue);
    }

    /** Update setting value for corresponding {@link CustomVibrationPreferenceConfig} */
    public boolean setValue(int value) {
        return Settings.System.putInt(mContentResolver, mSettingKey, value);
    }

    /** Play a vibration effect when switched on. */
    public void playVibrationPreview() {
        mVibrator.vibrate(PREVIEW_VIBRATION_EFFECT, ACCESSIBILITY_VIBRATION_ATTRIBUTES);
    }

    /** {@link ContentObserver} for a setting described by a {@link CustomVibrationPreferenceConfig}. */
    public static final class SettingObserver extends ContentObserver {

        private final Uri mMainSettingUri;
        private final Uri mUri;

        private AbstractPreferenceController mPreferenceController;
        private Preference mPreference;

        /** Creates observer for given preference. */
        public SettingObserver(CustomVibrationPreferenceConfig preferenceConfig) {
            super(new Handler(/* async= */ true));
            mMainSettingUri = Settings.System.getUriFor(preferenceConfig.getMainSettingKey());
            mUri = Settings.System.getUriFor(preferenceConfig.getSettingKey());
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (mUri.equals(uri) || mMainSettingUri.equals(uri)) {
                notifyChange();
            }
        }

        private void notifyChange() {
            if (mPreferenceController != null && mPreference != null) {
                mPreferenceController.updateState(mPreference);
            }
        }

        /**
         * Register this observer to given {@link Context}, to be called from lifecycle
         * {@code onStart} method.
         */
        public void register(Context context) {
            context.getContentResolver().registerContentObserver(
                    mUri, /* notifyForDescendants= */ false, this);
            context.getContentResolver().registerContentObserver(
                    mMainSettingUri, /* notifyForDescendants= */ false, this);
        }

        /**
         * Unregister this observer from given {@link Context}, to be called from lifecycle
         * {@code onStop} method.
         */
        public void unregister(Context context) {
            context.getContentResolver().unregisterContentObserver(this);
        }

        /**
         * Binds this observer to given controller and preference, once it has been displayed to the
         * user.
         */
        public void onDisplayPreference(AbstractPreferenceController controller,
                Preference preference) {
            mPreferenceController = controller;
            mPreference = preference;
        }
    }
}
