/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.systemui.statusbar.ticker;

import static android.graphics.PorterDuff.Mode.SRC_IN;

import static org.nameless.os.DebugConstants.DEBUG_TICKER;
import static org.nameless.view.DisplayResolutionManager.FHD_WIDTH;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.TextSwitcher;

import com.android.internal.util.CharSequences;

import com.android.systemui.plugins.DarkIconDispatcher;
import com.android.systemui.res.R;

import java.util.ArrayList;
import java.util.List;

public abstract class MarqueeTicker implements DarkIconDispatcher.DarkReceiver {

    private static final String TAG = "Ticker::MarqueeTicker";

    private static final long TICKER_END_DELAY = 1500L;
    private static final long TICKER_RUNNING_CHECK_DELAY = 100L;
    private static final long TICKER_SHOW_TIME = 5000L;

    private static final int LEFT_FULL_LOOP_COUNT = 10;

    private static final float TICKER_HOLE_SPACE = 2.0f;

    private final Handler mHandler = new Handler();
    private final Paint mPaint = new Paint();
    private final Rect mRect = new Rect();

    private final Context mContext;

    public ImageSwitcher mIconSwitcher;
    public TextSwitcher mTextSwitcher;

    private int mLeftPadding;
    private int mLeftSpace;
    private int mPorDisplayCutoutLeft;
    private int mPorDisplayCutoutRight;
    private int mPorScreenWidth = FHD_WIDTH;
    private int mHorDisplayCutoutLeft;
    private int mHorDisplayCutoutRight;
    private int mHorScreenWidth;

    private MarqueeTextView mCurrentTicker;
    private int mCurrentColor;
    private int mSymbolLength;

    private boolean mInDisplay = false;
    private boolean mIsLeftFull = false;
    private boolean mIsPortrait = true;
    private boolean mIsCurved = false;
    private boolean mIsCenterDisplayCutout = false;

    protected boolean mTicking;

    private final StringBuilder mToShowContentString = new StringBuilder();
    private final StringBuilder mTempString = new StringBuilder();

    private final ArrayList<Segment> mSegments = new ArrayList<>();
    private final List<String> mTickerList = new ArrayList();

    private List<String> mUnfinishTickerList;

    private final Runnable mShowUnfinishTicker = () -> showNextTicker();
    private final Runnable mHideLastTicker = () -> tickerDone();
    private final Runnable mAdvanceTicker = () -> {
        if (mCurrentTicker.isRunning()) {
            if (!mCurrentTicker.mSegment.isRemoved) {
                scheduleAdvance();
            } else {
                skipToNextTicker();
            }
        } else {
            scheduleAdvanceToNextTicker();
        }
    };
    private final Runnable mAdvanceToNextTicker = () -> {
        if (mSegments.size() > 0) {
            mSegments.remove(0);
        }
        if (mSegments.size() > 0) {
            final Segment seg = (Segment) mSegments.get(0);
            if (DEBUG_TICKER) {
                Log.d(TAG, "AdvanceToNextTicker, seg=" + seg);
            }
            if (!seg.isRemoved) {
                mIconSwitcher.setImageDrawable(seg.icon);
                mTextSwitcher.setText(seg.text);
                mCurrentTicker = (MarqueeTextView) mTextSwitcher.getChildAt(mTextSwitcher.getDisplayedChild());
                mCurrentTicker.mSegment = seg;
                mCurrentTicker.startScrollSoon();
            }
            scheduleAdvance();
        }
        if (mSegments.size() == 0) {
            tickerDone();
        }
    };

    public abstract void tickerDone();
    public abstract void tickerHalting();
    public abstract void tickerStarting();

    public final class Segment {
        StatusBarNotification notification;
        Drawable icon;
        CharSequence text;
        boolean isRemoved = false;

        Segment(StatusBarNotification n, Drawable icon, CharSequence text) {
            this.notification = n;
            this.icon = icon;
            this.text = text;
        }

