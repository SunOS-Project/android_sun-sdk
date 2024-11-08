/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.server.policy.gesture;

import static android.os.Process.THREAD_PRIORITY_DEFAULT;

import android.content.Context;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Slog;
import android.view.Display;
import android.view.MotionEvent;
import android.view.WindowManager;

import com.android.server.LocalServices;
import com.android.server.input.InputManagerInternal;
import com.android.server.policy.PhoneWindowManagerExt;

import java.util.ArrayList;

import org.nameless.app.GameModeInfo;
import org.nameless.server.display.DisplayFeatureController;
import org.nameless.server.policy.gesture.GestureListenerBase.GestureState;
import org.nameless.view.ISystemGestureListener;

public class SystemGesture {

    private static final String TAG = "SystemGesture";

    public static final long GESTURE_TRIGGER_TIME_OUT = 300L;

    private final Context mContext;
    private final Display mDisplay;
    private final PowerManager mPowerManager;
    private final InputManagerInternal mInputManager;

    private final ArrayList<GestureListenerBase> mGestureListeners = new ArrayList<>();
    private final ArrayList<SystemGestureClient> mSystemGestureClients = new ArrayList<>();

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private final GameModeGestureListener mGameModeGestureListener;
    private final WindowModeGestureListener mWindowModeGestureListener;
    private final LeftRightGestureListener mLeftRightGestureListener;

    private GestureListenerBase mTargetGestureListener;

    private DisplayFeatureController mDisplayFeatureController;

    private boolean mTouching;

    public SystemGesture(Context context, PhoneWindowManagerExt ext) {
        mContext = context;
        mDisplay = context.getSystemService(WindowManager.class).getDefaultDisplay();
        mPowerManager = context.getSystemService(PowerManager.class);
        mInputManager = LocalServices.getService(InputManagerInternal.class);

        mGameModeGestureListener = new GameModeGestureListener(this, ext, mContext);
        mGestureListeners.add(mGameModeGestureListener);

        mWindowModeGestureListener = new WindowModeGestureListener(this, ext, mContext);
        mGestureListeners.add(mWindowModeGestureListener);

        mLeftRightGestureListener = new LeftRightGestureListener(this, ext, mContext);
        mGestureListeners.add(mLeftRightGestureListener);
    }

    Display getDisplay() {
        return mDisplay;
    }

    public void configure() {
        for (GestureListenerBase listener : mGestureListeners) {
            listener.configure();
        }
    }

    public void systemReady() {
        mDisplayFeatureController = DisplayFeatureController.getInstance();
        configure();
    }

    public void onGameModeInfoChanged(GameModeInfo info) {
        mWindowModeGestureListener.setDisabledByGame(info.isInGame());
    }

    public boolean interceptMotionBeforeQueueing(MotionEvent event) {
        mHandler.removeCallbacksAndMessages(null);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                setTouching(true);
                for (GestureListenerBase listener : mGestureListeners) {
                    if (listener.onActionDown(event)) {
                        mTargetGestureListener = listener;
                        mInputManager.notifySystemGestureDown();
                        mHandler.postDelayed(() -> {
                            mTargetGestureListener.reset();
                            mTargetGestureListener = null;
                            mInputManager.dispatchPendingSystemGesture();
                        }, GESTURE_TRIGGER_TIME_OUT);
                        return true;
                    }
                }
                return false;
            case MotionEvent.ACTION_MOVE:
                if (mTargetGestureListener == null) {
                    return false;
                }
                if (mTargetGestureListener.onActionMove(event)) {
                    if (mTargetGestureListener.mGestureState == GestureState.TRIGGERED) {
                        mPowerManager.userActivity(SystemClock.uptimeMillis(), false);
                    }
                    if (mTargetGestureListener.mGestureState == GestureState.CANCELED) {
                        mTargetGestureListener.reset();
                        mTargetGestureListener = null;
                        mInputManager.dispatchPendingSystemGesture();
                    }
                    return true;
                }
                return false;
            case MotionEvent.ACTION_UP:
                setTouching(false);
                if (mTargetGestureListener == null) {
                    return false;
                }
                final boolean ret = mTargetGestureListener.onActionUp(event);
                mTargetGestureListener = null;
                if (ret) {
                    mInputManager.dropPendingSystemGesture();
                } else {
                    mInputManager.dispatchPendingSystemGesture();
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                setTouching(false);
                if (mTargetGestureListener == null) {
                    return false;
                }
                mTargetGestureListener.onActionCancel(event);
                mInputManager.dropPendingSystemGesture();
                mTargetGestureListener = null;
                return true;
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_POINTER_UP:
                if (mTargetGestureListener == null) {
                    return false;
                }
                mTargetGestureListener.onActionMove(event);
                return true;
            default:
                return false;
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

    public boolean isTouching() {
        return mTouching;
    }

    private void setTouching(boolean touching) {
        if (touching) {
            mTouching = true;
        } else {
            mTouching = false;
            mDisplayFeatureController.maybeUpdateGameState();
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

    public void notifyBackGestureRegion(int left, int right) {
        mLeftRightGestureListener.setTouchRegion(left, right);
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
}
