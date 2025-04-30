/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.systemui.statusbar.ticker;

import static org.sun.os.DebugConstants.DEBUG_TICKER;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextSwitcher;
import android.widget.TextView;

import com.android.systemui.Dependency;
import com.android.systemui.plugins.DarkIconDispatcher;

import java.util.ArrayList;
import java.util.Objects;

public class MarqueeTickerView extends TextSwitcher implements DarkIconDispatcher.DarkReceiver {

    private static final String TAG = "Ticker::MarqueeTickerView";

    private static final long MIN_REPEAT_INTERVAL = 50L;

    private CharSequence mLastText;
    private long mLastTextSetTime;

    protected MarqueeTicker mMarqueeTicker;

    public MarqueeTickerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (DEBUG_TICKER) {
            Log.d(TAG, "onSizeChanged, w=" + w + ", h=" + h);
        }
        if (mMarqueeTicker != null) {
            mMarqueeTicker.reflowText();
        }
    }

    public void setTicker(MarqueeTicker t) {
        mMarqueeTicker = t;
    }

    public MarqueeTicker getTicker() {
        return mMarqueeTicker;
    }

    @Override
    public void onDarkChanged(ArrayList<Rect> areas, float darkIntensity, int tint) {
        final int colorTint = DarkIconDispatcher.getTint(areas, this, tint);
        for (int i = 0; i < getChildCount(); i++) {
            final TextView tv = (TextView) getChildAt(i);
            tv.setTextColor(colorTint);
        }
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

    @Override
    public void setText(CharSequence text) {
        final long curTime = System.currentTimeMillis();
        if (Objects.equals(mLastText, text) && curTime - mLastTextSetTime <= MIN_REPEAT_INTERVAL) {
            if (DEBUG_TICKER) {
                Log.d(TAG, "ignore duplicate setText() invoke, text=" + text);
            }
            return;
        }
        super.setText(text);
        mLastTextSetTime = curTime;
        mLastText = text;
    }

    @Override
    public void setCurrentText(CharSequence text) {
        super.setCurrentText(text);
        if (DEBUG_TICKER) {
            Log.d(TAG, "setCurrentText, text=" + text);
        }
        mLastText = text;
    }
}
