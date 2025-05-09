/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.settings.display;

import static org.sun.provider.SettingsExt.System.UNLIMIT_EDGE_TOUCH_MODE;

import static vendor.sun.hardware.displayfeature.Feature.EDGE_TOUCH;

import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import org.sun.custom.preference.SwitchPreferenceCompat;
import org.sun.display.DisplayFeatureManager;

public class EdgeTouchPreferenceController extends TogglePreferenceController
        implements LifecycleObserver, OnStart, OnStop {

    private final DisplayFeatureManager mManager;

    private SwitchPreferenceCompat mPreference;
    private SettingObserver mSettingObserver;

    public EdgeTouchPreferenceController(Context context, String key) {
        super(context, key);
        mManager = DisplayFeatureManager.getInstance();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        mSettingObserver = new SettingObserver(mPreference);
    }

    @Override
    public int getAvailabilityStatus() {
        return mManager.hasFeature(EDGE_TOUCH) ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isChecked() {
        return Settings.System.getIntForUser(mContext.getContentResolver(),
                UNLIMIT_EDGE_TOUCH_MODE, 0, UserHandle.USER_SYSTEM) == 1;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (!isChecked) {
            Settings.System.putIntForUser(mContext.getContentResolver(),
                    UNLIMIT_EDGE_TOUCH_MODE, 0, UserHandle.USER_SYSTEM);
            return true;
        }

        new AlertDialog.Builder(mContext)
                .setTitle(mContext.getText(R.string.confirm_before_enable_title))
                .setMessage(mContext.getText(R.string.unlimit_edge_touch_warning))
                .setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Settings.System.putIntForUser(mContext.getContentResolver(),
                                UNLIMIT_EDGE_TOUCH_MODE, isChecked ? 1 : 0, UserHandle.USER_SYSTEM);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
        return isChecked();
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
        private final Uri mUri = Settings.System.getUriFor(UNLIMIT_EDGE_TOUCH_MODE);

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
