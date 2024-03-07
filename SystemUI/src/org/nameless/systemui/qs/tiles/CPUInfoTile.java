/*
 * Copyright (C) 2017-2018 Benzo Rom
 *           (C) 2017-2021 crDroidAndroid Project
 *           (C) 2023 Nameless-AOSP
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
import android.service.quicksettings.Tile;
import android.view.View;

import androidx.annotation.Nullable;

import com.android.internal.logging.MetricsLogger;

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
import com.android.systemui.res.R;

import javax.inject.Inject;

import org.nameless.systemui.cpuinfo.CPUInfoHelper;

/** Quick settings tile: CPUInfo overlay **/
public class CPUInfoTile extends QSTileImpl<BooleanState> {

    public static final String TILE_SPEC = "cpuinfo";

    private final Icon mIcon = ResourceIcon.get(R.drawable.ic_qs_cpu_info);

    private boolean mRunning = false;

    @Inject
    public CPUInfoTile(
            QSHost host,
            QsEventLogger uiEventLogger,
            @Background Looper backgroundLooper,
            @Main Handler mainHandler,
            FalsingManager falsingManager,
            MetricsLogger metricsLogger,
            StatusBarStateController statusBarStateController,
            ActivityStarter activityStarter,
            QSLogger qsLogger) {
        super(host, uiEventLogger, backgroundLooper, mainHandler, falsingManager,
                metricsLogger, statusBarStateController, activityStarter, qsLogger);
    }

    @Override
    public BooleanState newTileState() {
        BooleanState state = new BooleanState();
        state.handlesLongClick = false;
        return state;
    }

    @Override
    protected void handleClick(@Nullable View view) {
        if (mRunning) {
            CPUInfoHelper.stppService(mContext);
            mRunning = false;
        } else {
            CPUInfoHelper.startService(mContext);
            mRunning = true;
        }
        refreshState();
    }

    @Override
    public Intent getLongClickIntent() {
        return null;
    }

    @Override
    protected void handleInitialize() {
        mRunning = CPUInfoHelper.isServiceRunning(mContext);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        state.value = mRunning;
        state.label = mContext.getString(R.string.quick_settings_cpuinfo_label);
        state.icon = mIcon;
        state.contentDescription =  mContext.getString(
                R.string.quick_settings_cpuinfo_label);
        if (mRunning) {
            state.state = Tile.STATE_ACTIVE;
        } else {
            state.state = Tile.STATE_INACTIVE;
        }
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_cpuinfo_label);
    }

    @Override
    public int getMetricsCategory() {
        return 0;
    }

    @Override
    public void handleSetListening(boolean listening) {
        mRunning = CPUInfoHelper.isServiceRunning(mContext);
    }
}