        @Override
        public String toString() {
            return "{text=" + text + ", isRemoved=" + isRemoved + "}";
        }
    }

    public MarqueeTicker(Context context, View sb) {
        mContext = context;

        mIconSwitcher = (ImageSwitcher) sb.findViewById(R.id.tickerIcon);
        mTextSwitcher = (TextSwitcher) sb.findViewById(R.id.tickerText);

        final Animation inAnimation = AnimationUtils.loadAnimation(context, R.anim.marquee_push_down_in);
        mIconSwitcher.setInAnimation(inAnimation);
        mTextSwitcher.setInAnimation(inAnimation);
    }

    public boolean isTicking() {
        return mTicking;
    }

    public void addEntry(StatusBarNotification n) {
        final int initialCount = mSegments.size();
        if (initialCount > 0) {
            final Segment seg = mSegments.get(0);
            if (n.getPackageName().equals(seg.notification.getPackageName()) &&
                    n.getNotification().icon == seg.notification.getNotification().icon &&
                    n.getNotification().iconLevel == seg.notification.getNotification().iconLevel &&
                    CharSequences.equals(seg.notification.getNotification().tickerText, n.getNotification().tickerText)) {
                return;
            }
        }

        final Drawable tickerIcon = n.getNotification().getSmallIcon().loadDrawable(mContext);
        final CharSequence text = n.getNotification().tickerText;
        final Segment newSegment = new Segment(n, tickerIcon, text);
        if (DEBUG_TICKER) {
            Log.d(TAG, "addEntry, newSegment=" + newSegment);
        }
        int i = 0;
        while (i < mSegments.size()) {
            final Segment seg = mSegments.get(i);
            if (n.getId() == seg.notification.getId() &&
                    n.getPackageName().equals(seg.notification.getPackageName())) {
                mSegments.remove(i);
                i--;
            }
            i++;
        }
        mSegments.add(newSegment);
        if (initialCount > 0 || mSegments.size() == 0) {
            return;
        }

        final Segment seg = mSegments.get(0);
        mIconSwitcher.setAnimateFirstView(false);
        mIconSwitcher.reset();
        ((ImageView) mIconSwitcher.getCurrentView()).setImageDrawable(null);
        mIconSwitcher.setImageDrawable(seg.icon);
        mTextSwitcher.setAnimateFirstView(false);
        mTextSwitcher.reset();
        mCurrentTicker = (MarqueeTextView) mTextSwitcher.getChildAt(mTextSwitcher.getDisplayedChild());
        mCurrentTicker.mSegment = seg;
        if (mCurrentTicker != null) {
            mCurrentTicker.setTextColor(mCurrentColor);
        }
        if (mIconSwitcher != null) {
            mIconSwitcher.setVisibility(View.VISIBLE);
            ((ImageView) mIconSwitcher.getCurrentView()).setColorFilter(mCurrentColor, SRC_IN);
        }
        if (mIsCurved && mIsCenterDisplayCutout) {
            initSymbolLength();
            mIconSwitcher.post(() -> setNewData(seg.text.toString()));
            tickerStarting();
        } else {
            mTextSwitcher.setCurrentText(seg.text);
            mCurrentTicker.setVisibility(View.VISIBLE);
            mCurrentTicker.startScrollSoon();
            mHandler.removeCallbacks(mAdvanceToNextTicker);
            tickerStarting();
            scheduleAdvance();
        }
    }

    public void setNewData(String text) {
        mTickerList.clear();
        for (int i = 0; i < text.length(); i++) {
            mTickerList.add(text.substring(i, i + 1));
        }
        mHandler.removeCallbacks(mShowUnfinishTicker);
        mHandler.removeCallbacks(mHideLastTicker);
        fillTickerContent(mTickerList);
    }

