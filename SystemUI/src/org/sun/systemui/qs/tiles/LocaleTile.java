/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (C) 2017 AICP
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

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.LocaleList;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.android.internal.app.LocalePicker;
import com.android.internal.logging.MetricsLogger;

import com.android.systemui.animation.Expandable;
import com.android.systemui.dagger.qualifiers.Background;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.FalsingManager;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.plugins.qs.QSTile.BooleanState;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.QsEventLogger;
import com.android.systemui.qs.logging.QSLogger;
import com.android.systemui.qs.pipeline.domain.interactor.PanelInteractor;
import com.android.systemui.res.R;
import com.android.systemui.statusbar.policy.KeyguardStateController;

import java.util.Locale;

import javax.inject.Inject;

/** Quick settings tile: Locale **/
public class LocaleTile extends SecureQSTile<BooleanState> {

    public static final String TILE_SPEC = "locale";

    private boolean mListening;

    private LocaleList mLocaleList;

    // If not null: update pending
    private Locale currentLocaleBackup;

    // Allow multiple clicks to find the desired locale without immediately applying
    private static final int TOGGLE_DELAY = 800;

    private final PanelInteractor mPanelInteractor;

    @Inject
    public LocaleTile(
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
                metricsLogger, statusBarStateController,
                activityStarter, qsLogger, keyguardStateController);
        mPanelInteractor = panelInteractor;
        updateLocaleList();
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

        if (checkToggleDisabled(expandable)) return;
        toggleLocale();
    }

    @Override
    protected void handleSecondaryClick(@Nullable Expandable expandable) {
        if (checkToggleDisabled(expandable)) return;
        toggleLocale();
    }

    @Override
    public void handleSetListening(boolean listening) {
        // Do nothing
    }

    private void toggleLocale() {
        if (currentLocaleBackup == null) {
            currentLocaleBackup = mLocaleList.get(0);
        }
        Locale[] newLocales = new Locale[mLocaleList.size()];
        for (int i = 0; i < newLocales.length; i++) {
            newLocales[i] = mLocaleList.get((i+1)%newLocales.length);
        }
        mLocaleList = new LocaleList(newLocales);
        mHandler.removeCallbacks(applyLocale);
        mHandler.postDelayed(applyLocale, TOGGLE_DELAY);
        refreshState();
    }

    private Runnable applyLocale = new Runnable() {
        @Override
        public void run() {
            if (!mLocaleList.get(0).equals(currentLocaleBackup)) {
                mPanelInteractor.collapsePanels();
                LocalePicker.updateLocales(mLocaleList);
            }
            currentLocaleBackup = null;
        }
    };

    @Override
    public Intent getLongClickIntent() {
        return new Intent().setComponent(new ComponentName(
            "com.android.settings", "com.android.settings.LanguageSettings"));
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_locale_label);
    }

    @Override
    public int getMetricsCategory() {
        return 0;
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        state.icon = ResourceIcon.get(R.drawable.ic_qs_locale);
        state.label = mContext.getString(R.string.quick_settings_locale_label);
        state.secondaryLabel = mLocaleList.get(0).getDisplayLanguage();
    }

    public void setListening(boolean listening) {
        if (mListening == listening) return;
        mListening = listening;
        if (listening) {
            final IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_LOCALE_CHANGED);
            mContext.registerReceiver(mReceiver, filter);
        } else {
            mContext.unregisterReceiver(mReceiver);
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_LOCALE_CHANGED.equals(intent.getAction())) {
                updateLocaleList();
                refreshState();
            }
        }
    };

    private void updateLocaleList() {
        if (currentLocaleBackup != null) return;
        mLocaleList = LocaleList.getAdjustedDefault();
    }

    private boolean checkToggleDisabled(@Nullable Expandable expandable) {
        updateLocaleList();
        if (mLocaleList.size() <= 1) {
            handleLongClick(expandable);
            Toast.makeText(mContext,
                    mContext.getString(R.string.quick_settings_locale_more_locales_toast),
                    Toast.LENGTH_LONG).show();
            return true;
        } else {
            return false;
        }
    }
}
