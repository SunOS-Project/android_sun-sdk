/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.systemui.qs.tiles;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemProperties;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.sysprop.TelephonyProperties;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.TelephonyIntents;

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
import com.android.systemui.statusbar.policy.KeyguardStateController;

import java.util.List;

import javax.inject.Inject;

public class NrTile extends SecureQSTile<BooleanState> {

    public static final String TILE_SPEC = "nr_5g";

    private static final String TAG = "NrTile";

    private final Icon mIcon = ResourceIcon.get(R.drawable.ic_qs_5g);

    private final TelephonyManager mTelephonyManager;

    private boolean mCanSwitch = true;
    private boolean mRegistered = false;

    private int mSimCount = 0;

    private final BroadcastReceiver mDefaultSubChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "Default subcription changed");
            refreshState();
        }
    };

    private final BroadcastReceiver mSimReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Sim card changed");
            refreshState();
        }
    };

    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String arg1) {
            mCanSwitch = mTelephonyManager.getCallState() == 0;
            refreshState();
        }
    };

    @Inject
    public NrTile(
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
                metricsLogger, statusBarStateController, activityStarter,
                qsLogger, keyguardStateController);
        mTelephonyManager = TelephonyManager.from(host.getContext());
    }

    @Override
    public boolean isAvailable() {
        final List<Integer> list = TelephonyProperties.default_network();
        for (int type : list) {
            if (type > 22) {
                return true;
            }
        }
        return false;
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
        if (!mCanSwitch) {
            Log.i(TAG, "Interrupted 5g switching due to call state");
            return;
        }
        if (mSimCount == 0) {
            return;
        }
        final int subId = SubscriptionManager.getDefaultDataSubscriptionId();
        final TelephonyManager tm = mTelephonyManager.createForSubscriptionId(subId);
        final boolean isCurrentNr = isCurrentNr(tm);
        long newType = tm.getAllowedNetworkTypesForReason(TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER);
        if (!isCurrentNr) {
            newType |= TelephonyManager.NETWORK_TYPE_BITMASK_NR;
        } else {
            newType &= ~TelephonyManager.NETWORK_TYPE_BITMASK_NR;
        }
        tm.setAllowedNetworkTypesForReason(TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER, newType);
        refreshState();
    }

    @Override
    public Intent getLongClickIntent() {
        if (mSimCount == 0) {
            return null;
        }
        final Intent intent = new Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS);
        final int dataSub = SubscriptionManager.getDefaultDataSubscriptionId();
        if (dataSub != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            intent.putExtra(Settings.EXTRA_SUB_ID,
                    SubscriptionManager.getDefaultDataSubscriptionId());
        }
        return intent;
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        state.icon = mIcon;
        state.label = mContext.getResources().getString(R.string.quick_settings_5g_tile_label);

        updateSimCount();
        if (mSimCount == 0) {
            state.state = Tile.STATE_UNAVAILABLE;
            return;
        }

        final int subId = SubscriptionManager.getDefaultDataSubscriptionId();
        final TelephonyManager tm = mTelephonyManager.createForSubscriptionId(subId);
        state.state = isCurrentNr(tm) ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE;
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_5g_tile_label);
    }

    @Override
    public int getMetricsCategory() {
        return 0;
    }

    @Override
    public void handleSetListening(boolean listening) {
        if (listening) {
            if (!mRegistered) {
                IntentFilter filter = new IntentFilter();
                filter.addAction(TelephonyIntents.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED);
                mContext.registerReceiver(mDefaultSubChangeReceiver, filter);
                filter = new IntentFilter();
                filter.addAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
                mContext.registerReceiver(mSimReceiver, filter);
                mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
                mRegistered = true;
            }
            refreshState();
        } else if (mRegistered) {
            mContext.unregisterReceiver(mDefaultSubChangeReceiver);
            mContext.unregisterReceiver(mSimReceiver);
            mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
            mRegistered = false;
        }
    }

    private boolean isCurrentNr(TelephonyManager tm) {
        final long allowedNetworkTypes =
                tm.getAllowedNetworkTypesForReason(TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER);
        return (allowedNetworkTypes & TelephonyManager.NETWORK_TYPE_BITMASK_NR) != 0;
    }

    private void updateSimCount() {
        final String simState = SystemProperties.get("gsm.sim.state");
        Log.d(TAG, "updateSimCount, simState: " + simState);
        mSimCount = 0;
        try {
            final String[] sims = TextUtils.split(simState, ",");
            for (String sim : sims) {
                if (!sim.isEmpty()
                        && !sim.equalsIgnoreCase(IccCardConstants.INTENT_VALUE_ICC_ABSENT)
                        && !sim.equalsIgnoreCase(IccCardConstants.INTENT_VALUE_ICC_NOT_READY)) {
                    mSimCount++;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error to parse sim state");
        }
        Log.d(TAG, "updateSimCount, mSimCount: " + mSimCount);
    }
}
