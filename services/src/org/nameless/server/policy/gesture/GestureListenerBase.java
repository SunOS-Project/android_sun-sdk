/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.server.policy.gesture;

import static com.android.server.policy.WindowManagerPolicy.SYSTEM_GESTURE_NONE;

import static org.nameless.os.DebugConstants.DEBUG_PHONE_WINDOW_MANAGER;

import android.content.Context;
import android.graphics.Point;
import android.os.RemoteException;
import android.util.Slog;
import android.view.Display;
import android.view.MotionEvent;

import com.android.internal.R;

import com.android.server.policy.PhoneWindowManagerExt;

public abstract class GestureListenerBase implements IGestureListener {

    protected final String TAG = "GestureListenerBase";

    protected final long GESTURE_TRIGGER_TIME_OUT = 300L;

    protected final Context mContext;
    protected final Display mDisplay;
    protected final PhoneWindowManagerExt mPhoneWindowManagerExt;
    protected final SystemGesture mSystemGesture;

    protected SystemGestureClient mSystemGestureClient;

    protected GestureState mGestureState = GestureState.IDLE;
    protected boolean mGesturePreTriggerConsumed = false;
    protected long mMotionDownTime = 0L;

    protected boolean mDisabledByGame = false;

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

    protected abstract boolean shouldInterceptGesture();

    protected boolean dispatchCancelIfNeeded() {
        return false;
    }

    protected void setDisabledByGame(boolean disabled) {
        mDisabledByGame = disabled;
    }

    GestureListenerBase(SystemGesture systemGesture, PhoneWindowManagerExt ext, Context context) {
        mSystemGesture = systemGesture;
        mPhoneWindowManagerExt = ext;
        mContext = context;
        mDisplay = systemGesture.getDisplay();
        mGestureTouchSlop = getDefaultGestureTouchSlop();
    }

    protected void configure() {
        updateConfigureInfo();
    }

    @Override
    public boolean interceptMotionBeforeQueueing(MotionEvent event) {
        return false;
    }

    protected void onConfigureChanged() {
    }

    protected boolean hasRegisterClient() {
        return mSystemGestureClient != null;
    }

    protected void notifyGesturePreTrigger(MotionEvent event) {
        try {
            if (mSystemGestureClient != null && mSystemGestureClient.listener != null) {
                if (DEBUG_PHONE_WINDOW_MANAGER && event.getAction() != MotionEvent.ACTION_MOVE) {
                    Slog.i(TAG, "notifyGesturePreTrigger, client=" + mSystemGestureClient.pkg +
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
                    Slog.i(TAG, "notifyGestureTriggered, client=" + mSystemGestureClient.pkg +
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
                    Slog.i(TAG, "notifyGestureCanceled, client=" + mSystemGestureClient.pkg +
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
                    Slog.i(TAG, "notifyGesturePreTriggerBefore, client=" + mSystemGestureClient.pkg +
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
                Slog.d(TAG, "updateConfigureInfo, mDeviceHeight=" + mDeviceHeight + ", mDeviceWidth=" + mDeviceWidth);
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