    public void fillTickerContent(List<String> tickerList) {
        mToShowContentString.setLength(0);
        mTempString.setLength(0);
        int iconWidth;
        int screenWidth;
        int displayCutoutLeft;
        int displayCutoutRight;
        mIsLeftFull = false;
        if (mIconSwitcher.getVisibility() == View.VISIBLE) {
            iconWidth = mIconSwitcher.getWidth();
        } else {
            iconWidth = 0;
        }
        if (mIsPortrait) {
            screenWidth = mPorScreenWidth - iconWidth;
            displayCutoutLeft = (mPorDisplayCutoutLeft - iconWidth) - dip2px(TICKER_HOLE_SPACE);
            displayCutoutRight = (mPorDisplayCutoutRight - iconWidth) + dip2px(TICKER_HOLE_SPACE);
        } else {
            screenWidth = mHorScreenWidth - iconWidth;
            if (mIsCurved) {
                displayCutoutLeft = -1;
                displayCutoutRight = -1;
            } else {
                displayCutoutLeft = (mHorDisplayCutoutLeft - iconWidth) - dip2px(TICKER_HOLE_SPACE);
                displayCutoutRight = (mHorDisplayCutoutRight - iconWidth) - dip2px(TICKER_HOLE_SPACE);
            }
        }

        for (int i = 0; i < tickerList.size(); i++) {
            if (mRect.width() < displayCutoutLeft && !mIsLeftFull) {
                mTempString.append(tickerList.get(i));
                mPaint.getTextBounds(mTempString.toString(), 0, mTempString.toString().length(), mRect);
                if (mRect.width() >= displayCutoutLeft) {
                    mIsLeftFull = true;
                    mPaint.getTextBounds(mToShowContentString.toString(), 0, mToShowContentString.toString().length(), mRect);
                    mLeftSpace = displayCutoutLeft - mRect.width();
                } else {
                    mToShowContentString.setLength(0);
                    mToShowContentString.append(mTempString.toString());
                }
            } else if (mRect.width() >= displayCutoutRight) {
                mTempString.append(tickerList.get(i));
                mPaint.getTextBounds(mTempString.toString(), 0, mTempString.toString().length(), mRect);
                if (mRect.width() > screenWidth) {
                    mTextSwitcher.setText(mToShowContentString.toString());
                    mUnfinishTickerList = tickerList.subList(i, tickerList.size());
                    mHandler.postDelayed(mShowUnfinishTicker, TICKER_SHOW_TIME);
                    return;
                }
                mToShowContentString.setLength(0);
                mToShowContentString.append((CharSequence) mTempString);
            } else if (mIsLeftFull) {
                mInDisplay = true;
                final int shouldShowTickerRight = mLeftSpace + displayCutoutRight;
                int loopCnts = LEFT_FULL_LOOP_COUNT;
                while (mInDisplay) {
                    mToShowContentString.append(" ").append("!");
                    final int lastWidth = mRect.width();
                    mPaint.getTextBounds(mToShowContentString.toString(), 0, mToShowContentString.toString().length(), mRect);
                    mToShowContentString.deleteCharAt(mToShowContentString.length() - 1);
                    if (mRect.width() >= shouldShowTickerRight || loopCnts <= 0) {
                        if (loopCnts <= 0) {
                            Log.w(TAG, "oops, loopCnts max, skip the loop!");
                        }
                        mToShowContentString.append(tickerList.get(i));
                        mInDisplay = false;
                        mTempString.setLength(0);
                        mTempString.append((CharSequence) mToShowContentString);
                    } else if (mRect.width() <= lastWidth) {
                        loopCnts--;
                    }
                }
            }
            if (i == tickerList.size() - 1) {
                mTextSwitcher.setText(mTempString);
                mHandler.postDelayed(mHideLastTicker, TICKER_SHOW_TIME);
            }
        }
    }

    private void initSymbolLength() {
        mTextSwitcher.setText("!");
        mPaint.setTextSize(((MarqueeTextView) mTextSwitcher.getChildAt(mTextSwitcher.getDisplayedChild())).getTextSize());
        mPaint.getTextBounds("!", 0, "!".length(), mRect);
        mSymbolLength = mRect.width();
    }

