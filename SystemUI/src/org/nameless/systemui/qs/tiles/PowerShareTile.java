/*
 * Copyright (C) 2020 The LineageOS Project
 * Copyright (C) 2022-2024 Nameless-AOSP Project
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

 package org.nameless.systemui.qs.tiles;

import static com.android.internal.util.nameless.BatteryFeatureSettingsHelper.REVERSE_CHARGING_DISABLED;
import static com.android.internal.util.nameless.BatteryFeatureSettingsHelper.REVERSE_CHARGING_ENABLED;
import static com.android.internal.util.nameless.BatteryFeatureSettingsHelper.REVERSE_CHARGING_UNSUSPENDED;

import static org.nameless.provider.SettingsExt.System.WIRELESS_REVERSE_CHARGING_ENABLED;
import static org.nameless.provider.SettingsExt.System.WIRELESS_REVERSE_CHARGING_SUSPENDED_STATUS;

import static vendor.nameless.hardware.battery.V1_0.Feature.WIRELESS_CHARGING_TX;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.service.quicksettings.Tile;
import android.view.View;

import androidx.annotation.Nullable;

import com.android.internal.logging.MetricsLogger;

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
import com.android.systemui.res.R;
import com.android.systemui.statusbar.policy.KeyguardStateController;
import com.android.systemui.util.settings.SystemSettings;

import javax.inject.Inject;

import org.nameless.os.BatteryFeatureManager;

public class PowerShareTile extends SecureQSTile<BooleanState> {

    public static final String TILE_SPEC = "powershare";

    private final BatteryFeatureManager mBatteryFeatureManager;
    private final Icon mIcon = ResourceIcon.get(
            com.android.internal.R.drawable.ic_wireless_reverse_charging);

    private UserSettingObserver mEnabledSetting;
    private UserSettingObserver mStatusSetting;

    @Inject
    public PowerShareTile(
            QSHost host,
            QsEventLogger uiEventLogger,
            @Background Looper backgroundLooper,
            @Main Handler mainHandler,
            FalsingManager falsingManager,
            MetricsLogger metricsLogger,
            StatusBarStateController statusBarStateController,
            ActivityStarter activityStarter,
            QSLogger qsLogger,
            SystemSettings systemSettings,
            KeyguardStateController keyguardStateController) {
        super(host, uiEventLogger, backgroundLooper, mainHandler, falsingManager,
                metricsLogger, statusBarStateController,
                activityStarter, qsLogger, keyguardStateController);

        mBatteryFeatureManager = BatteryFeatureManager.getInstance();
        if (!mBatteryFeatureManager.hasFeature(WIRELESS_CHARGING_TX)) {
            return;
        }

        mEnabledSetting = new UserSettingObserver(systemSettings, mHandler,
                WIRELESS_REVERSE_CHARGING_ENABLED, UserHandle.USER_SYSTEM) {
            @Override
            protected void handleValueChanged(int value, boolean observedChange) {
                handleRefreshState(null);
            }
        };
        mStatusSetting = new UserSettingObserver(systemSettings, mHandler,
                WIRELESS_REVERSE_CHARGING_SUSPENDED_STATUS, UserHandle.USER_SYSTEM) {
            @Override
            protected void handleValueChanged(int value, boolean observedChange) {
                handleRefreshState(null);
            }
        };
    }

    @Override
    public boolean isAvailable() {
        return mBatteryFeatureManager.hasFeature(WIRELESS_CHARGING_TX);
    }

    @Override
    public BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    protected void handleClick(@Nullable View view, boolean keyguardShowing) {
        if (checkKeyguard(view, keyguardShowing)) {
            return;
        }
        mEnabledSetting.setValue(mState.value ? REVERSE_CHARGING_DISABLED : REVERSE_CHARGING_ENABLED);
        refreshState();
    }

    @Override
    public Intent getLongClickIntent() {
        return new Intent().setComponent(new ComponentName(
                "com.android.settings", "com.android.settings.Settings$ReverseChargingActivity"));
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_wireless_reverse_charging_label);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        if (!isAvailable()) {
            return;
        }

        if (mEnabledSetting == null || mStatusSetting == null) {
            return;
        }
        final boolean enabled = mEnabledSetting.getValue() != REVERSE_CHARGING_DISABLED;
        state.value = enabled;
        state.label = mContext.getString(R.string.quick_settings_wireless_reverse_charging_label);
        state.icon = mIcon;
        state.contentDescription = mContext.getString(
                R.string.quick_settings_wireless_reverse_charging_label);
        if (enabled) {
            state.secondaryLabel = mStatusSetting.getValue() != REVERSE_CHARGING_UNSUSPENDED
                    ? mContext.getString(R.string.quick_settings_wireless_reverse_charging_suspended_label)
                    : mContext.getString(R.string.switch_bar_on);
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
    }
}
