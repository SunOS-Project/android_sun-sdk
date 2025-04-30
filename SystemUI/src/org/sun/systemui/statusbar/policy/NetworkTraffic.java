package org.sun.systemui.statusbar.policy;

import static android.content.pm.ActivityInfo.CONFIG_DENSITY;
import static android.content.pm.ActivityInfo.CONFIG_FONT_SCALE;

import static com.android.systemui.statusbar.StatusBarIconView.STATE_DOT;
import static com.android.systemui.statusbar.StatusBarIconView.STATE_HIDDEN;
import static com.android.systemui.statusbar.StatusBarIconView.STATE_ICON;

import static org.sun.provider.SettingsExt.System.NETWORK_TRAFFIC_AUTOHIDE_THRESHOLD;
import static org.sun.provider.SettingsExt.System.NETWORK_TRAFFIC_MODE;
import static org.sun.provider.SettingsExt.System.NETWORK_TRAFFIC_REFRESH_INTERVAL;
import static org.sun.provider.SettingsExt.System.NETWORK_TRAFFIC_STATE;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.view.Gravity;
import android.net.ConnectivityManager;
import android.net.TrafficStats;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.Spanned;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.internal.util.sun.ScreenStateListener;

import com.android.systemui.Dependency;
import com.android.systemui.plugins.DarkIconDispatcher;
import com.android.systemui.statusbar.StatusIconDisplayable;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Set;

public class NetworkTraffic extends TextView implements StatusIconDisplayable {

    public static final String SLOT = "networktraffic";

    private static final Set<Integer> SUPPORTED_CHANGES = Set.of(
        CONFIG_DENSITY,
        CONFIG_FONT_SCALE
    );

    private static final int KB = 1024;
    private static final int MB = KB * KB;
    private static final int GB = MB * KB;

    private static final int MODE_DYNAMIC = 0;
    private static final int MODE_DOWNLOAD_ONLY = 1;
    private static final int MODE_UPLOAD_ONLY = 2;

    private static final int MSG_UPDATE_VIEW = 0;
    private static final int MSG_UPDATE_ALL = 1;

    private final ConnectivityManager mConnectivityManager;
    private final ContentResolver mResolver;
    private final Context mContext;
    private final Resources mResources;

    private ScreenStateListener mScreenStateListener;
    private SettingsObserver mObserver;

    private long totalRxBytes;
    private long totalTxBytes;
    private long lastUpdateTime;

    private int mAutoHideThreshold = 0;
    private int mRefreshInterval = 1;
    private int mIndicatorMode = MODE_DYNAMIC;

    private int mTintColor;

    private int mVisibleState = -1;

    private boolean mRegistered = false;
    private boolean mIsEnabled = false;
    private boolean mScreenOn = true;
    private boolean mSystemIconVisible = true;

    private final Configuration mLastConfig = new Configuration();

    private final Handler mTrafficHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            long timeDelta = SystemClock.elapsedRealtime() - lastUpdateTime;

            if (timeDelta < mRefreshInterval * 1000 * 0.95) {
                if (msg.what == MSG_UPDATE_VIEW) {
                    // we just updated the view, nothing further to do
                    return;
                }
                if (timeDelta < 1) {
                    // Can't div by 0 so make sure the value displayed is minimal
                    timeDelta = Long.MAX_VALUE;
                }
            }
            lastUpdateTime = SystemClock.elapsedRealtime();

            // Calculate the data rate from the change in total bytes and time
            final long newTotalRxBytes = TrafficStats.getTotalRxBytes();
            final long newTotalTxBytes = TrafficStats.getTotalTxBytes();
            final long rxData = newTotalRxBytes - totalRxBytes;
            final long txData = newTotalTxBytes - totalTxBytes;

            if (shouldHide(rxData, txData, timeDelta)) {
                setText("");
                setVisibility(View.GONE);
            } else if (shouldShowUpload(rxData, txData, timeDelta)) {
                // Show information for uplink if it's called for
                final CharSequence output = formatOutput(timeDelta, txData);

                // Update view if there's anything new to show
                if (!output.toString().equals(getText().toString())) {
                    setText(output);
                }
                setVisibility(View.VISIBLE);
            } else {
                // Add information for downlink if it's called for
                final CharSequence output = formatOutput(timeDelta, rxData);

                // Update view if there's anything new to show
                if (!output.toString().equals(getText().toString())) {
                    setText(output);
                }
                setVisibility(View.VISIBLE);
            }

