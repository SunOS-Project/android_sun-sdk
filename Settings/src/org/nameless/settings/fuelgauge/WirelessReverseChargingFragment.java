/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.settings.fuelgauge;

import static org.nameless.provider.SettingsExt.System.WIRELESS_REVERSE_CHARGING_ENABLED;
import static org.nameless.provider.SettingsExt.System.WIRELESS_REVERSE_CHARGING_SUSPENDED_STATUS;
import static org.nameless.provider.SettingsExt.System.WIRELESS_REVERSE_CHARGING_MIN_LEVEL;

import android.app.settings.SettingsEnums;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;

public class WirelessReverseChargingFragment extends DashboardFragment {

    private static final String TAG = "WirelessReverseChargingFragment";

    private final SettingsObserver mObserver = new SettingsObserver();

    private final class SettingsObserver extends ContentObserver {

        private final Uri mEnabledUri = Settings.System.getUriFor(
                WIRELESS_REVERSE_CHARGING_ENABLED);
        private final Uri mSuspendedUri = Settings.System.getUriFor(
                WIRELESS_REVERSE_CHARGING_SUSPENDED_STATUS);
        private final Uri mLevelUri = Settings.System.getUriFor(
                WIRELESS_REVERSE_CHARGING_MIN_LEVEL);

        SettingsObserver() {
            super(new Handler());
        }

        void register(ContentResolver cr) {
            cr.registerContentObserver(mEnabledUri, false, this);
            cr.registerContentObserver(mSuspendedUri, false, this);
            cr.registerContentObserver(mLevelUri, false, this);
        }

        void unregister(ContentResolver cr) {
            cr.unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            updatePreferenceStates();
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mObserver.register(getContext().getContentResolver());
    }

    @Override
    public void onDestroy() {
        mObserver.unregister(getContext().getContentResolver());
        super.onDestroy();
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.PAGE_UNKNOWN;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.wireless_reverse_charging;
    }
}
