/*
 * Copyright (C) 2022-2023 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

 package org.nameless.systemui.qs.tiles;

import static com.android.internal.util.nameless.BatteryFeatureSettingsHelper.CHARGING_DISABLED;
import static com.android.internal.util.nameless.BatteryFeatureSettingsHelper.CHARGING_ENABLED;
import static com.android.internal.util.nameless.BatteryFeatureSettingsHelper.QUIET_MODE_ALWAYS;
import static com.android.internal.util.nameless.BatteryFeatureSettingsHelper.QUIET_MODE_DISABLED;
import static com.android.internal.util.nameless.BatteryFeatureSettingsHelper.QUIET_MODE_ENABLED;
import static com.android.internal.util.nameless.BatteryFeatureSettingsHelper.QUIET_MODE_SCHEDULED;

import static org.nameless.provider.SettingsExt.System.WIRELESS_CHARGING_ENABLED;
import static org.nameless.provider.SettingsExt.System.WIRELESS_CHARGING_QUIET_MODE_ENABLED;
import static org.nameless.provider.SettingsExt.System.WIRELESS_CHARGING_QUIET_MODE_STATUS;

import static vendor.nameless.hardware.battery.Feature.WIRELESS_CHARGING_QUIET_MODE;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings.System;
import android.service.quicksettings.Tile;

import androidx.annotation.Nullable;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.util.nameless.BatteryFeatureSettingsHelper;

import com.android.systemui.animation.Expandable;
import com.android.systemui.dagger.qualifiers.Background;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.FalsingManager;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.plugins.qs.QSTile.BooleanState;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.QsEventLogger;
import com.android.systemui.qs.UserSettingObserver;
import com.android.systemui.qs.logging.QSLogger;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.res.R;
import com.android.systemui.util.settings.SystemSettings;

import javax.inject.Inject;

import org.nameless.os.BatteryFeatureManager;

public class QuietModeTile extends QSTileImpl<BooleanState> {

    public static final String TILE_SPEC = "quietmode";

    private final BatteryFeatureManager mBatteryFeatureManager;  
    private final Icon mIcon = ResourceIcon.get(R.drawable.ic_qs_quiet_mode);

    private final UserSettingObserver mEnabledSetting;
    private final UserSettingObserver mStatusSetting;
    private final UserSettingObserver mWirelessChargingSetting;

    @Inject
    public QuietModeTile(
            QSHost host,
            QsEventLogger uiEventLogger,
            @Background Looper backgroundLooper,
            @Main Handler mainHandler,
            FalsingManager falsingManager,
            MetricsLogger metricsLogger,
            StatusBarStateController statusBarStateController,
            ActivityStarter activityStarter,
            QSLogger qsLogger,
            SystemSettings systemSettings) {
        super(host, uiEventLogger, backgroundLooper, mainHandler, falsingManager,
                metricsLogger, statusBarStateController, activityStarter, qsLogger);

        mBatteryFeatureManager = BatteryFeatureManager.getInstance();

        mEnabledSetting = new UserSettingObserver(systemSettings, mHandler,
                WIRELESS_CHARGING_QUIET_MODE_ENABLED, UserHandle.USER_SYSTEM) {
            @Override
            protected void handleValueChanged(int value, boolean observedChange) {
                refreshState();
            }
        };
        mStatusSetting = new UserSettingObserver(systemSettings, mHandler,
                WIRELESS_CHARGING_QUIET_MODE_STATUS, UserHandle.USER_SYSTEM) {
            @Override
            protected void handleValueChanged(int value, boolean observedChange) {
                refreshState();
            }
        };
        mWirelessChargingSetting = new UserSettingObserver(systemSettings, mHandler,
                WIRELESS_CHARGING_ENABLED, UserHandle.USER_SYSTEM, CHARGING_ENABLED) {
            @Override
            protected void handleValueChanged(int value, boolean observedChange) {
                refreshState();
            }
        };
    }

    @Override
    public boolean isAvailable() {
        return mBatteryFeatureManager.hasFeature(WIRELESS_CHARGING_QUIET_MODE);
    }

    @Override
    public BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    protected void handleClick(@Nullable Expandable expandable) {
        if (mState.state == Tile.STATE_UNAVAILABLE) {
            return;
        }
        if (!mState.value) {
            mEnabledSetting.setValue(QUIET_MODE_ENABLED);
        } else {
            if (mStatusSetting.getValue() == QUIET_MODE_ALWAYS) {
                mStatusSetting.setValue(QUIET_MODE_SCHEDULED);
            } else {
                mStatusSetting.setValue(QUIET_MODE_ALWAYS);
                mEnabledSetting.setValue(QUIET_MODE_DISABLED);
            }
        }
        refreshState();
    }

    @Override
    public Intent getLongClickIntent() {
        if (mState.state == Tile.STATE_UNAVAILABLE) {
            return null;
        }
        return new Intent().setComponent(new ComponentName(
                "com.android.settings", "com.android.settings.Settings$QuietModeActivity"));
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_quiet_mode_label);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        if (!isAvailable()) {
            return;
        }

        final boolean enabled = mEnabledSetting.getValue() != 0;
        state.value = enabled;
        state.label = mContext.getString(R.string.quick_settings_quiet_mode_label);
        state.icon = mIcon;
        state.contentDescription = mContext.getString(
                R.string.quick_settings_quiet_mode_label);

        if (mWirelessChargingSetting.getValue() == CHARGING_DISABLED) {
            state.secondaryLabel = mContext.getString(R.string.tile_unavailable);
            state.state = Tile.STATE_UNAVAILABLE;
            return;
        }
        if (enabled) {
            state.secondaryLabel = mStatusSetting.getValue() == QUIET_MODE_ALWAYS ?
                    mContext.getString(R.string.quick_settings_quiet_mode_always) :
                    getTimeLabel();
            state.state = Tile.STATE_ACTIVE;
        } else {
            state.secondaryLabel = mContext.getString(R.string.switch_bar_off);
            state.state = Tile.STATE_INACTIVE;
        }
    }

    @Override
    public int getMetricsCategory() {
        return 0;
    }

    @Override
    public void handleSetListening(boolean listening) {
        super.handleSetListening(listening);
        mEnabledSetting.setListening(listening);
        mStatusSetting.setListening(listening);
        mWirelessChargingSetting.setListening(listening);
    }

    private String getTimeLabel() {
        String[] times = BatteryFeatureSettingsHelper.getQuietModeTime(mContext).split(",");
        return times[0] + " ~ " + times[1];
    }
}
