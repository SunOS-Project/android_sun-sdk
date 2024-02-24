/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.server.policy.gesture;

import static android.os.Process.THREAD_PRIORITY_DEFAULT;

import static com.android.server.policy.WindowManagerPolicy.SYSTEM_GESTURE_CANCEL;
import static com.android.server.policy.WindowManagerPolicy.SYSTEM_GESTURE_DOWN;
import static com.android.server.policy.WindowManagerPolicy.SYSTEM_GESTURE_MOVE;
import static com.android.server.policy.WindowManagerPolicy.SYSTEM_GESTURE_MOVE_TRIGGERED;
import static com.android.server.policy.WindowManagerPolicy.SYSTEM_GESTURE_NONE;
import static com.android.server.policy.WindowManagerPolicy.SYSTEM_GESTURE_UP;
import static com.android.server.policy.WindowManagerPolicy.SYSTEM_GESTURE_UP_TRIGGERED;

import android.app.Instrumentation;
import android.content.Context;
import android.graphics.PointF;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Slog;
import android.view.Display;
import android.view.MotionEvent;
import android.view.WindowManager;

import com.android.server.ServiceThread;
import com.android.server.policy.PhoneWindowManager;
import com.android.server.policy.PhoneWindowManagerExt;

import java.util.ArrayList;

import org.nameless.server.policy.gesture.GestureListenerBase.GestureState;
import org.nameless.view.ISystemGestureListener;

public class SystemGesture {

    private static final String TAG = "SystemGesture";

    private static final int MSG_DISPATCH_MOVE = 1;

    private final Context mContext;
    private final Display mDisplay;
    private final PhoneWindowManagerExt mPhoneWindowManagerExt;

    private final ArrayList<GestureListenerBase> mGestureListeners = new ArrayList<>();
    private final ArrayList<SystemGestureClient> mSystemGestureClients = new ArrayList<>();

    private final Handler mHandler;
    private final ServiceThread mServiceThread;

    private PointF mLastDownPos;

    private GestureListenerBase mTargetGestureListener;

    public SystemGesture(Context context, PhoneWindowManagerExt ext) {
        mPhoneWindowManagerExt = ext;
        mContext = context;
        mDisplay = context.getSystemService(WindowManager.class).getDefaultDisplay();

        mServiceThread = new ServiceThread(TAG, THREAD_PRIORITY_DEFAULT, false);
        mServiceThread.start();
        mHandler = new H(mServiceThread.getLooper());

        mGestureListeners.add(new WindowModeGestureListener(this, mContext));
        mGestureListeners.add(new ThreeFingerGestureListener(this, mContext, mServiceThread.getLooper()));
    }

    Display getDisplay() {
        return mDisplay;
    }

    PhoneWindowManagerExt getPhoneWindowManagerExt() {
        return mPhoneWindowManagerExt;
    }

    public void configure() {
        for (GestureListenerBase listener : mGestureListeners) {
            listener.configure();
        }
    }

