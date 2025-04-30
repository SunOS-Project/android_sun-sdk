/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.systemui.statusbar.ticker;

import static org.sun.os.DebugConstants.DEBUG_TICKER;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

import com.android.systemui.res.R;

public class MarqueeTextView extends TextView {

    private static final String TAG = "Ticker::MarqueeTextView";

    private static final int TICKER_START_PHASE = 100;
    private static final int TICKER_END_PHASE = 100;

    private static final int TICKER_INITIAL_SPEED_INTERVAL = 10;
    private static final int TICKER_MAX_SPEED_INTERVAL = 3;

    private static final float TICKER_ACC = 0.07f;

    private static final long TICKER_START_DELAY = 1500L;

    private final Handler mHandler = new Handler();
    private Handler mScrollHandler;
    private HandlerThread mHandlerThread;

    public MarqueeTicker.Segment mSegment;

    private int mCurrentScrollInterval;
    private int mCurrentScrollPosition;
    private int mTextViewWidth;
    private int mTextWidth;

    private boolean mIsMeasured;
    private boolean mIsScrolling;

    private Runnable mScrollTickerRunnable;

    public MarqueeTextView(Context context) {
        this(context, null);
    }

    public MarqueeTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MarqueeTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mScrollTickerRunnable = () -> {
            if (mTextWidth == 0 || mTextViewWidth == 0) {
                getTextWidth();
            }
            if (mTextWidth - mTextViewWidth < 1) {
                mIsScrolling = false;
                return;
            }
            final int endPosition = mTextWidth - mTextViewWidth;
            final int startPhase = TICKER_START_PHASE;
            final int endPhase = endPosition - TICKER_END_PHASE;
            if (endPosition < TICKER_START_PHASE + TICKER_END_PHASE) {
                mCurrentScrollInterval = TICKER_INITIAL_SPEED_INTERVAL;
            } else if (getScrollX() < startPhase) {
                mCurrentScrollInterval = (int) (TICKER_INITIAL_SPEED_INTERVAL - (getScrollX() * TICKER_ACC));
            } else if (getScrollX() > endPhase) {
                mCurrentScrollInterval = (int) ((getScrollX() - endPhase) * TICKER_ACC + TICKER_MAX_SPEED_INTERVAL);
            } else {
                mCurrentScrollInterval = TICKER_MAX_SPEED_INTERVAL;
            }
            mCurrentScrollPosition++;
            if (DEBUG_TICKER) {
                Log.d(TAG, "mCurrentScrollInterval=" + mCurrentScrollInterval
                        + ", mCurrentScrollPosition=" + mCurrentScrollPosition
                        + ", mIsScrolling=" + mIsScrolling);
            }
            if (!mIsScrolling) {
                return;
            }
            if (getScrollX() > endPosition) {
                mIsScrolling = false;
            }
            mHandler.postAtFrontOfQueue(() -> scrollTo(mCurrentScrollPosition, 0));
            if (mScrollHandler != null) {
                mScrollHandler.postDelayed(mScrollTickerRunnable, mCurrentScrollInterval);
            }
        };
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!mIsMeasured) {
            getTextWidth();
            mIsMeasured = true;
        }
    }

    public void getTextWidth() {
        mTextWidth = (int) getPaint().measureText(getText().toString());
        mTextViewWidth = getWidth();
        if (DEBUG_TICKER) {
            Log.d(TAG, "getTextWidth, mTextWidth=" + mTextWidth + ", mTextViewWidth=" + mTextViewWidth);
        }
    }

    @Override
    public void setText(CharSequence text, TextView.BufferType type) {
        super.setText(text, type);
        if (DEBUG_TICKER) {
            Log.d(TAG, "setText, text=" + text);
        }
        mIsMeasured = false;
        getTextWidth();
        setEllipsize(null);
        setHorizontalFadingEdgeEnabled(true);
    }

    public void startScrollSoon() {
        mCurrentScrollPosition = 0;
        mIsScrolling = true;
        if (mScrollHandler != null) {
            mScrollHandler.removeCallbacks(mScrollTickerRunnable);
            mScrollHandler.postDelayed(mScrollTickerRunnable, TICKER_START_DELAY);
        }
    }

    @Override
    public boolean isFocused() {
        return true;
    }

    public boolean isRunning() {
        return mIsScrolling;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mHandlerThread == null) {
            mHandlerThread = new HandlerThread(TAG);
            mHandlerThread.start();
        }
        if (mScrollHandler == null) {
            mScrollHandler = new Handler(mHandlerThread.getLooper());
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mHandlerThread.quitSafely();
        mScrollHandler = null;
        mHandlerThread = null;
    }
}
