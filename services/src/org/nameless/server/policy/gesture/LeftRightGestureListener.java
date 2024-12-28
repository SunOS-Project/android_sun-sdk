/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.server.policy.gesture;

import static org.nameless.os.DebugConstants.DEBUG_PHONE_WINDOW_MANAGER;
import static org.nameless.view.ISystemGestureListener.GESTURE_LEFT_RIGHT;

import android.content.Context;
import android.util.Slog;
import android.view.MotionEvent;

import com.android.internal.R;

import com.android.server.policy.PhoneWindowManagerExt;

import org.nameless.server.app.GameModeController;

public class LeftRightGestureListener extends GestureListenerBase {

    private static final String TAG = "LeftRightGestureListener";

    private int mTouchLeft = Integer.MAX_VALUE;
    private int mTouchRight = 0;

    private float mTouchSlop;

    private boolean mIsLeft;
    private boolean mBlockedDispatch;

    public LeftRightGestureListener(SystemGesture SystemGesture,
            PhoneWindowManagerExt ext, Context context) {
        super(SystemGesture, ext, context, TAG);
    }

    @Override
    protected void onConfigureChanged() {
        super.onConfigureChanged();
        mTouchSlop = getGestureTouchSlop();
    }

    @Override
    public boolean onActionDown(MotionEvent event) {
        if (!hasRegisterClient()) {
            return false;
        }
        super.onActionDown(event);
        if (mDownPosX > mTouchLeft && mDownPosX < mTouchRight) {
            return false;
        }
        mIsLeft = mDownPosX <= mTouchLeft;
        mGesturePreTriggerConsumed = notifyGesturePreTriggerBefore(event);
        if (mGesturePreTriggerConsumed) {
            mGestureState = GestureState.PENDING_CHECK;
        }
        if (DEBUG_PHONE_WINDOW_MANAGER) {
            Slog.d(TAG, "onActionDown, mGesturePreTriggerConsumed=" + mGesturePreTriggerConsumed
                    + ", mGestureState=" + mGestureState + ", mIsLeft=" + mIsLeft);
        }
        if (mGestureState != GestureState.PENDING_CHECK) {
            return false;
        }
        return true;
    }

    @Override
    public boolean onActionMove(MotionEvent event) {
        if (mGestureState == GestureState.PENDING_CHECK) {
            checkLeftRightGesture(event);
            if (!mBlockedDispatch && !notifyGesturePreTriggerBefore(event)) {
                mGestureState = GestureState.CANCELED;
            }
        } else if (!mBlockedDispatch && mGestureState == GestureState.TRIGGERED) {
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
            if (!mBlockedDispatch) {
                notifyGestureTriggered(event);
            }
        } else {
            ret = false;
            notifyGestureCanceled();
        }
        if (DEBUG_PHONE_WINDOW_MANAGER) {
            Slog.d(TAG, "onActionUp, mGestureState=" + mGestureState);
        }
        super.onActionUp(event);
        return ret;
    }

    @Override
    public void onActionCancel(MotionEvent event) {
        notifyGestureCanceled();
        if (DEBUG_PHONE_WINDOW_MANAGER) {
            Slog.d(TAG, "onActionCancel");
        }
        super.onActionCancel(event);
    }

    private void checkLeftRightGesture(MotionEvent event) {
        final float dx = event.getRawX() - mDownPosX;
        if ((mIsLeft && dx < 0) || (!mIsLeft && dx > 0)) {
            if (DEBUG_PHONE_WINDOW_MANAGER) {
                Slog.d(TAG, "checkLeftRightGesture, invalid x direction");
            }
            mGestureState = GestureState.CANCELED;
            return;
        }
        final float dxAbs = Math.abs(event.getRawX() - mDownPosX);
        final float dyAbs = Math.abs(event.getRawY() - mDownPosY);
        if (dyAbs > dxAbs && dyAbs > mTouchSlop) {
            if (DEBUG_PHONE_WINDOW_MANAGER) {
                Slog.d(TAG, "checkLeftRightGesture, invalid y direction");
            }
            mGestureState = GestureState.CANCELED;
            return;
        }
        if (dxAbs > dyAbs && dxAbs > mTouchSlop) {
            if (GameModeController.getInstance().isGestureLocked(GameModeController.GESTURE_TYPE_BACK)) {
                mBlockedDispatch = true;
                GameModeController.getInstance().warnGestureLocked();
                if (DEBUG_PHONE_WINDOW_MANAGER) {
                    Slog.d(TAG, "checkLeftRightGesture, gesture locked");
                }
            }
            mGestureState = GestureState.TRIGGERED;
        }
    }

    @Override
    public void reset() {
        super.reset();
        mBlockedDispatch = false;
    }

    @Override
    protected boolean isSupportGestureType(int gesture) {
        return gesture == GESTURE_LEFT_RIGHT;
    }

    @Override
    protected int getSupportGestureType() {
        return GESTURE_LEFT_RIGHT;
    }

    void setTouchRegion(int left, int right) {
        mTouchLeft = left;
        mTouchRight = right;
        if (DEBUG_PHONE_WINDOW_MANAGER) {
            Slog.d(TAG, "setTouchRegion, mTouchLeft=" + mTouchLeft + ", mTouchRight=" + mTouchRight);
        }
    }
}
