/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.systemui.statusbar.ticker;

import static org.nameless.os.DebugConstants.DEBUG_TICKER;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.android.systemui.Dependency;
import com.android.systemui.plugins.DarkIconDispatcher;
import com.android.systemui.res.R;
import com.android.systemui.statusbar.policy.ClockCenter;

public class MarqueeTickerEx extends MarqueeTicker implements Animation.AnimationListener {

    private static final String TAG = "Ticker::MarqueeTickerEx";

    private final Context mContext;

    private AdvertSwitcherView mSwitcherView;
    private ClockCenter mCenterClockView;
    private View mStatusBarContents;
    private View mTickerView;

    public MarqueeTickerEx(Context context, View sb) {
        super(context, sb);
        mContext = context;
    }

    public void setCenterClockView(ClockCenter centerClockView) {
        mCenterClockView = centerClockView;
    }

    public void setStatusBarContents(View statusBarContents) {
        mStatusBarContents = statusBarContents;
    }

    public void setSwitcherView(AdvertSwitcherView switcherView) {
        mSwitcherView = switcherView;
    }

    public void setTickerView(View tickerView) {
        mTickerView = tickerView;
    }

    @Override
    public void onAnimationEnd(Animation animation) {
        mTicking = false;
    }

    @Override
    public void onAnimationRepeat(Animation animation) {
    }

    @Override
    public void onAnimationStart(Animation animation) {
    }

    @Override
    public void tickerStarting() {
        if (DEBUG_TICKER) {
            Log.d(TAG, "tickerStarting");
        }
        mTicking = true;
        ((DarkIconDispatcher) Dependency.get(DarkIconDispatcher.class)).addDarkReceiver(this);
        mStatusBarContents.setVisibility(View.GONE);
        mTickerView.setVisibility(View.VISIBLE);
        mStatusBarContents.startAnimation(loadAnim(R.anim.marquee_fade_out, null));
        mTickerView.startAnimation(loadAnim(R.anim.marquee_push_down_in, null));
        if (mCenterClockView.isCenterClock()) {
            mCenterClockView.setVisibility(View.GONE);
            mCenterClockView.startAnimation(loadAnim(R.anim.marquee_fade_out, null));
        }
        mCenterClockView.setVisibilityLocked(true);
    }

    @Override
    public void tickerDone() {
        if (DEBUG_TICKER) {
            Log.d(TAG, "tickerDone");
        }
        mTicking = false;
        ((DarkIconDispatcher) Dependency.get(DarkIconDispatcher.class)).removeDarkReceiver(this);
        mStatusBarContents.setVisibility(View.VISIBLE);
        mTickerView.setVisibility(View.GONE);
        mStatusBarContents.startAnimation(loadAnim(R.anim.marquee_fade_in, null));
        mTickerView.startAnimation(loadAnim(R.anim.marquee_push_up_out, this));
        mCenterClockView.setVisibilityLocked(false);
        if (!mSwitcherView.isShow() && mCenterClockView.isCenterClock()) {
            mCenterClockView.setVisibility(View.VISIBLE);
            mCenterClockView.startAnimation(loadAnim(R.anim.marquee_fade_in, null));
        }
    }

    @Override
    public void tickerHalting() {
        if (DEBUG_TICKER) {
            Log.d(TAG, "tickerHalting");
        }
        mTicking = false;
        if (mStatusBarContents.getVisibility() != View.VISIBLE) {
            ((DarkIconDispatcher) Dependency.get(DarkIconDispatcher.class)).removeDarkReceiver(this);
            mStatusBarContents.setVisibility(View.VISIBLE);
            mTickerView.setVisibility(View.GONE);
            mStatusBarContents.startAnimation(loadAnim(android.R.anim.fade_in, null));
            mCenterClockView.setVisibilityLocked(false);
            if (!mSwitcherView.isShow() && mCenterClockView.isCenterClock()) {
                mCenterClockView.setVisibility(View.VISIBLE);
                mCenterClockView.startAnimation(loadAnim(R.anim.marquee_fade_in, null));
            }
        }
    }

    public Animation loadAnim(int id, Animation.AnimationListener listener) {
        final Animation anim = AnimationUtils.loadAnimation(mContext, id);
        if (listener != null) {
            anim.setAnimationListener(listener);
        }
        return anim;
    }
}
