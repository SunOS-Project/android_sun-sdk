/*
 * Copyright (C) 2018 The OmniROM Project
 *               2020-2021 The LineageOS Project
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

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.view.View;

import androidx.annotation.Nullable;

import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.dagger.qualifiers.Background;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.FalsingManager;
import com.android.systemui.plugins.qs.QSTile.BooleanState;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.QsEventLogger;
import com.android.systemui.qs.SettingObserver;
import com.android.systemui.qs.logging.QSLogger;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.settings.UserTracker;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.util.settings.SecureSettings;

import javax.inject.Inject;

import org.nameless.provider.SettingsExt;

public class AODTile extends QSTileImpl<BooleanState> implements
        BatteryController.BatteryStateChangeCallback {

    public static final String TILE_SPEC = "aod";

    private final Icon mIcon = ResourceIcon.get(R.drawable.ic_qs_aod);
    private final BatteryController mBatteryController;

    private final SettingObserver mAodSetting;
    private final SettingObserver mAodOnChargeSetting;

    @Inject
    public AODTile(
            QSHost host,
            QsEventLogger uiEventLogger,
            @Background Looper backgroundLooper,
            @Main Handler mainHandler,
            FalsingManager falsingManager,
            MetricsLogger metricsLogger,
            StatusBarStateController statusBarStateController,
            ActivityStarter activityStarter,
            QSLogger qsLogger,
            SecureSettings secureSettings,
            BatteryController batteryController,
            UserTracker userTracker
    ) {
        super(host, uiEventLogger, backgroundLooper, mainHandler, falsingManager,
                metricsLogger, statusBarStateController, activityStarter, qsLogger);

        mAodSetting = new SettingObserver(secureSettings, mHandler,
                Settings.Secure.DOZE_ALWAYS_ON,
                userTracker.getUserId()) {
            @Override
            protected void handleValueChanged(int value, boolean observedChange) {
                handleRefreshState(value);
            }
        };
        mAodOnChargeSetting = new SettingObserver(secureSettings, mHandler,
                SettingsExt.Secure.DOZE_ON_CHARGE,
                userTracker.getUserId()) {
            @Override
            protected void handleValueChanged(int value, boolean observedChange) {
                handleRefreshState(value);
            }
        };

        mBatteryController = batteryController;
        batteryController.observe(getLifecycle(), this);
    }

    @Override
    public void onPowerSaveChanged(boolean isPowerSave) {
        refreshState();
    }

    @Override
    protected void handleDestroy() {
        super.handleDestroy();
        mAodSetting.setListening(false);
        mAodOnChargeSetting.setListening(false);
    }

    @Override
    public boolean isAvailable() {
        return mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_dozeAlwaysOnDisplayAvailable);
    }

    @Override
    public BooleanState newTileState() {
        BooleanState state = new BooleanState();
        state.handlesLongClick = false;
        return state;
    }

    @Override
    public void handleSetListening(boolean listening) {
        super.handleSetListening(listening);
        mAodSetting.setListening(listening);
        mAodOnChargeSetting.setListening(listening);
    }

    @Override
    protected void handleUserSwitch(int newUserId) {
        mAodSetting.setUserId(newUserId);
        mAodOnChargeSetting.setUserId(newUserId);
        refreshState();
    }

    @Override
    protected void handleClick(@Nullable View view) {
        if (mState.state == Tile.STATE_UNAVAILABLE) {
            return;
        }
        if (!mState.value) {
            mAodSetting.setValue(1);
        } else {
            if (mAodOnChargeSetting.getValue() == 0) {
                mAodOnChargeSetting.setValue(1);
            } else {
                mAodOnChargeSetting.setValue(0);
                mAodSetting.setValue(0);
            }
        }
        refreshState();
    }

    @Override
    public Intent getLongClickIntent() {
        return null;
    }

    @Override
    public CharSequence getTileLabel() {
        if (mBatteryController.isAodPowerSave()) {
            return mContext.getString(R.string.quick_settings_aod_off_powersave_label);
        }
        return mContext.getString(R.string.quick_settings_aod_label);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        if (state.slash == null) {
            state.slash = new SlashState();
        }
        final boolean enable = mAodSetting.getValue() != 0;
        state.icon = mIcon;
        state.value = enable;
        state.slash.isSlashed = state.value;
        state.label = enable && mAodOnChargeSetting.getValue() != 0 ?
                mContext.getString(R.string.quick_settings_aod_on_charge_label) :
                mContext.getString(R.string.quick_settings_aod_label);
        if (mBatteryController.isAodPowerSave()) {
            state.state = Tile.STATE_UNAVAILABLE;
        } else {
            state.state = enable ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE;
        }
    }

    @Override
    public int getMetricsCategory() {
        return 0;
    }
}
