/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.systemui.qs.tiles;

import static vendor.nameless.hardware.displayfeature.Feature.DC_DIMMING;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.quicksettings.Tile;

import androidx.annotation.Nullable;

import com.android.internal.logging.MetricsLogger;

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

import javax.inject.Inject;

import org.nameless.display.DisplayFeatureManager;
import org.nameless.provider.SettingsExt.System;

/** Quick settings tile: DC Dimming **/
public class DcDimmingTile extends QSTileImpl<BooleanState> {

    public static final String TILE_SPEC = "dc_dimming";

    private final Icon mIcon = ResourceIcon.get(R.drawable.ic_dc_dimming_tile);

    private final DisplayFeatureManager mManager;
    private final UserSettingObserver mSetting;

    @Inject
    public DcDimmingTile(
            QSHost host,
            QsEventLogger uiEventLogger,
            @Background Looper backgroundLooper,
            @Main Handler mainHandler,
            FalsingManager falsingManager,
            MetricsLogger metricsLogger,
            StatusBarStateController statusBarStateController,
            ActivityStarter activityStarter,
            QSLogger qsLogger,
            SystemSettings systemSettings
    ) {
        super(host, uiEventLogger, backgroundLooper, mainHandler, falsingManager,
                metricsLogger, statusBarStateController, activityStarter, qsLogger);

        mManager = DisplayFeatureManager.getInstance();
        mSetting = new UserSettingObserver(systemSettings, mHandler,
                System.DC_DIMMING_STATE, UserHandle.USER_SYSTEM) {
            @Override
            protected void handleValueChanged(int value, boolean observedChange) {
                handleRefreshState(value);
            }
        };
    }

    @Override
    public boolean isAvailable() {
        return mManager.hasFeature(DC_DIMMING);
    }

    @Override
    public BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    protected void handleClick(@Nullable Expandable expandable) {
        if (getState().state == Tile.STATE_UNAVAILABLE) {
            return;
        }
        mSetting.setValue(mState.value ? 0 : 1);
        refreshState();
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        final int value = arg instanceof Integer ? (Integer) arg : mSetting.getValue();
        state.value = value != 0;;
        state.label = mContext.getString(R.string.quick_settings_dc_dimming_label);
        state.contentDescription = mContext.getString(R.string.quick_settings_dc_dimming_label);
        state.icon = mIcon;
        state.state = state.value ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE;
    }

    @Override
    public int getMetricsCategory() {
        return 0;
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_dc_dimming_label);
    }

    @Override
    public Intent getLongClickIntent() {
        return new Intent(Settings.ACTION_DISPLAY_SETTINGS);
    }

    @Override
    protected void handleSetListening(boolean listening) {
        super.handleSetListening(listening);
        mSetting.setListening(listening);
    }
}
