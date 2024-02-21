/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.settings.gestures;

import static org.nameless.provider.SettingsExt.System.TOUCH_GESTURE_M;
import static org.nameless.provider.SettingsExt.System.TOUCH_GESTURE_MUSIC_CONTROL;
import static org.nameless.provider.SettingsExt.System.TOUCH_GESTURE_O;
import static org.nameless.provider.SettingsExt.System.TOUCH_GESTURE_S;
import static org.nameless.provider.SettingsExt.System.TOUCH_GESTURE_SINGLE_TAP_SHOW_AMBIENT;
import static org.nameless.provider.SettingsExt.System.TOUCH_GESTURE_V;
import static org.nameless.provider.SettingsExt.System.TOUCH_GESTURE_W;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import org.nameless.custom.preference.SwitchPreference;
import org.nameless.hardware.TouchGestureManager;

public class OffScreenGesturesFragment extends SettingsPreferenceFragment {

    private final class GestureSwitchPreference extends SwitchPreference {

        GestureSwitchPreference(Context context, String settings, boolean defaultOn,
                int titleResId, int summaryResId) {
            super(context);
            setTitle(titleResId);
            setSummary(summaryResId);
            setChecked(isSettingsOn(settings, defaultOn));
            setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    putSettings(settings, (Boolean) newValue ? 1 : 0);
                    return true;
                }
            });
        }
    }

    private final class GestureListPreference extends ListPreference {

        GestureListPreference(Context context, String settings, int titleResId) {
            super(context);
            setTitle(titleResId);
            setDialogTitle(titleResId);
            setEntries(R.array.off_screen_gesture_actions_entries);
            setEntryValues(R.array.off_screen_gesture_actions_values);
            setValueIndex(findIndexOfValue(String.valueOf(getSettings(settings, 0))));
            setSummary(getEntry());
            setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    final int idx = Integer.parseInt(newValue.toString());
                    putSettings(settings, Integer.parseInt(newValue.toString()));
                    setSummary(getEntries()[findIndexOfValue(newValue.toString())]);
                    return true;
                }
            });
        }
    }

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        addPreferencesFromResource(R.xml.off_screen_gestures);
    }

    @Override
    public void onResume() {
        super.onResume();

        final PreferenceScreen screen = getPreferenceScreen();
        screen.removeAll();

        final Context prefContext = getPrefContext();

        // Single tap
        if (TouchGestureManager.isSingleTapSupported()) {
            final GestureSwitchPreference singleTapPref = new GestureSwitchPreference(prefContext,
                    TOUCH_GESTURE_SINGLE_TAP_SHOW_AMBIENT, true,
                    R.string.off_screen_gesture_single_tap_pulse_title,
                    R.string.off_screen_gesture_single_tap_pulse_summary);
            screen.addPreference(singleTapPref);
        }

        // Music control
        if (TouchGestureManager.isMusicControlSupported()) {
            final GestureSwitchPreference musicControlPref = new GestureSwitchPreference(prefContext,
                    TOUCH_GESTURE_MUSIC_CONTROL, false,
                    R.string.off_screen_gesture_music_control_title,
                    R.string.off_screen_gesture_music_control_summary);
            screen.addPreference(musicControlPref);
        }

        // Draw M
        if (TouchGestureManager.isDrawMSupported()) {
            final GestureListPreference drawMPref = new GestureListPreference(prefContext,
                    TOUCH_GESTURE_M, R.string.off_screen_gesture_draw_m_title);
            screen.addPreference(drawMPref);
        }

        // Draw O
        if (TouchGestureManager.isDrawOSupported()) {
            final GestureListPreference drawOPref = new GestureListPreference(prefContext,
                    TOUCH_GESTURE_O, R.string.off_screen_gesture_draw_o_title);
            screen.addPreference(drawOPref);
        }

        // Draw S
        if (TouchGestureManager.isDrawSSupported()) {
            final GestureListPreference drawSPref = new GestureListPreference(prefContext,
                    TOUCH_GESTURE_S, R.string.off_screen_gesture_draw_s_title);
            screen.addPreference(drawSPref);
        }

        // Draw V
        if (TouchGestureManager.isDrawVSupported()) {
            final GestureListPreference drawVPref = new GestureListPreference(prefContext,
                    TOUCH_GESTURE_V, R.string.off_screen_gesture_draw_v_title);
            screen.addPreference(drawVPref);
        }

        // Draw W
        if (TouchGestureManager.isDrawWSupported()) {
            final GestureListPreference drawWPref = new GestureListPreference(prefContext,
                    TOUCH_GESTURE_W, R.string.off_screen_gesture_draw_w_title);
            screen.addPreference(drawWPref);
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.PAGE_UNKNOWN;
    }

    private int getSettings(String settings, int def) {
        return Settings.System.getIntForUser(getContext().getContentResolver(),
                settings, def, UserHandle.USER_CURRENT);
    }

    private boolean isSettingsOn(String settings, boolean def) {
        return getSettings(settings, def ? 1 : 0) == 1;
    }

    private void putSettings(String settings, int val) {
        Settings.System.putIntForUser(getContext().getContentResolver(),
                settings, val, UserHandle.USER_CURRENT);
    }
}
