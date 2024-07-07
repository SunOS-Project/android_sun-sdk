/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.server.policy.gesture;

import static org.nameless.os.DebugConstants.DEBUG_PHONE_WINDOW_MANAGER;
import static org.nameless.view.ISystemGestureListener.GESTURE_GAME_MODE;

import android.content.Context;
import android.content.res.Resources;
import android.util.Slog;
import android.view.MotionEvent;
import android.view.Surface;

import com.android.internal.R;

import com.android.server.policy.PhoneWindowManagerExt;

public class GameModeGestureListener extends GestureListenerBase {

    private static final String TAG = "GameModeGestureListener";

    private static final float ANGLE_THRESHOLD = 64.0f;

    private float mGesturePortAreaBottom;
    private float mGestureLandAreaBottom;
    private float mGestureValidDistance;
    private float mSquaredSlop;

    public GameModeGestureListener(SystemGesture SystemGesture,
            PhoneWindowManagerExt ext, Context context) {
        super(SystemGesture, ext, context);
    }

    @Override
    protected void onConfigureChanged() {
        super.onConfigureChanged();
        final float slop = getGestureTouchSlop();
        mSquaredSlop = slop * slop;
        mGestureValidDistance = ResourceUtils.getGameModeGestureValidDistance(
                mContext.getResources(), Math.min(mDeviceHeight, mDeviceWidth));
        mGesturePortAreaBottom = ResourceUtils.getGameModePortraitAreaBottom(
                mContext.getResources(), Math.min(mDeviceHeight, mDeviceWidth));
        mGestureLandAreaBottom = ResourceUtils.getGameModeLandscapeAreaBottom(
                mContext.getResources(), Math.min(mDeviceHeight, mDeviceWidth));
        if (DEBUG_PHONE_WINDOW_MANAGER) {
            Slog.d(TAG, "mGestureValidDistance = " + mGestureValidDistance
                    + ", mGesturePortAreaBottom = " + mGesturePortAreaBottom
                    + ", mGestureLandAreaBottom = " + mGestureLandAreaBottom);
        }
    }

    @Override
    protected int getDefaultGestureTouchSlop() {
        return mContext.getResources().getDimensionPixelSize(R.dimen.game_mode_gesture_touch_slop);
    }

    @Override
    public boolean onActionDown(MotionEvent event) {
        if (!hasRegisterClient()) {
            return false;
        }
        mDownPosX = event.getRawX();
        mDownPosY = event.getRawY();
        mDownTime = System.currentTimeMillis();
        if (isInGameModeGestureArea(event)) {
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
            checkGameModeGesture(event);
        }
        if (mGestureState == GestureState.TRIGGERED) {
            notifyGestureTriggered(event);
        }
        if (DEBUG_PHONE_WINDOW_MANAGER) {
            Slog.d(TAG, "onActionMove, mGestureState=" + mGestureState);
        }
        return true;
    }

    @Override
    public void onActionUp(MotionEvent event) {
        if (mGestureState == GestureState.TRIGGERED) {
            notifyGestureTriggered(event);
        } else {
            notifyGestureCanceled();
        }
        if (DEBUG_PHONE_WINDOW_MANAGER) {
            Slog.d(TAG, "onActionUp, mGestureState=" + mGestureState);
        }
        super.onActionUp(event);
    }

    @Override
    public void onActionCancel(MotionEvent event) {
        notifyGestureCanceled();
        if (DEBUG_PHONE_WINDOW_MANAGER) {
            Slog.d(TAG, "onActionCancel");
        }
        super.onActionCancel(event);
    }

    private void checkGameModeGesture(MotionEvent event) {
        if (System.currentTimeMillis() - mDownTime > GESTURE_TRIGGER_TIME_OUT) {
            if (DEBUG_PHONE_WINDOW_MANAGER) {
                Slog.d(TAG, "Game mode gesture time out");
            }
            mGestureState = GestureState.CANCELED;
        } else if (squaredHypot(event.getRawX() - mDownPosX, event.getRawY() - mDownPosY) > mSquaredSlop) {
            if (isValidGestureAngle(Math.abs(mDownPosX - event.getRawX()), Math.abs(mDownPosY - event.getRawY()))) {
                mGestureState = GestureState.TRIGGERED;
            } else {
                mGestureState = GestureState.CANCELED;
            }
        }
    }

    @Override
    protected boolean isSupportGestureType(int gesture) {
        return gesture == GESTURE_GAME_MODE;
    }

    @Override
    protected int getSupportGestureType() {
        return GESTURE_GAME_MODE;
    }

    private boolean isInGameModeGestureArea(MotionEvent event) {
        switch (mDisplay.getRotation()) {
            case Surface.ROTATION_0:
            case Surface.ROTATION_180:
                return event.getRawX() < mGestureValidDistance && event.getRawY() < mGesturePortAreaBottom;
            case Surface.ROTATION_90:
            case Surface.ROTATION_270:
                return event.getRawX() < mGestureValidDistance && event.getRawY() < mGestureLandAreaBottom;
            default:
                return false;
        }
    }

    private boolean isValidGestureAngle(float absDeltaX, float absDeltaY) {
        final float angle = (float) Math.toDegrees(Math.atan2(absDeltaY, absDeltaX));
        return angle < ANGLE_THRESHOLD;
    }

    public static float squaredHypot(float x, float y) {
        return x * x + y * y;
    }
}
