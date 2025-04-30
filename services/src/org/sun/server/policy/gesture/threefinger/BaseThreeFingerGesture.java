/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.server.policy.gesture.threefinger;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

import static com.android.internal.policy.ThreeFingerGestureHelper.MAX_VALID_FINGER_DOWN_INTERVAL;

import static org.sun.os.DebugConstants.DEBUG_THREE_FINGER;

import android.content.Context;
import android.graphics.PointF;
import android.os.Handler;
import android.os.PowerManager;
import android.util.DisplayMetrics;
import android.util.Size;
import android.util.Slog;
import android.util.SparseArray;
import android.view.MotionEvent;

import com.android.internal.policy.ThreeFingerGestureHelper;

import org.sun.app.GameModeInfo;
import org.sun.server.policy.gesture.threefinger.ThreeFingerGestureListener.ActionMask;

public abstract class BaseThreeFingerGesture {

    protected final Context mContext;
    protected final Handler mHandler;
    protected final PowerManager mPowerManager;
    protected final Size mScreenSize;

    protected final SparseArray<PointF> mPoints = new SparseArray<>();

    protected boolean mEnabled;
    protected boolean mDisabledByGame;
    protected boolean mHandleNeeded;

    public BaseThreeFingerGesture(Context context, Handler handler) {
        mContext = context;
        mHandler = handler;

        final DisplayMetrics metrics = new DisplayMetrics();
        mContext.getDisplay().getRealMetrics(metrics);
        mScreenSize = new Size(Math.min(metrics.widthPixels, metrics.heightPixels),
                Math.max(metrics.widthPixels, metrics.heightPixels));
        Slog.d(getTag(), "screenSize is " + mScreenSize);

        mPowerManager = mContext.getSystemService(PowerManager.class);
    }

    protected void reset(MotionEvent event) {
        mHandleNeeded = false;
    }

    protected abstract String getTag();

    protected boolean isGestureValid(MotionEvent event) {
        if (event.getEventTime() - event.getDownTime() >= MAX_VALID_FINGER_DOWN_INTERVAL) {
            if (DEBUG_THREE_FINGER) {
                Slog.d(getTag(), "invalid finger down interval");
            }
            return false;
        }

        ThreeFingerGestureHelper.getPoints(event, mPoints);
        if (!ThreeFingerGestureHelper.validStartingPoints(mPoints)) {
            if (DEBUG_THREE_FINGER) {
                Slog.d(getTag(), "invalid start points");
            }
            return false;
        }

        if (!mPowerManager.isInteractive()) {
            if (DEBUG_THREE_FINGER) {
                Slog.d(getTag(), "is not interactive, return");
            }
            return false;
        }

        return true;
    }

    public boolean handlePointerEvent(ActionMask actionMask, MotionEvent event) {
        if (event.getPointerCount() != 3) {
            reset(event);
            return false;
        }

        if (!mEnabled || mDisabledByGame) {
            return false;
        }

        if (actionMask == ActionMask.DOWN) {
            mHandleNeeded = isGestureValid(event);
            return mHandleNeeded;
        }

        if (actionMask == ActionMask.MOVE) {
            handleFingerMoveEvent(event);
            return mHandleNeeded;
        }

        if (actionMask == ActionMask.UP || actionMask == ActionMask.CANCEL) {
            return mHandleNeeded && handleFingerUpEvent(event);
        }

        return false;
    }

    protected void handleFingerMoveEvent(MotionEvent event) {
    }

    protected boolean handleFingerUpEvent(MotionEvent event) {
        return false;
    }

    protected boolean isPortrait() {
        return mContext.getResources().getConfiguration().orientation == SCREEN_ORIENTATION_PORTRAIT;
    }

    protected void onGameModeInfoChanged(GameModeInfo info) {
        mDisabledByGame = info.shouldDisableThreeFingerGesture();
    }

    protected void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }
}
