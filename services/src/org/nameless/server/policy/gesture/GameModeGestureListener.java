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

    private float mDownPosX = 0.0f;
    private float mDownPosY = 0.0f;
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
        mGestureValidDistance = (float) ResourceUtils.getGameModeGestureValidDistance(mContext.getResources());
        mGesturePortAreaBottom = (float) ResourceUtils.getGameModePortraitAreaBottom(mContext.getResources());
        mGestureLandAreaBottom = (float) ResourceUtils.getGameModeLandscapeAreaBottom(mContext.getResources());
        if (DEBUG_PHONE_WINDOW_MANAGER) {
            Slog.d(TAG, ", mGestureValidDistance = " + mGestureValidDistance
                    + ", mGesturePortAreaBottom = " + mGesturePortAreaBottom
                    + ", mGestureLandAreaBottom = " + mGestureLandAreaBottom);
        }
    }

    @Override
    protected int getDefaultGestureTouchSlop() {
        return mContext.getResources().getDimensionPixelSize(R.dimen.game_mode_gesture_touch_slop);
    }

    @Override
    public boolean interceptMotionBeforeQueueing(MotionEvent event) {
        boolean result = false;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (!hasRegisterClient()) {
                    return false;
                }
                mDownPosX = event.getRawX();
                mDownPosY = event.getRawY();
                mMotionDownTime = System.currentTimeMillis();
                if (isInGameModeGestureArea(event)) {
                    mGesturePreTriggerConsumed = notifyGesturePreTriggerBefore(event);
                } else {
                    mGesturePreTriggerConsumed = false;
                }
                if (mGesturePreTriggerConsumed) {
                    mGestureState = GestureListenerBase.GestureState.PENDING_CHECK;
                }
                if (DEBUG_PHONE_WINDOW_MANAGER) {
                    Slog.d(TAG, "interceptMotionBeforeQueueing, mGesturePreTriggerConsumed="
                            + mGesturePreTriggerConsumed + ", mGestureState=" + mGestureState);
                }
                if (mGestureState != GestureListenerBase.GestureState.TRIGGERED && mGestureState != GestureListenerBase.GestureState.PENDING_CHECK) {
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
                if (mGestureState == GestureListenerBase.GestureState.PENDING_CHECK) {
                    checkGameModeGesture(event);
                }
                if (mGestureState == GestureListenerBase.GestureState.TRIGGERED) {
                    notifyGestureTriggered(event);
                }
                return mGestureState != GestureState.IDLE;
            default:
                return result;
        }
    }

    private void checkGameModeGesture(MotionEvent event) {
        if (System.currentTimeMillis() - mMotionDownTime > GESTURE_TRIGGER_TIME_OUT) {
            if (DEBUG_PHONE_WINDOW_MANAGER) {
                Slog.d(TAG, "Game mode gesture time out");
            }
            mGestureState = GestureListenerBase.GestureState.CANCELED;
        } else if (squaredHypot(event.getRawX() - mDownPosX, event.getRawY() - mDownPosY) > mSquaredSlop) {
            if (isValidGestureAngle(Math.abs(mDownPosX - event.getRawX()), Math.abs(mDownPosY - event.getRawY()))) {
                mGestureState = GestureListenerBase.GestureState.TRIGGERED;
            } else {
                mGestureState = GestureListenerBase.GestureState.CANCELED;
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
