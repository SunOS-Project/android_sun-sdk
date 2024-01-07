/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.navigationbar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Property;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;

import com.android.app.animation.Interpolators;

import com.android.systemui.R;
import com.android.systemui.Dependency;
import com.android.systemui.recents.OverviewProxyService;
import com.android.systemui.shared.system.QuickStepContract;
import com.android.systemui.navigationbar.buttons.ButtonInterface;
import com.android.systemui.navigationbar.buttons.KeyButtonDrawable;
import com.android.systemui.navigationbar.buttons.KeyButtonView;

import java.util.ArrayList;

public class OpaLayout extends FrameLayout implements ButtonInterface {

    private final Interpolator mHomeDisappearInterpolator = new PathInterpolator(0.65f, 0.0f, 1.0f, 1.0f);
    private final Interpolator mDiamondInterpolator = new PathInterpolator(0.2f, 0.0f, 0.2f, 1.0f);

    private final ArrayList<View> mAnimatedViews = new ArrayList<>();
    private final ArraySet<Animator> mCurrentAnimators = new ArraySet<>();

    private final Runnable mDiamondAnimation = () -> {
        if (mCurrentAnimators.isEmpty()) {
            startDiamondAnimation();
        }
    };
    private final Runnable mRetract = () -> {
        cancelCurrentAnimation();
        startRetractAnimation();
        hideAllOpa();
    };

    private final OverviewProxyService.OverviewProxyListener mOverviewProxyListener =
            new OverviewProxyService.OverviewProxyListener() {
                @Override
                public void onConnectionChanged(boolean isConnected) {
                    updateOpaLayout();
                }
            };

    private AnimatorSet mGestureAnimatorSet;
    private AnimatorSet mGestureLineSet;
    private OverviewProxyService mOverviewProxyService;
    private Resources mResources;

    private KeyButtonView mHome;
    private ImageView mWhite;
    private View mBlue;
    private View mGreen;
    private View mRed;
    private View mYellow;

    private View mBottom;
    private View mLeft;
    private View mRight;
    private View mTop;

    private long mGestureAnimationSetDuration;
    private long mStartTime;
    private int mTouchDownX;
    private int mTouchDownY;

    private int mHomeDiameter;

    private boolean mDelayTouchFeedback;
    private boolean mDiamondAnimationDelayed;
    private boolean mIsPressed;
    private boolean mIsVertical;
    private boolean mWindowVisible;

    private int mAnimationState = 0;
    private int mGestureState = 0;

    public OpaLayout(Context context) {
        this(context, null);
    }