    public void showNextTicker() {
        mIconSwitcher.post(() -> {
            mIconSwitcher.setVisibility(View.GONE);
            fillTickerContent(mUnfinishTickerList);
        });
        if (mCurrentTicker != null) {
            mCurrentTicker.setTextColor(mCurrentColor);
        }
    }

    public int dip2px(float dpValue) {
        final float scale = mContext.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public void scheduleAdvance() {
        mHandler.postDelayed(mAdvanceTicker, TICKER_RUNNING_CHECK_DELAY);
    }

    public void scheduleAdvanceToNextTicker() {
        mHandler.postDelayed(mAdvanceToNextTicker, TICKER_END_DELAY);
    }

    public void skipToNextTicker() {
        mHandler.post(mAdvanceToNextTicker);
    }

    public void removeEntry(StatusBarNotification n) {
        for (int i = mSegments.size() - 1; i >= 0; i--) {
            final Segment seg = mSegments.get(i);
            if (n.getId() == seg.notification.getId() &&
                    n.getPackageName().equals(seg.notification.getPackageName())) {
                mSegments.get(i).isRemoved = true;
                if (i == 0 && mIsCurved && mIsCenterDisplayCutout) {
                    halt();
                }
            }
        }
    }

    public void halt() {
        mHandler.removeCallbacks(mAdvanceTicker);
        mHandler.removeCallbacks(mShowUnfinishTicker);
        mHandler.removeCallbacks(mHideLastTicker);
        mSegments.clear();
        tickerHalting();
    }

    public void reflowText() {
        if (!mIsCurved && mSegments.size() > 0) {
            final Segment seg = mSegments.get(0);
            mTextSwitcher.setCurrentText(seg.text);
        }
    }

    @Override
    public void onDarkChanged(ArrayList<Rect> areas, float darkIntensity, int tint) {
        if (mCurrentTicker != null) {
            int colorTint = DarkIconDispatcher.getTint(areas, mCurrentTicker, tint);
            mCurrentColor = colorTint;
            mCurrentTicker.setTextColor(mCurrentColor);
        }
        if (mIconSwitcher != null && mIconSwitcher.getCurrentView() != null) {
            ((ImageView) mIconSwitcher.getCurrentView()).setColorFilter(mCurrentColor, SRC_IN);
        }
    }

    public void setDisplayCutout(boolean isCenterDisplayCutout, int disLeft, int disRight,
            int screenWidth, boolean isPortrait, boolean isCurved, int leftPadding) {
        mIsPortrait = isPortrait;
        mIsCurved = isCurved;
        mIsCenterDisplayCutout = isCenterDisplayCutout;
        mLeftPadding = leftPadding;
        if (isCurved) {
            if (isPortrait) {
                mPorDisplayCutoutLeft = disLeft;
                mPorDisplayCutoutRight = disRight;
                mPorScreenWidth = screenWidth;
            } else {
                mHorDisplayCutoutLeft = 0;
                mHorDisplayCutoutRight = 0;
                mHorScreenWidth = screenWidth;
            }
        }
        if (DEBUG_TICKER) {
            Log.d(TAG, "setDisplayCutout, mIsPortrait=" + mIsPortrait
                    + ", mIsCurved=" + mIsCurved
                    + ", mIsCenterDisplayCutout=" + mIsCenterDisplayCutout
                    + ", mLeftPadding=" + mLeftPadding
                    + ", mPorDisplayCutoutLeft=" + mPorDisplayCutoutLeft
                    + ", mPorDisplayCutoutRight=" + mPorDisplayCutoutRight
                    + ", mPorScreenWidth=" + mPorScreenWidth
                    + ", mHorDisplayCutoutLeft=" + mHorDisplayCutoutLeft
                    + ", mHorDisplayCutoutRight=" + mHorDisplayCutoutRight
                    + ", mHorScreenWidth=" + mHorScreenWidth);
        }
    }
}
