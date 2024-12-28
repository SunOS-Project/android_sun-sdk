/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.server.policy.gesture;

import static org.nameless.os.DebugConstants.DEBUG_PHONE_WINDOW_MANAGER;
import static org.nameless.server.policy.gesture.SystemGesture.GESTURE_TRIGGER_TIME_OUT;

import android.content.Context;
import android.graphics.Point;
import android.os.RemoteException;
import android.util.Slog;
import android.view.Display;
import android.view.MotionEvent;

import com.android.internal.R;

import com.android.server.policy.PhoneWindowManagerExt;

public abstract class GestureListenerBase {

    protected final Context mContext;
    protected final Display mDisplay;
    protected final PhoneWindowManagerExt mPhoneWindowManagerExt;
    protected final SystemGesture mSystemGesture;

    protected String mTag = "GestureListenerBase";

    protected SystemGestureClient mSystemGestureClient;

    protected GestureState mGestureState = GestureState.IDLE;
    protected GestureState mLastMoveGestureState = GestureState.IDLE;
    protected boolean mGesturePreTriggerConsumed;
    protected float mDownPosX;
    protected float mDownPosY;
    protected long mDownTime;

    protected boolean mDisabledByGame;

    protected int mDeviceHeight;
    protected int mDeviceWidth;

    private int mGestureTouchSlop;

    protected enum GestureState {
        IDLE,
        PENDING_CHECK,
        CANCELED,
        TRIGGERED
    }

    protected abstract int getSupportGestureType();

    protected abstract boolean isSupportGestureType(int gesture);

    protected void setDisabledByGame(boolean disabled) {
        mDisabledByGame = disabled;
    }

    GestureListenerBase(SystemGesture systemGesture, PhoneWindowManagerExt ext,
            Context context, String tag) {
        mSystemGesture = systemGesture;
        mPhoneWindowManagerExt = ext;
        mContext = context;
        mTag = tag;
        mDisplay = systemGesture.getDisplay();
        mGestureTouchSlop = getDefaultGestureTouchSlop();
    }

    protected void configure() {
        updateConfigureInfo();
    }

    public void reset() {
        mGesturePreTriggerConsumed = false;
        mGestureState = GestureState.IDLE;
        mLastMoveGestureState = GestureState.IDLE;
        mDownPosX = 0f;
        mDownPosY = 0f;
        mDownTime = 0L;
    }

    public boolean onActionDown(MotionEvent event) {
        mDownPosX = event.getRawX();
        mDownPosY = event.getRawY();
        mDownTime = System.currentTimeMillis();
        return false;
    }

    public boolean onActionMove(MotionEvent event) {
        return false;
    }

    public boolean onActionUp(MotionEvent event) {
        reset();
        return false;
    }

    public void onActionCancel(MotionEvent event) {
        reset();
    }

    protected void onConfigureChanged() {
        reset();
    }

    protected boolean hasRegisterClient() {
        return mSystemGestureClient != null;
    }

    protected boolean isAlreadyTimeout() {
        return System.currentTimeMillis() - mDownTime > GESTURE_TRIGGER_TIME_OUT;
    }

    protected void notifyGesturePreTrigger(MotionEvent event) {
        try {
            if (mSystemGestureClient != null && mSystemGestureClient.listener != null) {
                if (DEBUG_PHONE_WINDOW_MANAGER && event.getAction() != MotionEvent.ACTION_MOVE) {
                    Slog.d(mTag, "notifyGesturePreTrigger, client=" + mSystemGestureClient.pkg +
                            ", supportGesture=" + getSupportGestureType() + ", " + motionEventToString(event));
                }
                mSystemGestureClient.listener.onGesturePreTrigger(getSupportGestureType(), event);
            }
        } catch (RemoteException e) {
        }
    }

    protected void notifyGestureTriggered(MotionEvent event) {
        try {
            if (mSystemGestureClient != null && mSystemGestureClient.listener != null) {
                if (DEBUG_PHONE_WINDOW_MANAGER && event.getAction() != MotionEvent.ACTION_MOVE) {
                    Slog.d(mTag, "notifyGestureTriggered, client=" + mSystemGestureClient.pkg +
                            ", supportGesture=" + getSupportGestureType() + ", " + motionEventToString(event));
                }
                mSystemGestureClient.listener.onGestureTriggered(getSupportGestureType(), event);
            }
        } catch (RemoteException e) {
        }
    }

    protected void notifyGestureCanceled() {
        try {
            if (mSystemGestureClient != null && mSystemGestureClient.listener != null) {
                if (DEBUG_PHONE_WINDOW_MANAGER) {
                    Slog.d(mTag, "notifyGestureCanceled, client=" + mSystemGestureClient.pkg +
                            ", supportGesture=" + getSupportGestureType());
                }
                mSystemGestureClient.listener.onGestureCanceled(getSupportGestureType());
            }
        } catch (RemoteException e) {
        }
    }

    protected boolean notifyGesturePreTriggerBefore(MotionEvent event) {
        try {
            if (mSystemGestureClient != null && mSystemGestureClient.listener != null) {
                final boolean consumed = mSystemGestureClient.listener.onGesturePreTriggerBefore(
                        getSupportGestureType(), event);
                if (DEBUG_PHONE_WINDOW_MANAGER && event.getAction() != MotionEvent.ACTION_MOVE) {
                    Slog.d(mTag, "notifyGesturePreTriggerBefore, client=" + mSystemGestureClient.pkg +
                            ", consumed=" + consumed + ", " + motionEventToString(event));
                }
                return consumed;
            }
            return false;
        } catch (RemoteException e) {
            return false;
        }
    }

    void setSystemGestureClient(SystemGestureClient client) {
        if (client != null && !isSupportGestureType(client.gesture)) {
            throw new IllegalArgumentException("Gesture type not support");
        }
        mSystemGestureClient = client;
    }

    int getGestureTouchSlop() {
        return mGestureTouchSlop;
    }

    protected int getDefaultGestureTouchSlop() {
        return mContext.getResources().getDimensionPixelSize(R.dimen.bottom_gesture_touch_slop);
    }

    private void updateConfigureInfo() {
        final Point point = new Point();
        mDisplay.getRealSize(point);
        final int width = point.x;
        final int height = point.y;
        final int currentHeight = Math.max(height, width);
        final int currentWidth = Math.min(height, width);
        if (currentHeight != mDeviceHeight || currentWidth != mDeviceWidth) {
            mDeviceHeight = currentHeight;
            mDeviceWidth = currentWidth;
            onConfigureChanged();
            if (DEBUG_PHONE_WINDOW_MANAGER) {
                Slog.d(mTag, "updateConfigureInfo, mDeviceHeight=" + mDeviceHeight + ", mDeviceWidth=" + mDeviceWidth);
            }
        }
    }

    public static String motionEventToString(MotionEvent event) {
        if (event == null) {
            return null;
        }
        final StringBuilder msg = new StringBuilder();
        msg.append("MotionEvent { action=").append(MotionEvent.actionToString(event.getAction()));
        final int pointerCount = event.getPointerCount();
        for (int i = 0; i < pointerCount; i++) {
            final float x = event.getX(i);
            final float y = event.getY(i);
            if (x != 0.0f || y != 0.0f) {
                msg.append(", x[").append(i).append("]=").append(x);
                msg.append(", y[").append(i).append("]=").append(y);
            }
        }
        msg.append(", eventTime=").append(event.getEventTime());
        msg.append(", downTime=").append(event.getDownTime());
        msg.append(" }");
        return msg.toString();
    }
}
