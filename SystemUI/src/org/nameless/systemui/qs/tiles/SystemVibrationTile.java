/*
 * Copyright (C) 2023 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.systemui.qs.tiles;

import static android.provider.Settings.System.VIBRATE_ON;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
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
import com.android.systemui.settings.UserTracker;
import com.android.systemui.util.settings.SystemSettings;

import javax.inject.Inject;

public class SystemVibrationTile extends QSTileImpl<BooleanState> {

    public static final String TILE_SPEC = "system_vibration";

    private static final VibrationAttributes VIBRATION_ATTRIBUTE =
            new VibrationAttributes.Builder(
                    VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH))
            .setFlags(VibrationAttributes.FLAG_BYPASS_USER_VIBRATION_INTENSITY_OFF)
            .build();
    private static final VibrationEffect EFFECT_CLICK =
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK);

    private final Icon mIcon = ResourceIcon.get(R.drawable.ic_system_vibration);

    private final UserSettingObserver mSetting;
    private final Vibrator mVibrator;

    @Inject
    public SystemVibrationTile(
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
            UserTracker userTracker) {
        super(host, uiEventLogger, backgroundLooper, mainHandler, falsingManager,
                metricsLogger, statusBarStateController, activityStarter, qsLogger);

        mSetting = new UserSettingObserver(systemSettings, mHandler,
                VIBRATE_ON, userTracker.getUserId(), 1) {
            @Override
            protected void handleValueChanged(int value, boolean observedChange) {
                handleRefreshState(value);
            }
        };

        mVibrator = mContext.getSystemService(Vibrator.class);
    }

    @Override
    public boolean isAvailable() {
        return mVibrator.hasVibrator();
    }

    @Override
    public BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    protected void handleClick(@Nullable Expandable expandable) {
        final boolean prevEnabled = mState.value;
        mSetting.setValue(prevEnabled ? 0 : 1);
        if (!prevEnabled) {
            mVibrator.vibrate(EFFECT_CLICK, VIBRATION_ATTRIBUTE);
        }
        refreshState();
    }

    @Override
    public Intent getLongClickIntent() {
        return new Intent().setComponent(new ComponentName(
                "com.android.settings",
                "com.android.settings.Settings$VibrationSettingsActivity"));
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_system_vibration_label);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        if (mSetting == null) return;
        final int value = arg instanceof Integer ? (Integer) arg : mSetting.getValue();
        final boolean enabled = value != 0;
        state.value = enabled;
        state.label = mContext.getString(R.string.quick_settings_system_vibration_label);
        state.icon = mIcon;
        state.contentDescription =  mContext.getString(
                R.string.quick_settings_system_vibration_label);
        if (enabled) {
            state.state = Tile.STATE_ACTIVE;
        } else {
            state.state = Tile.STATE_INACTIVE;
        }
    }

    @Override
    protected boolean vibrateOnClick() {
        return false;
    }

    @Override
    public void handleSetListening(boolean listening) {
        super.handleSetListening(listening);
        mSetting.setListening(listening);
    }

    @Override
    public int getMetricsCategory() {
        return 0;
    }
}
