/*
 * Copyright (C) 2017 ABC rom
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

package org.sun.systemui.qs.tiles;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.quicksettings.Tile;

import androidx.annotation.Nullable;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.util.sun.CustomUtils;

import com.android.systemui.animation.Expandable;
import com.android.systemui.dagger.qualifiers.Background;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.FalsingManager;
import com.android.systemui.plugins.qs.QSTile.BooleanState;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.QsEventLogger;
import com.android.systemui.qs.logging.QSLogger;
import com.android.systemui.qs.pipeline.domain.interactor.PanelInteractor;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.res.R;
import com.android.systemui.statusbar.policy.KeyguardStateController;

import javax.inject.Inject;

/** Quick settings tile: Screenshot **/
public class ScreenshotTile extends QSTileImpl<BooleanState> {

    public static final String TILE_SPEC = "screenshot";

    private final KeyguardStateController mKeyguard;
    private final PanelInteractor mPanelInteractor;

    private boolean mRegion = false;

    @Inject
    public ScreenshotTile(
            QSHost host,
            QsEventLogger uiEventLogger,
            @Background Looper backgroundLooper,
            @Main Handler mainHandler,
            FalsingManager falsingManager,
            MetricsLogger metricsLogger,
            StatusBarStateController statusBarStateController,
            ActivityStarter activityStarter,
            QSLogger qsLogger,
            KeyguardStateController keyguardStateController,
            PanelInteractor panelInteractor
    ) {
        super(host, uiEventLogger, backgroundLooper, mainHandler, falsingManager,
                metricsLogger, statusBarStateController, activityStarter, qsLogger);
        mKeyguard = keyguardStateController;
        mPanelInteractor = panelInteractor;
    }

    @Override
    public int getMetricsCategory() {
        return 0;
    }

    @Override
    public BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    public void handleSetListening(boolean listening) {
    }

    @Override
    protected void handleClick(@Nullable Expandable expandable) {
        if (mKeyguard.isShowing()) {
            return;
        }
        mRegion = !mRegion;
        refreshState();
    }

    @Override
    public void handleLongClick(@Nullable Expandable expandable) {
        if (mKeyguard.isShowing()) {
            return;
        }
        mPanelInteractor.collapsePanels();
        mHandler.postDelayed(() -> {
            CustomUtils.takeScreenshot(!mRegion);
        }, 1000L);
    }

    @Override
    public Intent getLongClickIntent() {
        return null;
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_screenshot_label);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        state.label = mContext.getString(R.string.quick_settings_screenshot_label);
        state.icon = ResourceIcon.get(R.drawable.ic_qs_screenshot);
        if (mKeyguard.isShowing()) {
            state.state = Tile.STATE_UNAVAILABLE;
        } else {
            state.state = Tile.STATE_INACTIVE;
        }
        if (mRegion) {
            state.secondaryLabel = mContext.getString(R.string.quick_settings_region_screenshot_label);
            state.contentDescription = mContext.getString(
                    R.string.quick_settings_region_screenshot_label);
        } else {
            state.secondaryLabel = mContext.getString(R.string.quick_settings_full_screenshot_label);
            state.contentDescription = mContext.getString(
                    R.string.quick_settings_full_screenshot_label);
        }
    }
}
