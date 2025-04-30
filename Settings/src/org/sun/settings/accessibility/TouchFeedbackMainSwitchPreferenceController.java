/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.settings.accessibility;

import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemClock;
import android.os.VibrationAttributes;
import android.os.Vibrator;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.accessibility.VibrationPreferenceConfig;
import com.android.settings.widget.SettingsMainSwitchPreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

/**
 * Preference controller for the main switch setting for vibration and haptics screen.
 *
 * <p>This preference is controlled by the setting key{@link Settings.System#VIBRATE_ON}, and it
 * will disable the entire settings screen once the settings is turned OFF. All device haptics will
 * be disabled by this setting, except the flagged alerts and accessibility touch feedback.
 */
public class TouchFeedbackMainSwitchPreferenceController extends SettingsMainSwitchPreferenceController
        implements LifecycleObserver, OnStart, OnStop {

    private final ContentObserver mSettingObserver;
    private final Vibrator mVibrator;

    private long mLastSetCheckedTime = 0L;

    public TouchFeedbackMainSwitchPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mVibrator = context.getSystemService(Vibrator.class);
        mSettingObserver = new ContentObserver(new Handler(/* async= */ true)) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                updateState(mSwitchPreference);
            }
        };
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void onStart() {
        mContext.getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.HAPTIC_FEEDBACK_ENABLED),
                /* notifyForDescendants= */ false,
                mSettingObserver);
    }

    @Override
    public void onStop() {
        mContext.getContentResolver().unregisterContentObserver(mSettingObserver);
    }

    @Override
    public boolean isChecked() {
        return Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.HAPTIC_FEEDBACK_ENABLED, ON) == ON;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        boolean success = Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.HAPTIC_FEEDBACK_ENABLED,
                isChecked ? ON : OFF);

        // Hack: setChecked is called twice on user-click, causing double haptic feedback.
        // Add 50ms interval requirement here.
        final long now = SystemClock.uptimeMillis();
        if (success && isChecked && now - mLastSetCheckedTime >= 50L) {
            // Play a haptic as preview for the main toggle only when touch feedback is enabled.
            VibrationPreferenceConfig.playVibrationPreview(
                    mVibrator, VibrationAttributes.USAGE_TOUCH);
        }
        mLastSetCheckedTime = now;

        return success;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_accessibility;
    }
}