    public OpaLayout(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public OpaLayout(Context context, AttributeSet attributeSet, int defStyleAttr) {
        this(context, attributeSet, defStyleAttr, 0);
    }

    public OpaLayout(Context context, AttributeSet attributeSet, int defStyleAttr, int defStyleRes) {
        super(context, attributeSet, defStyleAttr, defStyleRes);
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        mResources = getResources();
        mBlue = findViewById(R.id.blue);
        mRed = findViewById(R.id.red);
        mYellow = findViewById(R.id.yellow);
        mGreen = findViewById(R.id.green);
        mWhite = (ImageView) findViewById(R.id.white);
        mHome = (KeyButtonView) findViewById(R.id.home_button);
        mHomeDiameter = mResources.getDimensionPixelSize(R.dimen.opa_disabled_home_diameter);

        mAnimatedViews.add(mBlue);
        mAnimatedViews.add(mRed);
        mAnimatedViews.add(mYellow);
        mAnimatedViews.add(mGreen);
        mAnimatedViews.add(mWhite);

        mOverviewProxyService = (OverviewProxyService) Dependency.get(OverviewProxyService.class);
        hideAllOpa();
    }

    @Override
    public void onWindowVisibilityChanged(int visible) {
        super.onWindowVisibilityChanged(visible);
        mWindowVisible = visible == View.VISIBLE;
        if (mWindowVisible) {
            updateOpaLayout();
            return;
        }
        cancelCurrentAnimation();
        skipToStartingValue();
    }

    @Override
    public void setOnLongClickListener(View.OnLongClickListener onLongClickListener) {
        mHome.setOnLongClickListener(onLongClickListener);
    }

    @Override
    public void setOnTouchListener(View.OnTouchListener onTouchListener) {
        mHome.setOnTouchListener(onTouchListener);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        boolean z;
        if (!ValueAnimator.areAnimatorsEnabled() || mGestureState != 0) {
            return false;
        }
        final int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mTouchDownX = (int) event.getRawX();
                mTouchDownY = (int) event.getRawY();
                final boolean touchDown = !mCurrentAnimators.isEmpty();
                if (touchDown) {
                    endCurrentAnimation();
                }
                mStartTime = SystemClock.elapsedRealtime();
                mIsPressed = true;
                removeCallbacks(mDiamondAnimation);
                removeCallbacks(mRetract);
                if (!mDelayTouchFeedback || touchDown) {
                    mDiamondAnimationDelayed = false;
                    startDiamondAnimation();
                } else {
                    mDiamondAnimationDelayed = true;
                    postDelayed(mDiamondAnimation, (long) ViewConfiguration.getTapTimeout());
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mDiamondAnimationDelayed) {
                    if (mIsPressed) {
                        postDelayed(mRetract, 200L);
                    }
                } else if (mAnimationState == 1) {
                    removeCallbacks(mRetract);
                    postDelayed(mRetract, 100L - (SystemClock.elapsedRealtime() - mStartTime));
                    removeCallbacks(mDiamondAnimation);
                    cancelLongPress();
                    break;
                } else if (mIsPressed) {
                    mRetract.run();
                }
                mIsPressed = false;
                break;
            case MotionEvent.ACTION_MOVE:
                final float quickStepTouchSlopPx = QuickStepContract.getQuickStepTouchSlopPx(getContext());
                if (Math.abs(event.getRawX() - mTouchDownX) > quickStepTouchSlopPx ||
                        Math.abs(event.getRawY() - mTouchDownY) > quickStepTouchSlopPx) {
                    abortCurrentGesture();
                }
                break;
        }
        return false;
    }

    @Override
    public void setAccessibilityDelegate(View.AccessibilityDelegate accessibilityDelegate) {
        super.setAccessibilityDelegate(accessibilityDelegate);
        mHome.setAccessibilityDelegate(accessibilityDelegate);
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        mWhite.setImageDrawable(drawable);
    }

