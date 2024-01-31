/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.server.policy.gesture;

import static org.nameless.os.DebugConstants.DEBUG_PHONE_WINDOW_MANAGER;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Slog;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import com.android.internal.R;
import com.android.internal.policy.SystemBarUtils;

import java.util.HashMap;

class ThreeFingerGestureListener extends GestureListenerBase {

    private static final String TAG = "SystemGesture::ThreeFingerGestureListener";

    private static final float SCREENSHOT_HEIGHT_MULTIPLE = 0.4f;

    private static final long PARTIAL_SCREENSHOT_HOLD_DURATION = 1000L;
    private static final long SCREENSHOT_GESTURE_VALID_DURATION = 50L;

    private static final int MSG_TAKE_PARTIAL_SCREENSHOT = 1;
    private static final int MSG_TAKE_FULL_SCREENSHOT = 2;

    private final class H extends Handler {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case MSG_TAKE_PARTIAL_SCREENSHOT:
                    mHandledPartial = true;
                    mSystemGesture.getPhoneWindowManagerExt().takeScreenshotIfSetupCompleted(false);
                    break;
                case MSG_TAKE_FULL_SCREENSHOT:
                    mSystemGesture.getPhoneWindowManagerExt().takeScreenshotIfSetupCompleted(true);
                    break;
            }
        }
    }

    private final Handler mHandler = new H();

    private final HashMap<Integer, Float[]> mPointersDown = new HashMap<>();
    private final HashMap<Integer, Float[]> mPointersUp = new HashMap<>();

    private int mStatusBarHeight;
    private int mTouchSlop;

    private float mValidDistance = 100f;
    private long mLatestPointDownTime = -1L;

    private boolean mHandledPartial = false;

    ThreeFingerGestureListener(SystemGesture systemGesture, Context context) {
        super(systemGesture, context);
    }

    @Override
    protected void onConfigureChanged() {
        super.onConfigureChanged();
        mStatusBarHeight = SystemBarUtils.getStatusBarHeight(mContext);
        mValidDistance = mContext.getResources().getDimensionPixelSize(R.dimen.screenshot_gesture_valid_distance);
        mTouchSlop = ViewConfiguration.get(mContext).getScaledTouchSlop();
    }

    @Override
    protected int getSupportGestureType() {
        return -1;
    }

    @Override
    protected boolean isSupportGestureType(int gesture) {
        return false;
    }

    @Override
    protected boolean shouldInterceptGesture() {
        return mGestureState == GestureState.PENDING_CHECK || mGestureState == GestureState.TRIGGERED;
    }

    @Override
    protected boolean dispatchCancelIfNeeded() {
        return true;
    }

    @Override
    public boolean interceptMotionBeforeQueueing(MotionEvent event) {
        if (!mSystemGesture.getPhoneWindowManagerExt().isThreeFingerGestureOn()) {
            return false;
        }

        final float x = event.getX(event.getActionIndex());
        final float y = event.getY(event.getActionIndex());
        final Float[] location = {x, y};
        final int pointerId = event.getPointerId(event.getActionIndex());

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (mHandler.hasMessages(MSG_TAKE_PARTIAL_SCREENSHOT)) {
                    mHandler.removeMessages(MSG_TAKE_PARTIAL_SCREENSHOT);
                }
                mHandledPartial = false;
                mPointersDown.put(pointerId, location);
                mLatestPointDownTime = SystemClock.uptimeMillis();
                return true;
            case MotionEvent.ACTION_UP:
                if (mHandler.hasMessages(MSG_TAKE_PARTIAL_SCREENSHOT)) {
                    mHandler.removeMessages(MSG_TAKE_PARTIAL_SCREENSHOT);
                }
                mPointersUp.put(pointerId, location);
                if (mGestureState == GestureState.PENDING_CHECK && !mHandledPartial) {
                    for (int id : mPointersDown.keySet()) {
                        final Float[] locationDown = mPointersDown.get(id);
                        final Float[] locationUp = mPointersUp.get(id);
                        final float xOffset = locationUp[0] - locationDown[0];
                        final float yOffset = locationUp[1] - locationDown[1];
                        final float xOffsetAbs = Math.abs(xOffset);
                        final float yOffsetAbs = Math.abs(yOffset);

                        boolean shouldTrigger = true;
                        if (yOffsetAbs <= mValidDistance) {
                            logD("At least one finger move too short");
                            shouldTrigger = false;
                        } else if (xOffsetAbs > yOffsetAbs) {
                            logD("At least one finger x offset more than y");
                            shouldTrigger = false;
                        } else if (yOffset < 0.0f) {
                            logD("At least one finger go opposite after triggered");
                            shouldTrigger = false;
                        }
                        if (shouldTrigger) {
                            mGestureState = GestureState.TRIGGERED;
                        } else {
                            mGestureState = GestureState.IDLE;
                        }
                    }
                }
                if (mGestureState == GestureState.TRIGGERED &&
                        mSystemGesture.getPhoneWindowManagerExt().isThreeFingerSwipeOn()) {
                    logD("Three finger screen shot detect now");
                    mHandler.sendEmptyMessage(MSG_TAKE_FULL_SCREENSHOT);
                }
                mPointersDown.clear();
                mPointersUp.clear();
                mGestureState = GestureState.IDLE;
                return false;
            case MotionEvent.ACTION_MOVE:
                if (mGestureState == GestureState.PENDING_CHECK && !mHandledPartial) {
                    boolean validGesture = true;
                    for (int i = 0; i < event.getPointerCount(); i++) {
                        final Float[] locationDown = mPointersDown.get(event.getPointerId(i));
                        final float xOffset = event.getX(i) - locationDown[0];
                        final float yOffset = event.getY(i) - locationDown[1];
                        final float xOffsetAbs = Math.abs(xOffset);
                        final float yOffsetAbs = Math.abs(yOffset);
                        if (yOffset < -mTouchSlop) {
                            logD("Negative slide direction");
                            mGestureState = GestureState.CANCELED;
                            return false;
                        }
                        if (xOffsetAbs > mTouchSlop && xOffsetAbs > yOffsetAbs) {
                            logD("x offset more than y");
                            mGestureState = GestureState.CANCELED;
                            return false;
                        }
                        validGesture &= (yOffsetAbs >= mTouchSlop)
                                && (xOffsetAbs <= yOffsetAbs)
                                && (yOffset >= 0.0f);
                    }
                    if (validGesture) {
                        if (mHandler.hasMessages(MSG_TAKE_PARTIAL_SCREENSHOT)) {
                            mHandler.removeMessages(MSG_TAKE_PARTIAL_SCREENSHOT);
                        }
                        return true;
                    }
                }
                return mHandledPartial;
            case MotionEvent.ACTION_POINTER_DOWN:
                if (mHandler.hasMessages(MSG_TAKE_PARTIAL_SCREENSHOT)) {
                    mHandler.removeMessages(MSG_TAKE_PARTIAL_SCREENSHOT);
                }
                final long pointDownTime = SystemClock.uptimeMillis();
                if (pointDownTime - mLatestPointDownTime > SCREENSHOT_GESTURE_VALID_DURATION) {
                    logD("Finger down time out");
                    mGestureState = GestureState.CANCELED;
                } else {
                    mLatestPointDownTime = pointDownTime;
                }
                if (y <= mStatusBarHeight) {
                    logD("Finger down in status bar area");
                    mGestureState = GestureState.CANCELED;
                }
                mPointersDown.put(pointerId, location);
                if (mGestureState == GestureState.IDLE && event.getPointerCount() == 3) {
                    if (checkThreePointersDistance(event)) {
                        final boolean holdGestureOn = mSystemGesture.getPhoneWindowManagerExt().isThreeFingerHoldOn();
                        if (holdGestureOn) {
                            mHandler.sendEmptyMessageDelayed(MSG_TAKE_PARTIAL_SCREENSHOT,
                                    PARTIAL_SCREENSHOT_HOLD_DURATION);
                        }
                        mGestureState = GestureState.PENDING_CHECK;
                        return holdGestureOn;
                    }
                    logD("Fingers down too far away");
                    mGestureState = GestureState.CANCELED;
                    return false;
                }
                if (event.getPointerCount() > 3) {
                    logD("Fingers num over 3");
                    mGestureState = GestureState.CANCELED;
                    return false;
                }
                return false;
            case MotionEvent.ACTION_POINTER_UP:
                if (mHandler.hasMessages(MSG_TAKE_PARTIAL_SCREENSHOT)) {
                    mHandler.removeMessages(MSG_TAKE_PARTIAL_SCREENSHOT);
                }
                mPointersUp.put(pointerId, location);
                return true;
        }
        return false;
    }

    private boolean checkThreePointersDistance(MotionEvent event) {
        if (event == null) {
            return false;
        }
        final Float[] pointer0 = mPointersDown.get(event.getPointerId(0));
        final Float[] pointer1 = mPointersDown.get(event.getPointerId(1));
        final Float[] pointer2 = mPointersDown.get(event.getPointerId(2));
        if (pointer0 == null || pointer1 == null || pointer2 == null) {
            return false;
        }
        final double[] distances = {calDistance(pointer0[0], pointer0[1], pointer1[0], pointer1[1]),
                calDistance(pointer0[0], pointer0[1], pointer2[0], pointer2[1]),
                calDistance(pointer1[0], pointer1[1], pointer2[0], pointer2[1])};
        int distanceNearCount = 0;
        for (double distance : distances) {
            if (distance < SCREENSHOT_HEIGHT_MULTIPLE * mDeviceHeight) {
                distanceNearCount++;
            }
        }
        logD("checkThreePointersDistance, distanceNearCount=" + distanceNearCount);
        return distanceNearCount >= 2;
    }

    private static double calDistance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x1 - x2, 2.0d) + Math.pow(y1 - y2, 2.0d));
    }

    private static void logD(String msg) {
        if (DEBUG_PHONE_WINDOW_MANAGER) {
            Slog.d(TAG, msg);
        }
    }
}
