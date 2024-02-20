/*
 * Copyright (C) 2023 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.server.policy.gesture;

import static android.view.WindowManager.LayoutParams.TYPE_NOTIFICATION_SHADE;

import static org.nameless.os.DebugConstants.DEBUG_PHONE_WINDOW_MANAGER;
import static org.nameless.view.ISystemGestureListener.GESTURE_WINDOW_MODE;

import android.content.Context;
import android.content.res.Resources;
import android.util.Slog;
import android.view.MotionEvent;
import android.view.Surface;

import com.android.server.policy.WindowManagerPolicy.WindowState;

public class WindowModeGestureListener extends GestureListenerBase {

    private static final String TAG = "WindowModeGestureListener";

    private static final float ANGLE_THRESHOLD = 20.0f;

    private float mDownPosX = 0.0f;
    private float mDownPosY = 0.0f;
    private float mSquaredSlop;
    private float mValDisFromCorner;

    public WindowModeGestureListener(SystemGesture systemGesture, Context context) {
        super(systemGesture, context);
    }

    @Override
    protected void onConfigureChanged() {
        super.onConfigureChanged();
        final float slop = getGestureTouchSlop();
        mSquaredSlop = slop * slop;
        mValDisFromCorner = (float) ResourceUtils.getWindowModeGestureValidDistance(mContext.getResources());
        if (DEBUG_PHONE_WINDOW_MANAGER) {
            Slog.d(TAG, "onConfigureChanged, mValDisFromCorner=" + mValDisFromCorner);
        }
    }

    @Override
    protected boolean hasRegisterClient() {
        if (mSystemGestureClient != null) {
            final WindowState windowState =
                    mSystemGesture.getPhoneWindowManagerExt().getWindowState();
            return windowState == null || windowState.getBaseType() != TYPE_NOTIFICATION_SHADE;
        }
        return false;
    }

    @Override
    protected boolean shouldInterceptGesture() {
        return true;
    }

    @Override
    public boolean interceptMotionBeforeQueueing(MotionEvent event) {
        boolean result = false;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (!hasRegisterClient()) {
                    return result;
                }
                mDownPosX = event.getRawX();
                mDownPosY = event.getRawY();
                mMotionDownTime = System.currentTimeMillis();
                if (canTriggerWindowModeAction(event)) {
                    mGesturePreTriggerConsumed = notifyGesturePreTriggerBefore(event);
                } else {
                    mGesturePreTriggerConsumed = false;
                }
                if (mGesturePreTriggerConsumed) {
                    mGestureState = GestureState.PENDING_CHECK;
                }
                if (DEBUG_PHONE_WINDOW_MANAGER) {
                    Slog.d(TAG, "interceptMotionBeforeQueueing, mGesturePreTriggerConsumed="
                            + mGesturePreTriggerConsumed + ", mGestureState=" + mGestureState);
                }
                if (mGestureState != GestureState.TRIGGERED && mGestureState != GestureState.PENDING_CHECK) {
                    return result;
                }
                notifyGesturePreTrigger(event);
                return true;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (mGestureState == GestureState.PENDING_CHECK ||
                        mGestureState == GestureState.TRIGGERED ||
                        mGestureState == GestureState.CANCELED) {
                    if (mGestureState == GestureState.TRIGGERED && event.getAction() == MotionEvent.ACTION_UP) {
                        notifyGestureTriggered(event);
                    } else if (mGestureState == GestureState.CANCELED || mGestureState == GestureState.PENDING_CHECK) {
                        notifyGestureCanceled();
                    }
                }
                mGesturePreTriggerConsumed = false;
                return true;
            case MotionEvent.ACTION_MOVE:
                if (mGestureState == GestureState.PENDING_CHECK) {
                    checkWindowModeGesture(event);
                }
                if (mGestureState == GestureState.TRIGGERED) {
                    notifyGestureTriggered(event);
                }
                return mGestureState != GestureState.IDLE;
            default:
                return result;
        }
    }

    private void checkWindowModeGesture(MotionEvent event) {
        if (System.currentTimeMillis() - mMotionDownTime > GESTURE_TRIGGER_TIME_OUT) {
            if (DEBUG_PHONE_WINDOW_MANAGER) {
                Slog.d(TAG, "Window mode gesture time out");
            }
            mGestureState = GestureState.CANCELED;
        } else if (squaredHypot(event.getRawX() - mDownPosX, event.getRawY() - mDownPosY) > mSquaredSlop) {
            if (isLandscape() || isValidGestureAngle(mDownPosX - event.getRawX(), mDownPosY - event.getRawY())) {
                mGestureState = GestureState.TRIGGERED;
            } else {
                mGestureState = GestureState.CANCELED;
            }
        }
    }

    @Override
    protected boolean isSupportGestureType(int gesture) {
        return gesture == GESTURE_WINDOW_MODE;
    }

    @Override
    protected int getSupportGestureType() {
        return GESTURE_WINDOW_MODE;
    }

    private boolean isValidGestureAngle(float deltaX, float deltaY) {
        float angle = (float) Math.toDegrees(Math.atan2(deltaY, deltaX));
        if (angle > 90.0f) {
            angle = 180.0f - angle;
        }
        return angle > ANGLE_THRESHOLD && angle < 90.0f;
    }

    private boolean canTriggerWindowModeAction(MotionEvent event) {
        switch (mDisplay.getRotation()) {
            case Surface.ROTATION_0:
            case Surface.ROTATION_180:
                return (event.getRawX() < mValDisFromCorner || event.getRawX() > mDeviceWidth - mValDisFromCorner)
                        && event.getRawY() > mDeviceHeight - mValDisFromCorner;
            case Surface.ROTATION_90:
            case Surface.ROTATION_270:
                final float valXDisFromCornerLand = mValDisFromCorner / 2 * 3;
                return (event.getRawX() < valXDisFromCornerLand || event.getRawX() > mDeviceHeight - valXDisFromCornerLand)
                        && event.getRawY() > mDeviceWidth - mValDisFromCorner && event.getOrientation() != 0.0f;
            default:
                return false;
        }
    }

    private static float squaredHypot(float x, float y) {
        return x * x + y * y;
    }

    private boolean isLandscape() {
        final int rotation = mDisplay.getRotation();
        return rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270;
    }
}