    @Override
    public void abortCurrentGesture() {
        mHome.abortCurrentGesture();
        mIsPressed = false;
        mDiamondAnimationDelayed = false;
        removeCallbacks(mDiamondAnimation);
        cancelLongPress();
        if (mAnimationState == 1 || mAnimationState == 3) {
            mRetract.run();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        updateOpaLayout();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        mOverviewProxyService.addCallback(mOverviewProxyListener);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mOverviewProxyService.removeCallback(mOverviewProxyListener);
    }

    private void startDiamondAnimation() {
        if (allowAnimations()) {
            mCurrentAnimators.clear();
            setDotsVisible();
            mCurrentAnimators.addAll(getDiamondAnimatorSet());
            mAnimationState = 1;
            startAll(mCurrentAnimators);
            return;
        }
        skipToStartingValue();
    }

    private void startRetractAnimation() {
        if (allowAnimations()) {
            mCurrentAnimators.clear();
            mCurrentAnimators.addAll(getRetractAnimatorSet());
            mAnimationState = 2;
            startAll(mCurrentAnimators);
            return;
        }
        skipToStartingValue();
    }

    private void startLineAnimation() {
        if (allowAnimations()) {
            mCurrentAnimators.clear();
            mCurrentAnimators.addAll(getLineAnimatorSet());
            mAnimationState = 3;
            startAll(mCurrentAnimators);
            return;
        }
        skipToStartingValue();
    }

    private void startCollapseAnimation() {
        if (allowAnimations()) {
            mCurrentAnimators.clear();
            mCurrentAnimators.addAll(getCollapseAnimatorSet());
            mAnimationState = 3;
            startAll(mCurrentAnimators);
            return;
        }
        skipToStartingValue();
    }

    private void startAll(ArraySet<Animator> animatorSet) {
        showAllOpa();
        for (int i = animatorSet.size() - 1; i >= 0; i--) {
            animatorSet.valueAt(i).start();
        }
        for (int i = mAnimatedViews.size() - 1; i >= 0; i--) {
            mAnimatedViews.get(i).invalidate();
        }
    }

    private boolean allowAnimations() {
        return isAttachedToWindow() && mWindowVisible;
    }

    private ArraySet<Animator> getDiamondAnimatorSet() {
        final ArraySet<Animator> animatorSet = new ArraySet<>();
        animatorSet.add(getPropertyAnimator(mTop, View.Y, (-OpaUtils.getPxVal(mResources,
                R.dimen.opa_diamond_translation)) + mTop.getY(), 200L, mDiamondInterpolator));
        animatorSet.add(getPropertyAnimator(mTop, FrameLayout.SCALE_X, 0.8f, 200L, Interpolators.FAST_OUT_SLOW_IN));
        animatorSet.add(getPropertyAnimator(mTop, FrameLayout.SCALE_Y, 0.8f, 200L, Interpolators.FAST_OUT_SLOW_IN));
        animatorSet.add(getPropertyAnimator(mBottom, View.Y, mBottom.getY() + OpaUtils.getPxVal(mResources,
                R.dimen.opa_diamond_translation), 200L, mDiamondInterpolator));
        animatorSet.add(getPropertyAnimator(mBottom, FrameLayout.SCALE_X, 0.8f, 200L, Interpolators.FAST_OUT_SLOW_IN));
        animatorSet.add(getPropertyAnimator(mBottom, FrameLayout.SCALE_Y, 0.8f, 200L, Interpolators.FAST_OUT_SLOW_IN));
        animatorSet.add(getPropertyAnimator(mLeft, View.X, mLeft.getX() + (-OpaUtils.getPxVal(mResources,
                R.dimen.opa_diamond_translation)), 200L, mDiamondInterpolator));
        animatorSet.add(getPropertyAnimator(mLeft, FrameLayout.SCALE_X, 0.8f, 200L, Interpolators.FAST_OUT_SLOW_IN));
        animatorSet.add(getPropertyAnimator(mLeft, FrameLayout.SCALE_Y, 0.8f, 200L, Interpolators.FAST_OUT_SLOW_IN));
        animatorSet.add(getPropertyAnimator(mRight, View.X, mRight.getX() + OpaUtils.getPxVal(mResources,
                R.dimen.opa_diamond_translation), 200L, mDiamondInterpolator));
        animatorSet.add(getPropertyAnimator(mRight, FrameLayout.SCALE_X, 0.8f, 200L, Interpolators.FAST_OUT_SLOW_IN));
        animatorSet.add(getPropertyAnimator(mRight, FrameLayout.SCALE_Y, 0.8f, 200L, Interpolators.FAST_OUT_SLOW_IN));
        animatorSet.add(getPropertyAnimator(mWhite, FrameLayout.SCALE_X, 0.625f, 200L, Interpolators.FAST_OUT_SLOW_IN));
        animatorSet.add(getPropertyAnimator(mWhite, FrameLayout.SCALE_Y, 0.625f, 200L, Interpolators.FAST_OUT_SLOW_IN));
        OpaUtils.getLongestAnim(animatorSet).addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animator) {
                mCurrentAnimators.clear();
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                startLineAnimation();
            }
        });
        return animatorSet;
    }

    private ArraySet<Animator> getRetractAnimatorSet() {
        final ArraySet<Animator> animatorSet = new ArraySet<>();
        animatorSet.add(getPropertyAnimator(mRed, FrameLayout.TRANSLATION_X, 0.0f, 190L, OpaUtils.INTERPOLATOR_40_OUT));
        animatorSet.add(getPropertyAnimator(mRed, FrameLayout.TRANSLATION_Y, 0.0f, 190L, OpaUtils.INTERPOLATOR_40_OUT));
        animatorSet.add(getPropertyAnimator(mRed, FrameLayout.SCALE_X, 1.0f, 190L, OpaUtils.INTERPOLATOR_40_OUT));
        animatorSet.add(getPropertyAnimator(mRed, FrameLayout.SCALE_Y, 1.0f, 190L, OpaUtils.INTERPOLATOR_40_OUT));
        animatorSet.add(getPropertyAnimator(mBlue, FrameLayout.TRANSLATION_X, 0.0f, 190L, OpaUtils.INTERPOLATOR_40_OUT));
        animatorSet.add(getPropertyAnimator(mBlue, FrameLayout.TRANSLATION_Y, 0.0f, 190L, OpaUtils.INTERPOLATOR_40_OUT));
        animatorSet.add(getPropertyAnimator(mBlue, FrameLayout.SCALE_X, 1.0f, 190L, OpaUtils.INTERPOLATOR_40_OUT));
        animatorSet.add(getPropertyAnimator(mBlue, FrameLayout.SCALE_Y, 1.0f, 190L, OpaUtils.INTERPOLATOR_40_OUT));
        animatorSet.add(getPropertyAnimator(mGreen, FrameLayout.TRANSLATION_X, 0.0f, 190L, OpaUtils.INTERPOLATOR_40_OUT));
        animatorSet.add(getPropertyAnimator(mGreen, FrameLayout.TRANSLATION_Y, 0.0f, 190L, OpaUtils.INTERPOLATOR_40_OUT));
        animatorSet.add(getPropertyAnimator(mGreen, FrameLayout.SCALE_X, 1.0f, 190L, OpaUtils.INTERPOLATOR_40_OUT));
        animatorSet.add(getPropertyAnimator(mGreen, FrameLayout.SCALE_Y, 1.0f, 190L, OpaUtils.INTERPOLATOR_40_OUT));
        animatorSet.add(getPropertyAnimator(mYellow, FrameLayout.TRANSLATION_X, 0.0f, 190L, OpaUtils.INTERPOLATOR_40_OUT));
        animatorSet.add(getPropertyAnimator(mYellow, FrameLayout.TRANSLATION_Y, 0.0f, 190L, OpaUtils.INTERPOLATOR_40_OUT));
        animatorSet.add(getPropertyAnimator(mYellow, FrameLayout.SCALE_X, 1.0f, 190L, OpaUtils.INTERPOLATOR_40_OUT));
        animatorSet.add(getPropertyAnimator(mYellow, FrameLayout.SCALE_Y, 1.0f, 190L, OpaUtils.INTERPOLATOR_40_OUT));
        animatorSet.add(getPropertyAnimator(mWhite, FrameLayout.SCALE_X, 1.0f, 190L, OpaUtils.INTERPOLATOR_40_OUT));
        animatorSet.add(getPropertyAnimator(mWhite, FrameLayout.SCALE_Y, 1.0f, 190L, OpaUtils.INTERPOLATOR_40_OUT));
        OpaUtils.getLongestAnim(animatorSet).addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                mCurrentAnimators.clear();
                skipToStartingValue();
            }
        });
        return animatorSet;
    }

    private ArraySet<Animator> getCollapseAnimatorSet() {
        final ArraySet<Animator> animatorSet = new ArraySet<>();
        if (mIsVertical) {
            animatorSet.add(getPropertyAnimator(mRed, FrameLayout.TRANSLATION_Y, 0.0f, 133L, OpaUtils.INTERPOLATOR_40_OUT));
        } else {
            animatorSet.add(getPropertyAnimator(mRed, FrameLayout.TRANSLATION_X, 0.0f, 133L, OpaUtils.INTERPOLATOR_40_OUT));
        }
        animatorSet.add(getPropertyAnimator(mRed, FrameLayout.SCALE_X, 1.0f, 200L, OpaUtils.INTERPOLATOR_40_OUT));
        animatorSet.add(getPropertyAnimator(mRed, FrameLayout.SCALE_Y, 1.0f, 200L, OpaUtils.INTERPOLATOR_40_OUT));
        if (mIsVertical) {
            animatorSet.add(getPropertyAnimator(mBlue, FrameLayout.TRANSLATION_Y, 0.0f, 150L, OpaUtils.INTERPOLATOR_40_OUT));
        } else {
            animatorSet.add(getPropertyAnimator(mBlue, FrameLayout.TRANSLATION_X, 0.0f, 150L, OpaUtils.INTERPOLATOR_40_OUT));
        }
        animatorSet.add(getPropertyAnimator(mBlue, FrameLayout.SCALE_X, 1.0f, 200L, OpaUtils.INTERPOLATOR_40_OUT));
        animatorSet.add(getPropertyAnimator(mBlue, FrameLayout.SCALE_Y, 1.0f, 200L, OpaUtils.INTERPOLATOR_40_OUT));
        if (mIsVertical) {
            animatorSet.add(getPropertyAnimator(mYellow, FrameLayout.TRANSLATION_Y, 0.0f, 133L, OpaUtils.INTERPOLATOR_40_OUT));
        } else {
            animatorSet.add(getPropertyAnimator(mYellow, FrameLayout.TRANSLATION_X, 0.0f, 133L, OpaUtils.INTERPOLATOR_40_OUT));
        }
        animatorSet.add(getPropertyAnimator(mYellow, FrameLayout.SCALE_X, 1.0f, 200L, OpaUtils.INTERPOLATOR_40_OUT));
        animatorSet.add(getPropertyAnimator(mYellow, FrameLayout.SCALE_Y, 1.0f, 200L, OpaUtils.INTERPOLATOR_40_OUT));
        if (mIsVertical) {
            animatorSet.add(getPropertyAnimator(mGreen, FrameLayout.TRANSLATION_Y, 0.0f, 150L, OpaUtils.INTERPOLATOR_40_OUT));
        } else {
            animatorSet.add(getPropertyAnimator(mGreen, FrameLayout.TRANSLATION_X, 0.0f, 150L, OpaUtils.INTERPOLATOR_40_OUT));
        }
        animatorSet.add(getPropertyAnimator(mGreen, FrameLayout.SCALE_X, 1.0f, 200L, OpaUtils.INTERPOLATOR_40_OUT));
        animatorSet.add(getPropertyAnimator(mGreen, FrameLayout.SCALE_Y, 1.0f, 200L, OpaUtils.INTERPOLATOR_40_OUT));
        final Animator scaleXAnimator = getPropertyAnimator(mWhite, FrameLayout.SCALE_X, 1.0f, 150L, Interpolators.FAST_OUT_SLOW_IN);
        final Animator scaleYAnimator = getPropertyAnimator(mWhite, FrameLayout.SCALE_Y, 1.0f, 150L, Interpolators.FAST_OUT_SLOW_IN);
        scaleXAnimator.setStartDelay(33L);
        scaleYAnimator.setStartDelay(33L);
        animatorSet.add(scaleXAnimator);
        animatorSet.add(scaleYAnimator);
        OpaUtils.getLongestAnim(animatorSet).addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                mCurrentAnimators.clear();
                skipToStartingValue();
            }
        });
        return animatorSet;
    }

    private ArraySet<Animator> getLineAnimatorSet() {
        final ArraySet<Animator> animatorSet = new ArraySet<>();
        if (mIsVertical) {
            animatorSet.add(getPropertyAnimator(mRed, View.Y, mRed.getY() + OpaUtils.getPxVal(mResources,
                    R.dimen.opa_line_x_trans_ry), 225L, Interpolators.FAST_OUT_SLOW_IN));
            animatorSet.add(getPropertyAnimator(mRed, View.X, mRed.getX() + OpaUtils.getPxVal(mResources,
                    R.dimen.opa_line_y_translation), 133L, Interpolators.FAST_OUT_SLOW_IN));
            animatorSet.add(getPropertyAnimator(mBlue, View.Y, mBlue.getY() + OpaUtils.getPxVal(mResources,
                    R.dimen.opa_line_x_trans_bg), 225L, Interpolators.FAST_OUT_SLOW_IN));
            animatorSet.add(getPropertyAnimator(mYellow, View.Y, mYellow.getY() + (-OpaUtils.getPxVal(mResources,
                    R.dimen.opa_line_x_trans_ry)), 225L, Interpolators.FAST_OUT_SLOW_IN));
            animatorSet.add(getPropertyAnimator(mYellow, View.X, mYellow.getX() + (-OpaUtils.getPxVal(mResources,
                    R.dimen.opa_line_y_translation)), 133L, Interpolators.FAST_OUT_SLOW_IN));
            animatorSet.add(getPropertyAnimator(mGreen, View.Y, mGreen.getY() + (-OpaUtils.getPxVal(mResources,
                    R.dimen.opa_line_x_trans_bg)), 225L, Interpolators.FAST_OUT_SLOW_IN));
        } else {
            animatorSet.add(getPropertyAnimator(mRed, View.X, mRed.getX() + (-OpaUtils.getPxVal(mResources,
                    R.dimen.opa_line_x_trans_ry)), 225L, Interpolators.FAST_OUT_SLOW_IN));
            animatorSet.add(getPropertyAnimator(mRed, View.Y, mRed.getY() + OpaUtils.getPxVal(mResources,
                    R.dimen.opa_line_y_translation), 133L, Interpolators.FAST_OUT_SLOW_IN));
            animatorSet.add(getPropertyAnimator(mBlue, View.X, mBlue.getX() + (-OpaUtils.getPxVal(mResources,
                    R.dimen.opa_line_x_trans_bg)), 225L, Interpolators.FAST_OUT_SLOW_IN));
            animatorSet.add(getPropertyAnimator(mYellow, View.X, mYellow.getX() + OpaUtils.getPxVal(mResources,
                    R.dimen.opa_line_x_trans_ry), 225L, Interpolators.FAST_OUT_SLOW_IN));
            animatorSet.add(getPropertyAnimator(mYellow, View.Y, mYellow.getY() + (-OpaUtils.getPxVal(mResources,
                    R.dimen.opa_line_y_translation)), 133L, Interpolators.FAST_OUT_SLOW_IN));
            animatorSet.add(getPropertyAnimator(mGreen, View.X, mGreen.getX() + OpaUtils.getPxVal(mResources,
                    R.dimen.opa_line_x_trans_bg), 225L, Interpolators.FAST_OUT_SLOW_IN));
        }
        animatorSet.add(getPropertyAnimator(mWhite, FrameLayout.SCALE_X, 0.0f, 83L, mHomeDisappearInterpolator));
        animatorSet.add(getPropertyAnimator(mWhite, FrameLayout.SCALE_Y, 0.0f, 83L, mHomeDisappearInterpolator));
        OpaUtils.getLongestAnim(animatorSet).addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animator) {
                mCurrentAnimators.clear();
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                startCollapseAnimation();
            }
        });
        return animatorSet;
    }

    public void updateOpaLayout() {
        final LayoutParams layoutParams = (LayoutParams) mWhite.getLayoutParams();
        layoutParams.width = LayoutParams.MATCH_PARENT;
        layoutParams.height = LayoutParams.MATCH_PARENT;
        mWhite.setLayoutParams(layoutParams);
        mWhite.setScaleType(ImageView.ScaleType.CENTER);
    }

    private void cancelCurrentAnimation() {
        if (!mCurrentAnimators.isEmpty()) {
            for (int i = mCurrentAnimators.size() - 1; i >= 0; i--) {
                final Animator animator = mCurrentAnimators.valueAt(i);
                animator.removeAllListeners();
                animator.cancel();
            }
            mCurrentAnimators.clear();
            mAnimationState = 0;
        }
        if (mGestureAnimatorSet != null) {
            mGestureAnimatorSet.cancel();
            mGestureState = 0;
        }
    }

    private void endCurrentAnimation() {
        if (!mCurrentAnimators.isEmpty()) {
            for (int i = mCurrentAnimators.size() - 1; i >= 0; i--) {
                final Animator animator = mCurrentAnimators.valueAt(i);
                animator.removeAllListeners();
                animator.end();
            }
            mCurrentAnimators.clear();
        }
        mAnimationState = 0;
    }

    private void setDotsVisible() {
        for (int i = 0; i < mAnimatedViews.size(); i++) {
            mAnimatedViews.get(i).setAlpha(1.0f);
        }
    }

    private void skipToStartingValue() {
        for (int i = 0; i < mAnimatedViews.size(); i++) {
            final View view = mAnimatedViews.get(i);
            view.setScaleY(1.0f);
            view.setScaleX(1.0f);
            view.setTranslationY(0.0f);
            view.setTranslationX(0.0f);
            view.setAlpha(0.0f);
        }
        mWhite.setAlpha(1.0f);
        mAnimationState = 0;
        mGestureState = 0;
    }

    @Override
    public void setVertical(boolean vertical) {
        if (mIsVertical != vertical && mGestureAnimatorSet != null) {
            mGestureAnimatorSet.cancel();
            mGestureAnimatorSet = null;
            skipToStartingValue();
        }
        mIsVertical = vertical;
        mHome.setVertical(vertical);
        if (mIsVertical) {
            mTop = mGreen;
            mBottom = mBlue;
            mRight = mYellow;
            mLeft = mRed;
        } else {
            mTop = mRed;
            mBottom = mYellow;
            mLeft = mBlue;
            mRight = mGreen;
        }
    }

    @Override
    public void setDarkIntensity(float intensity) {
        if (mWhite.getDrawable() instanceof KeyButtonDrawable) {
            ((KeyButtonDrawable) mWhite.getDrawable()).setDarkIntensity(intensity);
        }
        mWhite.invalidate();
        mHome.setDarkIntensity(intensity);
    }

    @Override
    public void setDelayTouchFeedback(boolean delay) {
        mHome.setDelayTouchFeedback(delay);
        mDelayTouchFeedback = delay;
    }

    private AnimatorSet getGestureAnimatorSet() {
        if (mGestureLineSet != null) {
            mGestureLineSet.removeAllListeners();
            mGestureLineSet.cancel();
            return mGestureLineSet;
        }
        mGestureLineSet = new AnimatorSet();
        final ObjectAnimator scaleObjectAnimator = OpaUtils.getScaleObjectAnimator(mWhite, 0.0f, 100L, OpaUtils.INTERPOLATOR_40_OUT);
        scaleObjectAnimator.setStartDelay(50L);
        mGestureLineSet.play(scaleObjectAnimator);
        mGestureLineSet.play(OpaUtils.getScaleObjectAnimator(mTop, 0.8f, 200L, Interpolators.FAST_OUT_SLOW_IN))
                .with(scaleObjectAnimator)
                .with(OpaUtils.getAlphaObjectAnimator(mRed, 1.0f, 50L, 130L, Interpolators.LINEAR))
                .with(OpaUtils.getAlphaObjectAnimator(mYellow, 1.0f, 50L, 130L, Interpolators.LINEAR))
                .with(OpaUtils.getAlphaObjectAnimator(mBlue, 1.0f, 50L, 113L, Interpolators.LINEAR))
                .with(OpaUtils.getAlphaObjectAnimator(mGreen, 1.0f, 50L, 113L, Interpolators.LINEAR))
                .with(OpaUtils.getScaleObjectAnimator(mBottom, 0.8f, 200L, Interpolators.FAST_OUT_SLOW_IN))
                .with(OpaUtils.getScaleObjectAnimator(mLeft, 0.8f, 200L, Interpolators.FAST_OUT_SLOW_IN))
                .with(OpaUtils.getScaleObjectAnimator(mRight, 0.8f, 200L, Interpolators.FAST_OUT_SLOW_IN));
        if (mIsVertical) {
            final ObjectAnimator translationObjectAnimatorY = OpaUtils.getTranslationObjectAnimatorY(
                    mRed, OpaUtils.INTERPOLATOR_40_40, OpaUtils.getPxVal(mResources, R.dimen.opa_line_x_trans_ry),
                    mRed.getY() + OpaUtils.getDeltaDiamondPositionLeftY(), 350L);
            translationObjectAnimatorY.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    startCollapseAnimation();
                }
            });
            mGestureLineSet.play(translationObjectAnimatorY).with(OpaUtils.getTranslationObjectAnimatorY(
                    mBlue, OpaUtils.INTERPOLATOR_40_40, OpaUtils.getPxVal(mResources, R.dimen.opa_line_x_trans_bg),
                    mBlue.getY() + OpaUtils.getDeltaDiamondPositionBottomY(mResources), 350L))
                    .with(OpaUtils.getTranslationObjectAnimatorY(mYellow, OpaUtils.INTERPOLATOR_40_40,
                        -OpaUtils.getPxVal(mResources, R.dimen.opa_line_x_trans_ry), mYellow.getY()
                            + OpaUtils.getDeltaDiamondPositionRightY(), 350L))
                    .with(OpaUtils.getTranslationObjectAnimatorY(mGreen, OpaUtils.INTERPOLATOR_40_40,
                        -OpaUtils.getPxVal(mResources, R.dimen.opa_line_x_trans_bg), mGreen.getY()
                            + OpaUtils.getDeltaDiamondPositionTopY(mResources), 350L));
        } else {
            final ObjectAnimator translationObjectAnimatorX = OpaUtils.getTranslationObjectAnimatorX(
                    mRed, OpaUtils.INTERPOLATOR_40_40, -OpaUtils.getPxVal(mResources, R.dimen.opa_line_x_trans_ry),
                    mRed.getX() + OpaUtils.getDeltaDiamondPositionTopX(), 350L);
            translationObjectAnimatorX.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    startCollapseAnimation();
                }
            });
            mGestureLineSet.play(translationObjectAnimatorX).with(scaleObjectAnimator)
                    .with(OpaUtils.getTranslationObjectAnimatorX(mBlue, OpaUtils.INTERPOLATOR_40_40,
                        -OpaUtils.getPxVal(mResources, R.dimen.opa_line_x_trans_bg), mBlue.getX()
                            + OpaUtils.getDeltaDiamondPositionLeftX(mResources), 350L))
                    .with(OpaUtils.getTranslationObjectAnimatorX(mYellow, OpaUtils.INTERPOLATOR_40_40,
                        OpaUtils.getPxVal(mResources, R.dimen.opa_line_x_trans_ry), mYellow.getX()
                            + OpaUtils.getDeltaDiamondPositionBottomX(), 350L))
                    .with(OpaUtils.getTranslationObjectAnimatorX(mGreen, OpaUtils.INTERPOLATOR_40_40,
                        OpaUtils.getPxVal(mResources, R.dimen.opa_line_x_trans_bg), mGreen.getX()
                            + OpaUtils.getDeltaDiamondPositionRightX(mResources), 350L));
        }
        return mGestureLineSet;
    }

    private Animator getPropertyAnimator(View view, Property<View, Float> property,
            float value, long duration, Interpolator interpolator) {
        final ObjectAnimator ofFloat = ObjectAnimator.ofFloat(view, property, value);
        ofFloat.setDuration(duration);
        ofFloat.setInterpolator(interpolator);
        return ofFloat;
    }

    private void hideAllOpa() {
        fadeOutButton(mBlue);
        fadeOutButton(mRed);
        fadeOutButton(mYellow);
        fadeOutButton(mGreen);
    }

    private void showAllOpa() {
        fadeInButton(mBlue);
        fadeInButton(mRed);
        fadeInButton(mYellow);
        fadeInButton(mGreen);
    }

    private void fadeInButton(View viewToFade){
        if (viewToFade == null) return;
        final ObjectAnimator animator = ObjectAnimator.ofFloat(viewToFade, View.ALPHA, 0.0f, 1.0f);
        animator.setDuration(50L);
        animator.start();
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                viewToFade.setVisibility(View.VISIBLE);
            }
        });
    }

    private void fadeOutButton(View viewToFade){
        if (viewToFade == null) return;
        final ObjectAnimator animator = ObjectAnimator.ofFloat(viewToFade, View.ALPHA, 1.0f, 0.0f);
        animator.setDuration(250L);
        animator.start();
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                viewToFade.setVisibility(View.INVISIBLE);
            }
        });
    }
}
