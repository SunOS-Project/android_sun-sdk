/*
 * Copyright (C) 2023 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.server.policy.gesture;

import static android.view.WindowManager.LayoutParams.TYPE_NOTIFICATION_SHADE;

import static org.sun.os.DebugConstants.DEBUG_PHONE_WINDOW_MANAGER;
import static org.sun.view.ISystemGestureListener.GESTURE_WINDOW_MODE;

import android.content.Context;
import android.content.res.Resources;
import android.util.Slog;
import android.view.MotionEvent;
import android.view.Surface;

import com.android.server.policy.PhoneWindowManagerExt;
import com.android.server.policy.WindowManagerPolicy.WindowState;

public class WindowModeGestureListener extends GestureListenerBase {

    private static final String TAG = "WindowModeGestureListener";

    private static final float ANGLE_THRESHOLD = 20.0f;

    private float mSquaredSlop;
    private float mValDisFromCorner;

    public WindowModeGestureListener(SystemGesture systemGesture,
            PhoneWindowManagerExt ext, Context context) {
        super(systemGesture, ext, context, TAG);
    }

    @Override
    protected void onConfigureChanged() {
        super.onConfigureChanged();
        final float slop = getGestureTouchSlop();
        mSquaredSlop = slop * slop;
        mValDisFromCorner = ResourceUtils.getWindowModeGestureValidDistance(
                mContext.getResources(), Math.min(mDeviceHeight, mDeviceWidth));
        if (DEBUG_PHONE_WINDOW_MANAGER) {
            Slog.d(TAG, "onConfigureChanged, mValDisFromCorner=" + mValDisFromCorner);
        }
    }

    @Override
    public boolean onActionDown(MotionEvent event) {
        if (!hasRegisterClient()) {
            return false;
        }
        if (mDisabledByGame) {
            return false;
        }
        super.onActionDown(event);
        if (canTriggerWindowModeAction(event)) {
            mGesturePreTriggerConsumed = notifyGesturePreTriggerBefore(event);
        } else {
            mGesturePreTriggerConsumed = false;
        }
        if (mGesturePreTriggerConsumed) {
            mGestureState = GestureState.PENDING_CHECK;
        }
        if (DEBUG_PHONE_WINDOW_MANAGER) {
            Slog.d(TAG, "onActionDown, mGesturePreTriggerConsumed="
                    + mGesturePreTriggerConsumed + ", mGestureState=" + mGestureState);
        }
        if (mGestureState != GestureState.PENDING_CHECK) {
            return false;
        }
        notifyGesturePreTrigger(event);
        return true;
    }

    @Override
    public boolean onActionMove(MotionEvent event) {
        if (mGestureState == GestureState.PENDING_CHECK) {
            checkWindowModeGesture(event);
        }
        if (mGestureState == GestureState.TRIGGERED) {
            notifyGestureTriggered(event);
        }
        if (DEBUG_PHONE_WINDOW_MANAGER && mGestureState != mLastMoveGestureState) {
            Slog.d(TAG, "onActionMove, mGestureState=" + mGestureState);
        }
        mLastMoveGestureState = mGestureState;
        return true;
    }

    @Override
    public boolean onActionUp(MotionEvent event) {
        final boolean ret;
        if (mGestureState == GestureState.TRIGGERED) {
            ret = true;
            notifyGestureTriggered(event);
        } else {
            ret = false;
            notifyGestureCanceled(event);
        }
        if (DEBUG_PHONE_WINDOW_MANAGER) {
            Slog.d(TAG, "onActionUp, mGestureState=" + mGestureState);
        }
        super.onActionUp(event);
        return ret;
    }

    @Override
    public void onActionCancel(MotionEvent event) {
        notifyGestureCanceled(event);
        if (DEBUG_PHONE_WINDOW_MANAGER) {
            Slog.d(TAG, "onActionCancel");
        }
        super.onActionCancel(event);
    }

    @Override
    protected boolean hasRegisterClient() {
        if (mSystemGestureClient != null) {
            final WindowState windowState =
                    mPhoneWindowManagerExt.getWindowState();
            return windowState == null || windowState.getBaseType() != TYPE_NOTIFICATION_SHADE;
        }
        return false;
    }

    private void checkWindowModeGesture(MotionEvent event) {
        if (isAlreadyTimeout()) {
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
                        && event.getRawY() > mDeviceWidth - mValDisFromCorner;
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
