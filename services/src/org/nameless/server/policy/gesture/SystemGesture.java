/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.server.policy.gesture;

import static android.os.Process.THREAD_PRIORITY_DEFAULT;

import static com.android.server.policy.WindowManagerPolicy.SYSTEM_GESTURE_DOWN;
import static com.android.server.policy.WindowManagerPolicy.SYSTEM_GESTURE_MOVE;
import static com.android.server.policy.WindowManagerPolicy.SYSTEM_GESTURE_MOVE_TRIGGERED;
import static com.android.server.policy.WindowManagerPolicy.SYSTEM_GESTURE_NONE;
import static com.android.server.policy.WindowManagerPolicy.SYSTEM_GESTURE_RESET;

import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Slog;
import android.view.Display;
import android.view.MotionEvent;
import android.view.WindowManager;

import com.android.server.policy.PhoneWindowManager;
import com.android.server.policy.PhoneWindowManagerExt;

import java.util.ArrayList;

import org.nameless.app.GameModeInfo;
import org.nameless.server.display.DisplayFeatureController;
import org.nameless.server.policy.gesture.GestureListenerBase.GestureState;
import org.nameless.view.ISystemGestureListener;

public class SystemGesture {

    private static final String TAG = "SystemGesture";

    private final Context mContext;
    private final Display mDisplay;
    private final PhoneWindowManagerExt mPhoneWindowManagerExt;
    private final PowerManager mPowerManager;

    private final ArrayList<GestureListenerBase> mGestureListeners = new ArrayList<>();
    private final ArrayList<SystemGestureClient> mSystemGestureClients = new ArrayList<>();

    private final GameModeGestureListener mGameModeGestureListener;
    private final WindowModeGestureListener mWindowModeGestureListener;

    private GestureListenerBase mTargetGestureListener;

    private DisplayFeatureController mDisplayFeatureController;

    private boolean mTouching;

    public SystemGesture(Context context, PhoneWindowManagerExt ext) {
        mPhoneWindowManagerExt = ext;
        mContext = context;
        mDisplay = context.getSystemService(WindowManager.class).getDefaultDisplay();
        mPowerManager = context.getSystemService(PowerManager.class);

        mGameModeGestureListener = new GameModeGestureListener(
                this, ext, mContext);
        mGestureListeners.add(mGameModeGestureListener);

        mWindowModeGestureListener = new WindowModeGestureListener(
                this, ext, mContext);
        mGestureListeners.add(mWindowModeGestureListener);
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

    public int interceptMotionBeforeQueueing(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                setTouching(true);
                for (GestureListenerBase listener : mGestureListeners) {
                    if (listener.onActionDown(event)) {
                        mTargetGestureListener = listener;
                        return SYSTEM_GESTURE_DOWN;
                    }
                }
                return SYSTEM_GESTURE_NONE;
            case MotionEvent.ACTION_MOVE:
                if (mTargetGestureListener == null) {
                    return SYSTEM_GESTURE_NONE;
                }
                if (mTargetGestureListener.onActionMove(event)) {
                    if (mTargetGestureListener.mGestureState == GestureState.TRIGGERED) {
                        mPowerManager.userActivity(SystemClock.uptimeMillis(), false);
                        return SYSTEM_GESTURE_MOVE_TRIGGERED;
                    }
                    if (mTargetGestureListener.mGestureState == GestureState.PENDING_CHECK) {
                        return SYSTEM_GESTURE_MOVE;
                    }
                }
                return SYSTEM_GESTURE_NONE;
            case MotionEvent.ACTION_UP:
                setTouching(false);
                if (mTargetGestureListener == null) {
                    return SYSTEM_GESTURE_NONE;
                }
                mTargetGestureListener.onActionUp(event);
                mTargetGestureListener = null;
                return SYSTEM_GESTURE_RESET;
            case MotionEvent.ACTION_CANCEL:
                setTouching(false);
                if (mTargetGestureListener == null) {
                    return SYSTEM_GESTURE_NONE;
                }
                mTargetGestureListener.onActionCancel(event);
                mTargetGestureListener = null;
                return SYSTEM_GESTURE_RESET;
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
