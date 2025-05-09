/*
 * Copyright (C) 2015 The CyanogenMod Project
 * Copyright (C) 2017 The LineageOS Project
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

package org.sun.systemui.qs.tiles;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.SyncStatusObserver;
import android.os.Handler;
import android.os.Looper;
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
import com.android.systemui.qs.logging.QSLogger;
import com.android.systemui.res.R;
import com.android.systemui.statusbar.policy.KeyguardStateController;

import javax.inject.Inject;

/** Quick settings tile: Sync **/
public class SyncTile extends SecureQSTile<BooleanState> {

    public static final String TILE_SPEC = "sync";

    private final Icon mIcon = ResourceIcon.get(R.drawable.ic_qs_sync);

    private Object mSyncObserverHandle = null;
    private boolean mListening;

    @Inject
    public SyncTile(
            QSHost host,
            QsEventLogger uiEventLogger,
            @Background Looper backgroundLooper,
            @Main Handler mainHandler,
            FalsingManager falsingManager,
            MetricsLogger metricsLogger,
            StatusBarStateController statusBarStateController,
            ActivityStarter activityStarter,
            QSLogger qsLogger,
            KeyguardStateController keyguardStateController
    ) {
        super(host, uiEventLogger, backgroundLooper, mainHandler, falsingManager,
                metricsLogger, statusBarStateController,
                activityStarter, qsLogger, keyguardStateController);
    }

    @Override
    public BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    protected void handleClick(@Nullable Expandable expandable, boolean keyguardShowing) {
        if (checkKeyguard(expandable, keyguardShowing)) {
            return;
        }
        ContentResolver.setMasterSyncAutomatically(!mState.value);
        refreshState();
    }

    @Override
    public Intent getLongClickIntent() {
        Intent intent = new Intent("android.settings.SYNC_SETTINGS");
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        return intent;
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        state.value = ContentResolver.getMasterSyncAutomatically();
        state.label = mContext.getString(R.string.quick_settings_sync_label);
        state.icon = mIcon;
        if (state.value) {
            state.contentDescription =  mContext.getString(
                    R.string.accessibility_quick_settings_sync_on);
            state.state = Tile.STATE_ACTIVE;
        } else {
            state.contentDescription =  mContext.getString(
                    R.string.accessibility_quick_settings_sync_off);
            state.state = Tile.STATE_INACTIVE;
        }
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_sync_label);
    }

    @Override
    public int getMetricsCategory() {
        return 0;
    }

    @Override
    public void handleSetListening(boolean listening) {
        if (mListening == listening) return;
        mListening = listening;

        if (listening) {
            mSyncObserverHandle = ContentResolver.addStatusChangeListener(
                    ContentResolver.SYNC_OBSERVER_TYPE_SETTINGS, mSyncObserver);
        } else {
            ContentResolver.removeStatusChangeListener(mSyncObserverHandle);
            mSyncObserverHandle = null;
        }
    }

    private SyncStatusObserver mSyncObserver = new SyncStatusObserver() {
        public void onStatusChanged(int which) {
            mHandler.post(() -> {
                refreshState();
            });
        }
    };
}