    public int interceptMotionBeforeQueueing(MotionEvent event) {
        if (mHandler.hasMessages(MSG_DISPATCH_MOVE)) {
            mHandler.removeMessages(MSG_DISPATCH_MOVE);
        }

        boolean intercept = false;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                for (GestureListenerBase listener : mGestureListeners) {
                    if (listener.interceptMotionBeforeQueueing(event)) {
                        intercept = listener.shouldInterceptGesture();
                        mTargetGestureListener = listener;
                        mLastDownPos = new PointF(event.getRawX(), event.getRawY());
                        if (intercept) {
                            mHandler.sendEmptyMessageDelayed(MSG_DISPATCH_MOVE, 300L);
                        }
                        break;
                    }
                }
                return intercept ? SYSTEM_GESTURE_DOWN : SYSTEM_GESTURE_NONE;
            case MotionEvent.ACTION_MOVE:
                if (mTargetGestureListener == null) {
                    return SYSTEM_GESTURE_NONE;
                }
                if (mTargetGestureListener.interceptMotionBeforeQueueing(event)) {
                    if (mTargetGestureListener.dispatchCancelIfNeeded()) {
                        return SYSTEM_GESTURE_CANCEL;
                    }
                    if (!mTargetGestureListener.shouldInterceptGesture()) {
                        return SYSTEM_GESTURE_NONE;
                    }
                    return mTargetGestureListener.mGestureState == GestureState.TRIGGERED
                            ? SYSTEM_GESTURE_MOVE_TRIGGERED : SYSTEM_GESTURE_MOVE;
                }
                return SYSTEM_GESTURE_NONE;
            case MotionEvent.ACTION_UP:
                if (mTargetGestureListener == null) {
                    return SYSTEM_GESTURE_NONE;
                }
                final int ret;
                if (mTargetGestureListener.interceptMotionBeforeQueueing(event)) {
                    if (!mTargetGestureListener.shouldInterceptGesture()) {
                        ret = SYSTEM_GESTURE_NONE;
                    } else {
                        ret = mTargetGestureListener.mGestureState == GestureState.TRIGGERED
                                ? SYSTEM_GESTURE_UP_TRIGGERED : SYSTEM_GESTURE_UP;
                    }
                    mTargetGestureListener.mGestureState = GestureState.IDLE;
                } else {
                    ret = SYSTEM_GESTURE_NONE;
                }
                mTargetGestureListener = null;
                return ret;
            case MotionEvent.ACTION_POINTER_DOWN:
                if (mTargetGestureListener == null) {
                    return SYSTEM_GESTURE_NONE;
                }
                if (mTargetGestureListener.interceptMotionBeforeQueueing(event)) {
                    if (mTargetGestureListener.dispatchCancelIfNeeded()) {
                        return SYSTEM_GESTURE_CANCEL;
                    }
                }
                return SYSTEM_GESTURE_NONE;
            case MotionEvent.ACTION_POINTER_UP:
                if (mTargetGestureListener != null) {
                    mTargetGestureListener.interceptMotionBeforeQueueing(event);
                }
                return SYSTEM_GESTURE_NONE;
            default:
                return SYSTEM_GESTURE_NONE;
        }
    }

    private void onGestureListenerUpdate() {
        synchronized (mSystemGestureClients) {
            for (GestureListenerBase listener : mGestureListeners) {
                listener.setSystemGestureClient(null);
                for (int i = 0; i < mSystemGestureClients.size(); ++i) {
                    final SystemGestureClient client = mSystemGestureClients.get(i);
                    if (client != null && listener.isSupportGestureType(client.gesture)) {
                        listener.setSystemGestureClient(client);
                        break;
                    }
                }
            }
        }
    }

    public void registerSystemGestureListener(String pkg, int gesture, ISystemGestureListener listener)
            throws RemoteException {
        if (listener != null) {
            final SystemGestureClient client = new SystemGestureClient(listener.asBinder(),
                    pkg, Binder.getCallingUid(), gesture, listener);
            synchronized (mSystemGestureClients) {
                if (mSystemGestureClients.contains(client)) {
                    throw new RemoteException("Duplicate register because of same parameters");
                }
                mSystemGestureClients.add(client);
            }
            final BinderDeath binderDeath = new BinderDeath(pkg, Binder.getCallingUid(), gesture, client);
            listener.asBinder().linkToDeath(binderDeath, 0);
            onGestureListenerUpdate();
        }
    }

    public void unregisterSystemGestureListener(String pkg, int gesture, ISystemGestureListener listener) {
        final SystemGestureClient client = new SystemGestureClient(listener.asBinder(),
                pkg, Binder.getCallingUid(), gesture, listener);
        synchronized (mSystemGestureClients) {
            mSystemGestureClients.remove(client);
            onGestureListenerUpdate();
        }
    }

    private final class BinderDeath implements IBinder.DeathRecipient {
        private String mPkg;
        private int mUid;
        private int mGesture;
        private SystemGestureClient mClient;

        BinderDeath(String pkg, int uid, int gesture, SystemGestureClient client) {
            mPkg = pkg;
            mUid = uid;
            mGesture = gesture;
            mClient = client;
        }

        @Override
        public void binderDied() {
            Slog.w(TAG, "Death received from " + mPkg + ", with gesture " + mGesture + " for uid " + mUid);
            synchronized (mSystemGestureClients) {
                mSystemGestureClients.remove(mClient);
            }
        }
    }

    private final class H extends Handler {
        H(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case MSG_DISPATCH_MOVE:
                    if (mLastDownPos != null) {
                        final Instrumentation inst = new Instrumentation();
                        try {
                            inst.sendPointerSync(MotionEvent.obtain(SystemClock.uptimeMillis(),
                                    SystemClock.uptimeMillis(), MotionEvent.ACTION_MOVE,
                                    mLastDownPos.x, mLastDownPos.y, 0));
                        } catch (Exception e) {
                            Slog.w(TAG, "Failed to dispatch move motion event");
                        }
                        mLastDownPos = null;
                    }
                    break;
            }
        }
    }
}
