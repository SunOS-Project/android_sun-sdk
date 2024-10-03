/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.systemui.qs.tiles;

import static com.android.internal.util.nameless.BatteryFeatureSettingsHelper.OPTIMIZED_CHARGE_ALWAYS;
import static com.android.internal.util.nameless.BatteryFeatureSettingsHelper.OPTIMIZED_CHARGE_SCHEDULED;
import static com.android.internal.util.nameless.BatteryFeatureSettingsHelper.OPTIMIZED_CHARGE_SETTINGS_DISABLED;
import static com.android.internal.util.nameless.BatteryFeatureSettingsHelper.OPTIMIZED_CHARGE_SETTINGS_ENABLED;

import static org.nameless.provider.SettingsExt.System.OPTIMIZED_CHARGE_ENABLED;
import static org.nameless.provider.SettingsExt.System.OPTIMIZED_CHARGE_STATUS;

import static vendor.nameless.hardware.battery.V1_0.Feature.SUSPEND_CHARGING;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.service.quicksettings.Tile;

import androidx.annotation.Nullable;

import com.android.internal.util.nameless.BatteryFeatureSettingsHelper;

import com.android.systemui.animation.Expandable;
import com.android.systemui.dagger.qualifiers.Background;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.FalsingManager;
import com.android.systemui.plugins.qs.QSTile.BooleanState;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.QsEventLogger;
import com.android.systemui.qs.UserSettingObserver;
import com.android.systemui.qs.logging.QSLogger;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.res.R;
import com.android.systemui.util.settings.SystemSettings;

import com.android.internal.logging.MetricsLogger;

import javax.inject.Inject;

import org.nameless.os.BatteryFeatureManager;

public class OptimizedChargeTile extends QSTileImpl<BooleanState> {

    public static final String TILE_SPEC = "optimized_charge";

    private final BatteryFeatureManager mBatteryFeatureManager;
    private final Icon mIcon = ResourceIcon.get(R.drawable.ic_optimized_charge);

    private final UserSettingObserver mEnabledSetting;
    private final UserSettingObserver mStatusSetting;

    @Inject
    public OptimizedChargeTile(
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
                OPTIMIZED_CHARGE_ENABLED, UserHandle.USER_SYSTEM) {
            @Override
            protected void handleValueChanged(int value, boolean observedChange) {
                refreshState();
            }
        };
        mStatusSetting = new UserSettingObserver(systemSettings, mHandler,
                OPTIMIZED_CHARGE_STATUS, UserHandle.USER_SYSTEM) {
            @Override
            protected void handleValueChanged(int value, boolean observedChange) {
                refreshState();
            }
        };
    }

    @Override
    public boolean isAvailable() {
        return mBatteryFeatureManager.hasFeature(SUSPEND_CHARGING);
    }

    @Override
    public BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    protected void handleClick(@Nullable Expandable expandable) {
        if (!mState.value) {
            mEnabledSetting.setValue(OPTIMIZED_CHARGE_SETTINGS_ENABLED);
        } else {
            if (mStatusSetting.getValue() == OPTIMIZED_CHARGE_ALWAYS) {
                mStatusSetting.setValue(OPTIMIZED_CHARGE_SCHEDULED);
            } else {
                mStatusSetting.setValue(OPTIMIZED_CHARGE_ALWAYS);
                mEnabledSetting.setValue(OPTIMIZED_CHARGE_SETTINGS_DISABLED);
            }
        }
        refreshState();
    }

    @Override
    public Intent getLongClickIntent() {
        return new Intent().setComponent(new ComponentName(
            "com.android.settings", "com.android.settings.Settings$OptimizedChargeActivity"));
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_optimized_charge_label);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        final boolean enabled = mEnabledSetting.getValue() != 0;
        state.value = enabled;
        state.label = mContext.getString(R.string.quick_settings_optimized_charge_label);
        state.icon = mIcon;
        state.contentDescription =  mContext.getString(
                R.string.quick_settings_optimized_charge_label);
        if (enabled) {
            state.secondaryLabel = mStatusSetting.getValue() == OPTIMIZED_CHARGE_ALWAYS ?
                    mContext.getString(R.string.quick_settings_optimized_charge_always) :
                    getTimeLabel();
            state.state = Tile.STATE_ACTIVE;
        } else {
            state.secondaryLabel = mContext.getString(R.string.switch_bar_off);
            state.state = Tile.STATE_INACTIVE;
        }
    }

    @Override
    public void handleSetListening(boolean listening) {
        super.handleSetListening(listening);
        mEnabledSetting.setListening(listening);
        mStatusSetting.setListening(listening);
    }

    @Override
    public int getMetricsCategory() {
        return 0;
    }

    private String getTimeLabel() {
        String[] times = BatteryFeatureSettingsHelper.getOptimizedChargingTime(mContext).split(",");
        return times[0] + " ~ " + times[1];
    }
}
