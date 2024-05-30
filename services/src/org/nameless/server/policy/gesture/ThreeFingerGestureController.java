/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.server.policy.gesture;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Display;

import com.android.server.policy.PhoneWindowManagerExt;
import com.android.server.wm.WindowManagerService;

import org.nameless.app.GameModeInfo;
import org.nameless.server.policy.gesture.threefinger.ThreeFingerGestureListener;

public class ThreeFingerGestureController {

    private static class InstanceHolder {
        private static final ThreeFingerGestureController INSTANCE = new ThreeFingerGestureController();
    }

    public static ThreeFingerGestureController getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private final Handler mHandler;
    private final HandlerThread mHandlerThread;

    private Context mContext;
    private ThreeFingerGestureListener mThreeFingerGestureListener;
    private WindowManagerService mWms;

    private boolean mBootCompleted = false;
    private boolean mRegistered = false;

    private ThreeFingerGestureController() {
        mHandlerThread = new HandlerThread("ThreeFingerGestureController-handler");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }

    public void init(Context context, WindowManagerService wms) {
        mContext = context;
        mWms = wms;
    }

    public void onBootCompleted() {
        mThreeFingerGestureListener = new ThreeFingerGestureListener(mContext, mHandler);
        mBootCompleted = true;
        updateListenerState();
    }

    public void onGameModeInfoChanged(GameModeInfo info) {
        if (mBootCompleted) {
            mThreeFingerGestureListener.onGameModeInfoChanged(info);
        }
    }

    public void updateListenerState() {
        if (!mBootCompleted) {
            return;
        }
        final boolean enabled = PhoneWindowManagerExt.getInstance().isThreeFingerGestureOn();
        if (enabled != mRegistered) {
            if (enabled) {
                mWms.registerPointerEventListener(mThreeFingerGestureListener, Display.DEFAULT_DISPLAY);
            } else {
                mWms.unregisterPointerEventListener(mThreeFingerGestureListener, Display.DEFAULT_DISPLAY);
            }
            mRegistered = enabled;
        }
        mThreeFingerGestureListener.updateEnabled();
    }
}
