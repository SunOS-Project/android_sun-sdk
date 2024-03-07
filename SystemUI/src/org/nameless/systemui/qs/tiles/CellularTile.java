/*
 * Copyright (C) 2014 The Android Open Source Project
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

import android.annotation.NonNull;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.NetworkStats;
import android.net.NetworkTemplate;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.widget.Switch;

import androidx.annotation.Nullable;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.util.ArrayUtils;

import com.android.settingslib.net.DataUsageController;

import com.android.systemui.dagger.qualifiers.Background;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.FalsingManager;
import com.android.systemui.plugins.qs.QSIconView;
import com.android.systemui.plugins.qs.QSTile.BooleanState;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.QsEventLogger;
import com.android.systemui.qs.logging.QSLogger;
import com.android.systemui.res.R;
import com.android.systemui.statusbar.connectivity.IconState;
import com.android.systemui.statusbar.connectivity.MobileDataIndicators;
import com.android.systemui.statusbar.connectivity.NetworkController;
import com.android.systemui.statusbar.connectivity.SignalCallback;
import com.android.systemui.statusbar.policy.KeyguardStateController;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

/** Quick settings tile: Cellular **/
public class CellularTile extends SecureQSTile<BooleanState> {

    public static final String TILE_SPEC = "cell";

    private static final String ENABLE_SETTINGS_DATA_PLAN = "enable.settings.data.plan";

    private final NetworkController mController;
    private final DataUsageController mDataController;
    private final KeyguardStateController mKeyguard;
    private final CellSignalCallback mSignalCallback = new CellSignalCallback();

    @Inject
    public CellularTile(
            QSHost host,
            QsEventLogger uiEventLogger,
            @Background Looper backgroundLooper,
            @Main Handler mainHandler,
            FalsingManager falsingManager,
            MetricsLogger metricsLogger,
            StatusBarStateController statusBarStateController,
            ActivityStarter activityStarter,
            QSLogger qsLogger,
            NetworkController networkController,
            KeyguardStateController keyguardStateController
    ) {
        super(host, uiEventLogger, backgroundLooper, mainHandler, falsingManager,
                metricsLogger, statusBarStateController,
                activityStarter, qsLogger, keyguardStateController);
        mController = networkController;
        mKeyguard = keyguardStateController;
        mDataController = mController.getMobileDataController();
        mController.observe(getLifecycle(), mSignalCallback);
    }

    @Override
    public BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    public Intent getLongClickIntent() {
        if (getState().state == Tile.STATE_UNAVAILABLE) {
            return new Intent(Settings.ACTION_WIRELESS_SETTINGS);
        }
        return getCellularSettingIntent();
    }

    @Override
    protected void handleClick(@Nullable View view, boolean keyguardShowing) {
        if (checkKeyguard(view, keyguardShowing)) {
            return;
        }
        if (getState().state == Tile.STATE_UNAVAILABLE) {
            return;
        }
        mDataController.setMobileDataEnabled(!mDataController.isMobileDataEnabled());
    }

    @Override
    protected void handleSecondaryClick(@Nullable View view) {
        handleLongClick(view);
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_cellular_detail_title);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        CallbackInfo cb = (CallbackInfo) arg;
        if (cb == null) {
            cb = mSignalCallback.mInfo;
        }

        DataUsageController.DataUsageInfo carrierLabelInfo = mDataController.getDataUsageInfo(
                getMobileTemplate(mContext, mDataController.getSubscriptionId()));
        final Resources r = mContext.getResources();
        boolean mobileDataEnabled = mDataController.isMobileDataSupported()
                && mDataController.isMobileDataEnabled();
        state.value = mobileDataEnabled;
        state.expandedAccessibilityClassName = Switch.class.getName();
        if (cb.noSim) {
            state.label = r.getString(R.string.mobile_data);
            state.icon = ResourceIcon.get(R.drawable.ic_qs_no_sim);
        } else {
            if (carrierLabelInfo != null) {
                state.label = carrierLabelInfo.carrier;
            } else {
                state.label = r.getString(R.string.mobile_data);
            }
            state.icon = ResourceIcon.get(R.drawable.ic_swap_vert);
        }

        if (cb.noSim) {
            state.state = Tile.STATE_UNAVAILABLE;
            state.secondaryLabel = r.getString(R.string.keyguard_missing_sim_message_short);
        } else if (cb.airplaneModeEnabled) {
            state.state = Tile.STATE_UNAVAILABLE;
            state.secondaryLabel = r.getString(R.string.status_bar_airplane);
        } else if (mobileDataEnabled) {
            state.state = Tile.STATE_ACTIVE;
            state.secondaryLabel = appendMobileDataType(
                    // Only show carrier name if there are more than 1 subscription
                    cb.multipleSubs ? cb.dataSubscriptionName : "",
                    getMobileDataContentName(cb));
        } else {
            state.state = Tile.STATE_INACTIVE;
            state.secondaryLabel = r.getString(R.string.cell_data_off);
        }

