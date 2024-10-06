/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.systemui.statusbar.ticker;

import static android.graphics.PorterDuff.Mode.SRC_IN;

import static org.nameless.os.DebugConstants.DEBUG_TICKER;

import android.app.Notification;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.service.notification.StatusBarNotification;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;

import com.android.systemui.Dependency;
import com.android.systemui.plugins.DarkIconDispatcher;
import com.android.systemui.res.R;

import java.util.ArrayList;

public class AdvertTickerView extends LinearLayout implements DarkIconDispatcher.DarkReceiver {

    private static String TAG = "Ticker::AdvertTickerView";

    private static String KEY_TICKER_ICON = "ticker_icon";
    private static String KEY_TICKER_ICON_SWITCH = "ticker_icon_switch";

    private ImageSwitcher mIconSwitcher;
    private TextSwitcher mTextSwitcher;

    private Drawable mTickerIconDrawable;
    private int mTickIconId;

    private String mCurrentKey;
    private String mCurrentPkg;

    public AdvertTickerView(Context context) {
        super(context);
    }

    public AdvertTickerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AdvertTickerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ((DarkIconDispatcher) Dependency.get(DarkIconDispatcher.class)).addDarkReceiver(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ((DarkIconDispatcher) Dependency.get(DarkIconDispatcher.class)).removeDarkReceiver(this);
    }

    public boolean addNotification(StatusBarNotification sbn) {
        if (sbn == null) {
            return false;
        }
        final Notification notification = sbn.getNotification();
        final boolean show = (notification.flags & Notification.FLAG_ALWAYS_SHOW_TICKER) != 0
                && notification.tickerText != null && !sbn.isClearable();
        if (DEBUG_TICKER) {
            Log.d(TAG, "id=" + sbn.getId() + ", show=" + show
                    + ", tickerText=" + notification.tickerText
                    + ", key=" + sbn.getKey() + ", currentKey=" + mCurrentKey);
        }
        if (mCurrentKey != null && !sbn.getKey().equals(mCurrentKey)) {
            return false;
        }
        if (!show) {
            removeNotification(sbn.getKey());
            return false;
        }

        mCurrentKey = sbn.getKey();
        mCurrentPkg = sbn.getPackageName();
        mTickerIconDrawable = notification.getSmallIcon().loadDrawable(mContext);
        mIconSwitcher.setVisibility(View.VISIBLE);
        mIconSwitcher.setImageDrawable(mTickerIconDrawable);
        mTextSwitcher.setText(notification.tickerText);

        final MarqueeTextView currentTicker =
                (MarqueeTextView) mTextSwitcher.getChildAt(mTextSwitcher.getDisplayedChild());
        if (currentTicker != null) {
            currentTicker.startScrollSoon();
        }
        return true;
    }

    public void removeNotification(String key) {
        if (mCurrentKey != null && mCurrentKey.equals(key)) {
            mCurrentKey = null;
            mTickerIconDrawable = null;
            mIconSwitcher.setImageDrawable(null);
            mIconSwitcher.setVisibility(View.GONE);
            mTextSwitcher.setText(null);
        }
    }

    public boolean isShow() {
        return mCurrentKey != null;
    }

    public String getCurrentPkg() {
        return mCurrentPkg;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mIconSwitcher = (ImageSwitcher) findViewById(R.id.tickerIcon_ext);
        mTextSwitcher = (TextSwitcher) findViewById(R.id.tickerText_ext);
    }

    @Override
    public void onDarkChanged(ArrayList<Rect> areas, float darkIntensity, int tint) {
        final int colorTint = DarkIconDispatcher.getTint(areas, this, tint);
        if (mTextSwitcher != null) {
            for (int i = 0; i < mTextSwitcher.getChildCount(); i++) {
                final TextView child = (TextView) mTextSwitcher.getChildAt(i);
                child.setTextColor(colorTint);
            }
        }
        if (mIconSwitcher != null) {
            for (int i = 0; i < mIconSwitcher.getChildCount(); i++) {
                final ImageView child = (ImageView) mIconSwitcher.getChildAt(i);
                child.setColorFilter(colorTint, SRC_IN);
            }
        }
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        mIconSwitcher.setVisibility(visibility);
        mTextSwitcher.setVisibility(visibility);
    }
}
