/*
 * Copyright (C) 2016 The ParanoidAndroid Project
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.server.policy;

import android.animation.Animator;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import com.android.server.UiThread;

/**
 * This class provides a fullscreen overlays view, displaying itself
 * even on top of lock screen. While this view is displaying touch
 * inputs are not passed to the the views below.
 * @see android.view.WindowManager.LayoutParams.TYPE_SYSTEM_ERROR
 * @author Carlo Savignano
 */
public class PocketLock {

    private final Context mContext;
    private final View mView;
    private final WindowManager mWindowManager;
    private final WindowManager.LayoutParams mLayoutParams;

    private final Handler mHandler = new Handler(UiThread.getHandler().getLooper());

    private boolean mAnimating;

    /**
     * Creates pocket lock objects, inflate view and set layout parameters.
     * @param context
     */
    public PocketLock(Context context) {
        mContext = context;
        mView = LayoutInflater.from(mContext).inflate(
                com.android.internal.R.layout.pocket_lock_view, null);
        mView.setAlpha(0.0f);
        mView.setVisibility(View.GONE);
        mWindowManager = mContext.getSystemService(WindowManager.class);
        mLayoutParams = new WindowManager.LayoutParams();
        mLayoutParams.format = PixelFormat.TRANSLUCENT;
        mLayoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        mLayoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        mLayoutParams.gravity = Gravity.CENTER;
        mLayoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
        mLayoutParams.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        mLayoutParams.flags = WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                | WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        mHandler.post(() -> mWindowManager.addView(mView, mLayoutParams));
    }

    public void show() {
        mHandler.post(() -> {
            if (mAnimating) {
                mView.animate().cancel();
            }
            mView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            mView.animate().alpha(1.0f).setListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) {
                    mAnimating = true;
                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    mView.setLayerType(View.LAYER_TYPE_NONE, null);
                    mAnimating = false;
                }

                @Override
                public void onAnimationCancel(Animator animator) {
                }

                @Override
                public void onAnimationRepeat(Animator animator) {
                }
            }).withStartAction(() -> {
                mView.setAlpha(0.0f);
                mView.setVisibility(View.VISIBLE);
                mView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                                          | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                          | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
            }).start();
        });
    }

    public void hide() {
        mHandler.post(() -> {
            if (mAnimating) {
                mView.animate().cancel();
            }
            mView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            mView.animate().alpha(0.0f).setListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) {
                    mAnimating = true;
                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    mView.setVisibility(View.GONE);
                    mView.setLayerType(View.LAYER_TYPE_NONE, null);
                    mView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
                    mAnimating = false;
                }

                @Override
                public void onAnimationCancel(Animator animator) {
                }

                @Override
                public void onAnimationRepeat(Animator animator) {
                }
            }).start();
        });
    }
}
