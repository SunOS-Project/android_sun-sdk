/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.server.policy.gesture.threefinger;

import static org.nameless.os.DebugConstants.DEBUG_THREE_FINGER;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.graphics.PointF;
import android.os.Handler;
import android.util.Slog;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import com.android.internal.policy.ThreeFingerGestureHelper;

import com.android.server.policy.PhoneWindowManagerExt;

public class TouchHoldGesture extends BaseThreeFingerGesture {

    private static final String TAG = "ThreeFinger::TouchHold";

    private static final long LONG_PRESS_THRESHOLD = 1000L;

    private final DevicePolicyManager mDevicePolicyManager;
    private final PartialScreenshotHelper mPartialScreenshotHelper;
    private final ViewConfiguration mViewConfiguration;

    private final Runnable mLongPressRunnable = () -> {
        handleLongPress();
    };

    public TouchHoldGesture(Context context, Handler handler) {
        super(context, handler);
        mDevicePolicyManager = context.getSystemService(DevicePolicyManager.class);
        mPartialScreenshotHelper = new PartialScreenshotHelper(context);
        mViewConfiguration = ViewConfiguration.get(context);
    }

    @Override
    protected String getTag() {
        return TAG;
    }

    @Override
    protected void handleFingerMoveEvent(MotionEvent motionEvent) {
        if (mHandleNeeded) {
            if (mHandler.hasCallbacks(mLongPressRunnable)) {
                final SparseArray<PointF> points = new SparseArray<>();
                ThreeFingerGestureHelper.getPoints(motionEvent, points);
                if (!isHoldCanceled(points)) {
                    return;
                }
                if (DEBUG_THREE_FINGER) {
                    Slog.d(TAG, "hold still failed");
                }
                mHandleNeeded = false;
            }/* else if (mPartialScreenshotHelper.hasConnection()) {
                mPartialScreenshotHelper.sendFingerMoveMessage(motionEvent);
            }*/
        }
        mHandler.removeCallbacks(mLongPressRunnable);
    }

    @Override
    protected boolean handleFingerUpEvent(MotionEvent event) {
        final boolean handled = mHandleNeeded;
        mHandleNeeded = false;
        mHandler.removeCallbacks(mLongPressRunnable);
        /*if (mPartialScreenshotHelper.hasConnection()) {
            mPartialScreenshotHelper.sendFingerUpMessage(event);
        }*/
        return handled;
    }

    public void handleLongPress() {
        /*if (mDevicePolicyManager.getScreenCaptureDisabled(null, -1)) {
            Slog.w(TAG, "handleLongPress: skip screenshot because blocked by IT admin");
            return;
        }
        if (DEBUG_THREE_FINGER) {
            Slog.d(TAG, "handleLongPress");
        }
        mPartialScreenshotHelper.sendShowMessage(mPoints);*/
        PhoneWindowManagerExt.getInstance().takeScreenshotIfSetupCompleted(false);
    }

    @Override
    protected boolean isGestureValid(MotionEvent event) {
        final boolean isGestureValid = super.isGestureValid(event);
        if (isGestureValid) {
            mHandler.removeCallbacks(mLongPressRunnable);
            mHandler.postAtTime(mLongPressRunnable, event.getDownTime() + LONG_PRESS_THRESHOLD);
        }
        return isGestureValid;
    }

    private boolean isHoldCanceled(SparseArray<PointF> points) {
        for (int i = 0; i < points.size(); i++) {
            if (ThreeFingerGestureHelper.squaredHypot(mPoints.valueAt(i), points.valueAt(i)) >
                    mViewConfiguration.getScaledTouchSlop()) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void reset(MotionEvent motionEvent) {
        mHandleNeeded = false;
        mHandler.removeCallbacks(mLongPressRunnable);
        /*if (mPartialScreenshotHelper.hasConnection()) {
            mPartialScreenshotHelper.sendFingerUpMessage(motionEvent);
        }*/
    }
}