        state.contentDescription = state.label;
        if (state.state == Tile.STATE_INACTIVE) {
            // This information is appended later by converting the Tile.STATE_INACTIVE state.
            state.stateDescription = "";
        } else {
            state.stateDescription = state.secondaryLabel;
        }
    }

    private CharSequence appendMobileDataType(CharSequence current, CharSequence dataType) {
        if (TextUtils.isEmpty(dataType)) {
            return Html.fromHtml(current.toString(), 0);
        }
        if (TextUtils.isEmpty(current)) {
            return Html.fromHtml(dataType.toString(), 0);
        }
        String concat = mContext.getString(R.string.mobile_carrier_text_format, current, dataType);
        return Html.fromHtml(concat, 0);
    }

    private CharSequence getMobileDataContentName(CallbackInfo cb) {
        if (cb.roaming && !TextUtils.isEmpty(cb.dataContentDescription)) {
            String roaming = mContext.getString(R.string.data_connection_roaming);
            String dataDescription = cb.dataContentDescription.toString();
            return mContext.getString(R.string.mobile_data_text_format, roaming, dataDescription);
        }
        if (cb.roaming) {
            return mContext.getString(R.string.data_connection_roaming);
        }
        return cb.dataContentDescription;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.QS_CELLULAR;
    }

    @Override
    public boolean isAvailable() {
        return mController.hasMobileDataFeature()
            && mHost.getUserContext().getUserId() == UserHandle.USER_SYSTEM;
    }

    private static final class CallbackInfo {
        boolean airplaneModeEnabled;
        @Nullable
        CharSequence dataSubscriptionName;
        @Nullable
        CharSequence dataContentDescription;
        boolean noSim;
        boolean roaming;
        boolean multipleSubs;
    }

    private final class CellSignalCallback implements SignalCallback {
        private final CallbackInfo mInfo = new CallbackInfo();

        @Override
        public void setMobileDataIndicators(@NonNull MobileDataIndicators indicators) {
            if (indicators.qsIcon == null) {
                // Not data sim, don't display.
                return;
            }
            mInfo.dataSubscriptionName = mController.getMobileDataNetworkName();
            mInfo.dataContentDescription = indicators.qsDescription != null
                    ? indicators.typeContentDescriptionHtml : null;
            mInfo.roaming = indicators.roaming;
            mInfo.multipleSubs = mController.getNumberSubscriptions() > 1;
            refreshState(mInfo);
        }

        @Override
        public void setNoSims(boolean show, boolean simDetected) {
            mInfo.noSim = show;
            refreshState(mInfo);
        }

        @Override
        public void setIsAirplaneMode(@NonNull IconState icon) {
            mInfo.airplaneModeEnabled = icon.visible;
            refreshState(mInfo);
        }
    }

    static Intent getCellularSettingIntent() {
        Intent intent = new Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS);
        int dataSub = SubscriptionManager.getDefaultDataSubscriptionId();
        if (dataSub != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            intent.putExtra(Settings.EXTRA_SUB_ID,
                    SubscriptionManager.getDefaultDataSubscriptionId());
        }
        return intent;
    }

    private static NetworkTemplate getMobileTemplate(Context context, int subId) {
        final TelephonyManager telephonyManager = context.getSystemService(TelephonyManager.class);
        final int mobileDefaultSubId = telephonyManager.getSubscriptionId();

        final SubscriptionManager subscriptionManager =
                context.getSystemService(SubscriptionManager.class);
        final List<SubscriptionInfo> subInfoList =
                subscriptionManager.getAvailableSubscriptionInfoList();
        if (subInfoList == null) {
            return getMobileTemplateForSubId(telephonyManager, mobileDefaultSubId);
        }

        for (SubscriptionInfo subInfo : subInfoList) {
            if (subInfo != null && subInfo.getSubscriptionId() == subId) {
                return getNormalizedMobileTemplate(telephonyManager, subId);
            }
        }
        return getMobileTemplateForSubId(telephonyManager, mobileDefaultSubId);
    }

    private static NetworkTemplate getMobileTemplateForSubId(
            TelephonyManager telephonyManager, int subId) {
        // Create template that matches any mobile network when the subscriberId is null.
        String subscriberId = telephonyManager.getSubscriberId(subId);
        return subscriberId != null
                ? new NetworkTemplate.Builder(NetworkTemplate.MATCH_CARRIER)
                .setSubscriberIds(Set.of(subscriberId))
                .setMeteredness(NetworkStats.METERED_YES)
                .build()
                : new NetworkTemplate.Builder(NetworkTemplate.MATCH_MOBILE)
                        .setMeteredness(NetworkStats.METERED_YES)
                        .build();
    }

    private static NetworkTemplate getNormalizedMobileTemplate(
            TelephonyManager telephonyManager, int subId) {
        final NetworkTemplate mobileTemplate = getMobileTemplateForSubId(telephonyManager, subId);
        final Set<String> mergedSubscriberIds = Set.of(telephonyManager
                .createForSubscriptionId(subId).getMergedImsisFromGroup());
        if (ArrayUtils.isEmpty(mergedSubscriberIds)) {
            return mobileTemplate;
        }

        return normalizeMobileTemplate(mobileTemplate, mergedSubscriberIds);
    }

    private static NetworkTemplate normalizeMobileTemplate(
            NetworkTemplate template, Set<String> mergedSet) {
        if (template.getSubscriberIds().isEmpty()) return template;
        // The input template should have at most 1 subscriberId.
        final String subscriberId = template.getSubscriberIds().iterator().next();

        if (mergedSet.contains(subscriberId)) {
            // Requested template subscriber is part of the merge group; return
            // a template that matches all merged subscribers.
            return new NetworkTemplate.Builder(template.getMatchRule())
                    .setSubscriberIds(mergedSet)
                    .setWifiNetworkKeys(template.getWifiNetworkKeys())
                    .setMeteredness(NetworkStats.METERED_YES).build();
        }

        return template;
    }
}
