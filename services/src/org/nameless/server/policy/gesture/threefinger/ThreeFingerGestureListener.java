/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.server.policy.gesture.threefinger;

import static org.nameless.os.DebugConstants.DEBUG_THREE_FINGER;

import android.content.Context;
import android.os.Handler;
import android.util.Slog;
import android.view.MotionEvent;
import android.view.WindowManagerPolicyConstants.PointerEventListener;

import com.android.server.policy.PhoneWindowManagerExt;

import org.nameless.app.GameModeInfo;
import org.nameless.server.policy.gesture.ThreeFingerGestureController;

public class ThreeFingerGestureListener implements PointerEventListener {

    private static final String TAG = "ThreeFingerGestureListener";

    private final SwipeDownGesture mSwipeDownGesture;
    private final TouchHoldGesture mTouchHoldGesture;

    enum ActionMask {
        NONE,
        FIRST_DOWN,
        DOWN,
        UP,
        MOVE,
        LAST_UP,
        CANCEL
    }

    public ThreeFingerGestureListener(Context context, Handler handler) {
        mSwipeDownGesture = new SwipeDownGesture(context, handler);
        mTouchHoldGesture = new TouchHoldGesture(context, handler);
    }

    private ActionMask getActionMask(int action) {
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                return ActionMask.FIRST_DOWN;
            case MotionEvent.ACTION_UP:
                return ActionMask.LAST_UP;
            case MotionEvent.ACTION_MOVE:
                return ActionMask.MOVE;
            case MotionEvent.ACTION_CANCEL:
                return ActionMask.CANCEL;
            default:
                final int actionMasked = action & MotionEvent.ACTION_MASK;
                if (actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
                    return ActionMask.DOWN;
                }
                if (actionMasked == MotionEvent.ACTION_POINTER_UP) {
                    return ActionMask.UP;
                }
                return ActionMask.NONE;
        }
    }

    public void onGameModeInfoChanged(GameModeInfo info) {
        mSwipeDownGesture.onGameModeInfoChanged(info);
        mTouchHoldGesture.onGameModeInfoChanged(info);
    }

    @Override
    public void onPointerEvent(MotionEvent event) {
        final ActionMask actionMask = getActionMask(event.getAction());
        final boolean handled = mTouchHoldGesture.handlePointerEvent(actionMask, event);
        if ((actionMask == ActionMask.UP || actionMask == ActionMask.CANCEL) && handled) {
            if (DEBUG_THREE_FINGER) {
                Slog.d(TAG, "ACTION_UP handle by 3-finger touch & hold");
            }
            mSwipeDownGesture.reset(event);
        } else {
            mSwipeDownGesture.handlePointerEvent(actionMask, event);
        }
    }

    public void updateEnabled() {
        mSwipeDownGesture.setEnabled(PhoneWindowManagerExt.getInstance().isThreeFingerSwipeOn());
        mTouchHoldGesture.setEnabled(PhoneWindowManagerExt.getInstance().isThreeFingerHoldOn());
    }
}
