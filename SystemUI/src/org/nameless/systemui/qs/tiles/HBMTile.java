/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.systemui.qs.tiles;

import static vendor.nameless.hardware.displayfeature.V1_0.Feature.HBM_MODE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
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
import com.android.systemui.qs.logging.QSLogger;
import com.android.systemui.qs.tileimpl.QSTileImpl;

import javax.inject.Inject;

import org.nameless.display.DisplayFeatureManager;

public class HBMTile extends QSTileImpl<BooleanState> {

    public static final String TILE_SPEC = "hbm";

    private static final int[] DURATIONS = new int[] {
        1 * 60,   // 1 min
        5 * 60,   // 5 min
        10 * 60,  // 10 min
        -1,       // infinity
    };
    private static final int INFINITE_DURATION_INDEX = DURATIONS.length - 1;

    private final Icon mIcon = ResourceIcon.get(R.drawable.ic_hbm_mode);

    private final Receiver mReceiver = new Receiver();
    private final DisplayFeatureManager mManager = DisplayFeatureManager.getInstance();

    private int mDuration;
    private int mSecondsRemaining = 0;
    private long mLastClickTime = -1;
    private CountDownTimer mCountdownTimer = null;

    @Inject
    public HBMTile(
            QSHost host,
            QsEventLogger uiEventLogger,
            @Background Looper backgroundLooper,
            @Main Handler mainHandler,
            FalsingManager falsingManager,
            MetricsLogger metricsLogger,
            StatusBarStateController statusBarStateController,
            ActivityStarter activityStarter,
            QSLogger qsLogger
    ) {
        super(host, uiEventLogger, backgroundLooper, mainHandler, falsingManager,
                metricsLogger, statusBarStateController, activityStarter, qsLogger);
        mReceiver.init();
    }

    @Override
    public boolean isAvailable() {
        return mManager.hasFeature(HBM_MODE);
    }

    @Override
    public BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    protected void handleDestroy() {
        super.handleDestroy();
        stopCountDown();
        mReceiver.destroy();
    }

    @Override
    public void handleSetListening(boolean listening) {
    }

    @Override
    protected void handleClick(@Nullable View view) {
        final boolean enabled = getStatusForHBM();
        // If last user clicks < 5 seconds
        // we cycle different duration
        // otherwise toggle on/off
        if (enabled && (mLastClickTime != -1) &&
                (SystemClock.elapsedRealtime() - mLastClickTime < 3000)) {
            // cycle duration
            mDuration++;
            if (mDuration >= DURATIONS.length) {
                // all durations cycled, turn if off
                mDuration = -1;
                stopCountDown();
                setStatusForHBM(false);
            } else {
                // change duration
                startCountDown(DURATIONS[mDuration]);
                setStatusForHBM(true);
            }
        } else {
            // toggle
            if (enabled) {
                setStatusForHBM(false);
                stopCountDown();
            } else {
                setStatusForHBM(true);
                mDuration = 0;
                startCountDown(DURATIONS[mDuration]);
            }
        }
        mLastClickTime = SystemClock.elapsedRealtime();
        refreshState();
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        state.value = getStatusForHBM();
        state.icon = mIcon;
        state.label = mContext.getString(R.string.quick_settings_hbm_label);
        if (state.value) {
            state.secondaryLabel = formatValueWithRemainingTime();
            state.state = Tile.STATE_ACTIVE;
        } else {
            state.secondaryLabel = null;
            state.state = Tile.STATE_INACTIVE;
        }
    }

    @Override
    protected void handleLongClick(@Nullable View view) {
        if (getStatusForHBM()) {
            if (mDuration == INFINITE_DURATION_INDEX) {
                return;
            }
        } else {
            setStatusForHBM(true);
        }
        mDuration = INFINITE_DURATION_INDEX;
        startCountDown(DURATIONS[INFINITE_DURATION_INDEX]);
        refreshState();
    }

    @Override
    public Intent getLongClickIntent() {
        return null;
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_hbm_label);
    }

    @Override
    public int getMetricsCategory() {
        return 0;
    }

    private void startCountDown(long duration) {
        stopCountDown();
        mSecondsRemaining = (int) duration;
        if (duration == -1) {
            // infinity timing, no need to start timer
            return;
        }
        mCountdownTimer = new CountDownTimer(duration * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                mSecondsRemaining = (int) (millisUntilFinished / 1000);
                refreshState();
            }

            @Override
            public void onFinish() {
                setStatusForHBM(false);
                refreshState();
            }

        }.start();
    }

    private void stopCountDown() {
        if (mCountdownTimer != null) {
            mCountdownTimer.cancel();
            mCountdownTimer = null;
        }
    }

    private String formatValueWithRemainingTime() {
        if (mSecondsRemaining == -1 || mSecondsRemaining == 0 && getStatusForHBM()) {
            return "\u221E"; // infinity
        }
        return String.format("%02d:%02d",
                mSecondsRemaining / 60 % 60, mSecondsRemaining % 60);
    }

    private boolean getStatusForHBM() {
        return mManager.getFeatureEnabled(HBM_MODE);
    }

    private void setStatusForHBM(boolean enabled) {
        mManager.setFeatureEnabled(HBM_MODE, enabled);
    }

    private final class Receiver extends BroadcastReceiver {
        public void init() {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            mContext.registerReceiver(this, filter, null, mHandler);
        }

        public void destroy() {
            mContext.unregisterReceiver(this);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                stopCountDown();
                refreshState();
            }
        }
    }
}
