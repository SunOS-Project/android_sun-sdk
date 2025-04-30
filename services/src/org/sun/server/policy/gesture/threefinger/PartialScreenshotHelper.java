/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.server.policy.gesture.threefinger;

import static com.android.internal.policy.ThreeFingerGestureHelper.MESSAGE_TYPE_FINGER_MOVE;
import static com.android.internal.policy.ThreeFingerGestureHelper.MESSAGE_TYPE_FINGER_UP;
import static com.android.internal.policy.ThreeFingerGestureHelper.MESSAGE_TYPE_SHOW;

import static org.sun.os.DebugConstants.DEBUG_THREE_FINGER;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.PointF;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.UserHandle;
import android.util.Slog;
import android.util.SparseArray;
import android.view.MotionEvent;

import com.android.internal.policy.ThreeFingerGestureHelper;

public class PartialScreenshotHelper {

    private static final String TAG = "ThreeFinger::PartialScreenshotHelper";

    private static final long SERVICE_BIND_TIMEOUT = 10000L;

    private final Context mContext;
    private final Handler mHandler;
    private final HandlerThread mHandlerThread;
    private final Messenger mReplyToMessenger;

    private final Object mLock = new Object();

    private final Runnable mTimeoutRunnable = () -> {
        onTimeout();
    };

    private Messenger mSendFromMessenger = null;
    private ServiceConnection mServiceConnection = null;

    class ScreenshotServiceConnection implements ServiceConnection {

        private final SparseArray<PointF> mPoints;

        ScreenshotServiceConnection(SparseArray<PointF> points) {
            mPoints = points;
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            synchronized (mLock) {
                if (mServiceConnection != this) {
                    return;
                }
                if (DEBUG_THREE_FINGER) {
                    Slog.d(TAG, "onServiceConnected");
                }
                mSendFromMessenger = new Messenger(binder);
                sendMessage(MESSAGE_TYPE_SHOW, getMinY(mPoints));
                mHandler.removeCallbacks(mTimeoutRunnable);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            synchronized (mLock) {
                if (mServiceConnection != null) {
                    resetConnection();
                    if (mHandler.hasCallbacks(mTimeoutRunnable)) {
                        Slog.e(TAG, "service disconnected");
                        mHandler.removeCallbacks(mTimeoutRunnable);
                    }
                }
            }
        }
    }

    public PartialScreenshotHelper(Context context) {
        mContext = context;
        mHandlerThread = new HandlerThread(TAG);
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
        mReplyToMessenger = new Messenger(new Handler(mHandler.getLooper()) {
            @Override
            public void handleMessage(Message message) {
                synchronized (mLock) {
                    resetConnection();
                }
            }
        });
    }

    public boolean hasConnection() {
        return mServiceConnection != null && mSendFromMessenger != null;
    }

    public void resetConnection() {
        if (mServiceConnection == null) {
            return;
        }
        if (DEBUG_THREE_FINGER) {
            Slog.d(TAG, "resetConnection");
        }
        mContext.unbindService(mServiceConnection);
        mServiceConnection = null;
        mSendFromMessenger = null;
    }

    public void onTimeout() {
        synchronized (mLock) {
            if (mServiceConnection != null) {
                Slog.e(TAG, "Timed out connected");
                resetConnection();
            }
        }
    }

    public void sendMessage(int type, int minY) {
        try {
            mSendFromMessenger.send(Message.obtain(null, type, minY, 0));
        } catch (Exception e) {
            Slog.e(TAG, "sendMessage: type= " + type + " failed.", e);
        }
    }

    public void sendShowMessage(SparseArray<PointF> points) {
        synchronized (mLock) {
            if (!hasConnection()) {
                final ComponentName component = ComponentName.unflattenFromString(
                        "com.nothing.ntscreenshot/.PartialScreenshotService");
                final Intent intent = new Intent();
                intent.setComponent(component);
                final ScreenshotServiceConnection connection = new ScreenshotServiceConnection(points);
                if (mContext.bindServiceAsUser(intent, connection,
                        Context.BIND_AUTO_CREATE | Context.BIND_NOT_VISIBLE,
                        UserHandle.CURRENT)) {
                    mServiceConnection = connection;
                    mHandler.postDelayed(mTimeoutRunnable, SERVICE_BIND_TIMEOUT);
                } else {
                    Slog.e(TAG, "bind service failed");
                    mContext.unbindService(connection);
                }
            }

            if (hasConnection()) {
                sendMessage(MESSAGE_TYPE_SHOW, getMinY(points));
            }
        }
    }

    public void sendFingerUpMessage(MotionEvent motionEvent) {
        if (!hasConnection()) {
            return;
        }
        final SparseArray<PointF> points = new SparseArray<>();
        ThreeFingerGestureHelper.getPoints(motionEvent, points);
        try {
            final Message obtain = Message.obtain(null, MESSAGE_TYPE_FINGER_UP, getMinY(points), 0);
            obtain.replyTo = mReplyToMessenger;
            mSendFromMessenger.send(obtain);
        } catch (Exception e) {
            Slog.e(TAG, "touch up sendMessage failed", e);
        }
    }

    public void sendFingerMoveMessage(MotionEvent motionEvent) {
        if (!hasConnection()) {
            return;
        }
        final SparseArray<PointF> points = new SparseArray<>();
        ThreeFingerGestureHelper.getPoints(motionEvent, points);
        sendMessage(MESSAGE_TYPE_FINGER_MOVE, getMinY(points));
    }

    public int getMinY(SparseArray<PointF> points) {
        float minY = Float.MAX_VALUE;
        for (int i = 0; i < points.size(); i++) {
            final PointF p = points.valueAt(i);
            if (p.y < minY) {
                minY = p.y;
            }
        }
        return (int) minY;
    }
}