            // Post delayed message to refresh in ~1000ms
            totalRxBytes = newTotalRxBytes;
            totalTxBytes = newTotalTxBytes;
            mTrafficHandler.removeCallbacksAndMessages(null);
            if (!isDisabled()) {
                mTrafficHandler.sendEmptyMessageDelayed(MSG_UPDATE_VIEW, mRefreshInterval * 1000);
            } else {
                setText("");
                setVisibility(View.GONE);
            }
        }

        private CharSequence formatOutput(long timeDelta, long data) {
            return formatDecimal((long) (data / (timeDelta / 1000f)));
        }

        private CharSequence formatDecimal(long speed) {
            final DecimalFormat decimalFormat;
            final String formatSpeed;
            final String formatUnit;
            if (speed >= GB) {
                decimalFormat = new DecimalFormat("0.00");
                formatSpeed =  decimalFormat.format(speed / (float) GB);
                formatUnit = "GB/S";
            } else if (speed >= 100 * MB) {
                decimalFormat = new DecimalFormat("000");
                formatSpeed =  decimalFormat.format(speed / (float) MB);
                formatUnit = "MB/S";
            } else if (speed >= 10 * MB) {
                decimalFormat = new DecimalFormat("00.0");
                formatSpeed =  decimalFormat.format(speed / (float) MB);
                formatUnit = "MB/S";
            } else if (speed >= MB) {
                decimalFormat = new DecimalFormat("0.00");
                formatSpeed =  decimalFormat.format(speed / (float) MB);
                formatUnit = "MB/S";
            } else if (speed >= 100 * KB) {
                decimalFormat = new DecimalFormat("000");
                formatSpeed =  decimalFormat.format(speed / (float) KB);
                formatUnit = "KB/S";
            } else if (speed >= 10 * KB) {
                decimalFormat = new DecimalFormat("00.0");
                formatSpeed =  decimalFormat.format(speed / (float) KB);
                formatUnit = "KB/S";
            } else {
                decimalFormat = new DecimalFormat("0.00");
                formatSpeed = decimalFormat.format(speed / (float) KB);
                formatUnit = "KB/S";
            }

            final SpannableString spanSpeedString = new SpannableString(formatSpeed);
            spanSpeedString.setSpan(getSpeedRelativeSizeSpan(), 0, formatSpeed.length(),
                    Spanned.SPAN_INCLUSIVE_INCLUSIVE);

            final SpannableString spanUnitString = new SpannableString(formatUnit);
            spanUnitString.setSpan(getUnitRelativeSizeSpan(), 0, formatUnit.length(),
                    Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            return TextUtils.concat(spanSpeedString, "\n", spanUnitString);
        }

        private boolean shouldHide(long rxData, long txData, long timeDelta) {
            if (isDisabled()) return true;
            final long speedRxKB = (long) (rxData / (timeDelta / 1000f)) / KB;
            final long speedTxKB = (long) (txData / (timeDelta / 1000f)) / KB;
            return !getConnectAvailable() ||
                    (speedRxKB < mAutoHideThreshold &&
                    speedTxKB < mAutoHideThreshold);
        }

        private boolean shouldShowUpload(long rxData, long txData, long timeDelta) {
            final long speedRxKB = (long) (rxData / (timeDelta / 1000f)) / KB;
            final long speedTxKB = (long) (txData / (timeDelta / 1000f)) / KB;

            if (mIndicatorMode == MODE_DYNAMIC) {
                return (speedTxKB > speedRxKB);
            }
            return mIndicatorMode == MODE_UPLOAD_ONLY;
        }
    };

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                getHandler().post(NetworkTraffic.this::update);
            }
        }
    };

    private final class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void register() {
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    NETWORK_TRAFFIC_STATE), false, this, UserHandle.USER_ALL);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    NETWORK_TRAFFIC_MODE), false, this, UserHandle.USER_ALL);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    NETWORK_TRAFFIC_AUTOHIDE_THRESHOLD), false, this, UserHandle.USER_ALL);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    NETWORK_TRAFFIC_REFRESH_INTERVAL), false, this, UserHandle.USER_ALL);
        }

        void unregister() {
            mResolver.unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange) {
            setMode();
            update();
        }
    }

    public NetworkTraffic(Context context) {
        this(context, null);
    }

    public NetworkTraffic(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NetworkTraffic(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        mResolver = context.getContentResolver();
        mResources = context.getResources();

        mConnectivityManager = context.getSystemService(ConnectivityManager.class);

        mTintColor = getCurrentTextColor();
        setMode();

        setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mScreenStateListener = new ScreenStateListener(mContext, getHandler()) {
            @Override
            public void onScreenOff() {
                mScreenOn = false;
                registerListeners(false);
                update();
            }

            @Override
            public void onScreenOn() {
                mScreenOn = true;
                registerListeners(true);
                update();
            }

            @Override
            public void onScreenUnlocked() {}
        };
        mScreenStateListener.setListening(true);
        mObserver = new SettingsObserver(getHandler());
        registerListeners(true);
        update();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mScreenStateListener.setListening(false);
        registerListeners(false);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        final int diff = mLastConfig.updateFrom(newConfig);
        for (int change : SUPPORTED_CHANGES) {
            if ((diff & change) != 0) {
                setSpacingAndFonts();
                getHandler().post(NetworkTraffic.this::update);
                return;
            }
        }
    }

    @Override
    public void onDarkChanged(ArrayList<Rect> areas, float darkIntensity, int tint) {
        if (!mIsEnabled) return;
        mTintColor = DarkIconDispatcher.getTint(areas, this, tint);
        setTextColor(mTintColor);
        updateTrafficDrawable();
    }

    @Override
    public String getSlot() {
        return SLOT;
    }

    @Override
    public boolean isIconVisible() {
        return mIsEnabled;
    }

    @Override
    public int getVisibleState() {
        return mVisibleState;
    }

    @Override
    public void setVisibleState(int state, boolean animate) {
        if (state == mVisibleState || !mIsEnabled || !mRegistered) {
            return;
        }
        mVisibleState = state;

        switch (state) {
            case STATE_ICON:
                mSystemIconVisible = true;
                break;
            case STATE_DOT:
            case STATE_HIDDEN:
            default:
                mSystemIconVisible = false;
                break;
        }
    }

    @Override
    public void setStaticDrawableColor(int color) {
        mTintColor = color;
        setTextColor(mTintColor);
        updateTrafficDrawable();
    }

    @Override
    public void setDecorColor(int color) {
        mTintColor = color;
        updateTrafficDrawable();
    }

    private boolean getConnectAvailable() {
        return mConnectivityManager.getActiveNetworkInfo() != null;
    }

    private void registerListeners(boolean enable) {
        if (enable && !mRegistered && mScreenOn) {
            mRegistered = true;
            final IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            mContext.registerReceiver(mIntentReceiver, filter, null, getHandler());
            mObserver.register();
        } else if (!enable && mRegistered) {
            mRegistered = false;
            mObserver.unregister();
            mContext.unregisterReceiver(mIntentReceiver);
            mTrafficHandler.removeCallbacksAndMessages(null);
        }
    }

    private void update() {
        setText("");
        setVisibility(View.GONE);
        setSpacingAndFonts();
        updateTrafficDrawable();
        if (!isDisabled()) {
            totalRxBytes = TrafficStats.getTotalRxBytes();
            lastUpdateTime = SystemClock.elapsedRealtime();
            mTrafficHandler.sendEmptyMessage(MSG_UPDATE_ALL);
        }
    }

    private void setMode() {
        mIsEnabled = Settings.System.getIntForUser(mResolver,
                NETWORK_TRAFFIC_STATE, 0,
                UserHandle.USER_CURRENT) == 1;
        mIndicatorMode = Settings.System.getIntForUser(mResolver,
                NETWORK_TRAFFIC_MODE, MODE_DYNAMIC,
                UserHandle.USER_CURRENT);
        mAutoHideThreshold = Settings.System.getIntForUser(mResolver,
                NETWORK_TRAFFIC_AUTOHIDE_THRESHOLD, 0,
                UserHandle.USER_CURRENT);
        mRefreshInterval = Settings.System.getIntForUser(mResolver,
                NETWORK_TRAFFIC_REFRESH_INTERVAL, 1,
                UserHandle.USER_CURRENT);
    }

    private void updateTrafficDrawable() {
        if (isDisabled()) return;
        setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
        setTextColor(mTintColor);
    }

    private void setSpacingAndFonts() {
        if (isDisabled()) return;
        final String txtFont = mResources.getString(com.android.internal.R.string.config_headlineFontFamily);
        setTypeface(Typeface.create(txtFont, Typeface.BOLD));
        setLineSpacing(0.75f, 0.75f);
        setGravity(Gravity.CENTER);
        setMaxLines(2);
    }

    private boolean isDisabled() {
        return !mIsEnabled || !mSystemIconVisible || !mScreenOn || !mRegistered;
    }

    private static RelativeSizeSpan getSpeedRelativeSizeSpan() {
        return new RelativeSizeSpan(0.70f);
    }

    private static RelativeSizeSpan getUnitRelativeSizeSpan() {
        return new RelativeSizeSpan(0.65f);
    }
}
