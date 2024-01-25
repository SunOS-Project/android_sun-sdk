/**
 * Copyright (C) 2016 The ParanoidAndroid Project
 * Copyright (C) 2024 The Nameless-AOSP Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.nameless.os;

import android.content.Context;
import android.os.Handler;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.telecom.TelecomManager;
import android.text.format.DateUtils;
import android.util.Slog;

import com.android.internal.R;

/**
 * A class that coordinates listening for pocket state.
 *
 * @author Carlo Savignano
 * @hide
 */
public class PocketManager {

    private static final String TAG = "PocketManager";

    /**
     * Whether {@link IPocketCallback#onStateChanged(boolean, int)}
     * was fired because of the sensor.
     * @see PocketService#handleDispatchCallbacks()
     */
    public static final int REASON_SENSOR = 0;

    /**
     * Whether {@link IPocketCallback#onStateChanged(boolean, int)}
     * was fired because of an error while accessing service.
     * @see #addCallback(IPocketCallback)
     * @see #removeCallback(IPocketCallback)
     */
    public static final int REASON_ERROR = 1;

    /**
     * Whether {@link IPocketCallback#onStateChanged(boolean, int)}
     * was fired because of a needed reset.
     * @see PocketService#binderDied()
     */
    public static final int REASON_RESET = 2;

    private final Context mContext;
    private final IPocketService mService;

    private PowerManager mPowerManager;
    private TelecomManager mTelecomManager;

    private final Handler mHandler = new Handler();
    private final Runnable mPocketLockTimeout = () -> {
        if (!mTelecomManager.isInCall()) {
            mPowerManager.goToSleep(SystemClock.uptimeMillis());
        }
        mPocketViewTimerActive = false;
    };

    private boolean mPocketViewTimerActive;

    public PocketManager(Context context, IPocketService service) {
        mContext = context;
        mService = service;
        if (mService == null) {
            Slog.e(TAG, "PocketService was null");
        }
        mPowerManager = context.getSystemService(PowerManager.class);
        mTelecomManager = context.getSystemService(TelecomManager.class);
    }

    /**
     * Add pocket state callback.
     * @see PocketService#handleRemoveCallback(IPocketCallback)
     */
    public void addCallback(final IPocketCallback callback) {
        if (mService == null) {
            return;
        }
        try {
            mService.addCallback(callback);
        } catch (RemoteException e1) {
            Slog.w(TAG, "Remote exception in addCallback: ", e1);
            try {
                callback.onStateChanged(false, REASON_ERROR);
            } catch (RemoteException e2) {
                Slog.w(TAG, "Remote exception in callback.onPocketStateChanged: ", e2);
            }
        }
    }

    /**
     * Remove pocket state callback.
     * @see PocketService#handleAddCallback(IPocketCallback)
     */
    public void removeCallback(final IPocketCallback callback) {
        if (mService == null) {
            return;
        }
        try {
            mService.removeCallback(callback);
        } catch (RemoteException e1) {
            Slog.w(TAG, "Remote exception in removeCallback: ", e1);
            try {
                callback.onStateChanged(false, REASON_ERROR);
            } catch (RemoteException e2) {
                Slog.w(TAG, "Remote exception in callback.onPocketStateChanged: ", e2);
            }
        }
    }

    /**
     * Notify service about device interactive state changed.
     * {@link PhoneWindowManager#startedWakingUp()}
     * {@link PhoneWindowManager#startedGoingToSleep(int)}
     */
    public void onInteractiveChanged(boolean interactive) {
        final boolean isPocketViewShowing = (interactive && isDeviceInPocket());
        synchronized (mPocketLockTimeout) {
            if (mPocketViewTimerActive != isPocketViewShowing) {
                if (isPocketViewShowing) {
                    mHandler.removeCallbacks(mPocketLockTimeout); // remove any pending requests
                    mHandler.postDelayed(mPocketLockTimeout, 15 * DateUtils.SECOND_IN_MILLIS);
                    mPocketViewTimerActive = true;
                } else {
                    mHandler.removeCallbacks(mPocketLockTimeout);
                    mPocketViewTimerActive = false;
                }
            }
        }
        if (mService == null) {
            return;
        }
        try {
            mService.onInteractiveChanged(interactive);
        } catch (RemoteException e) {
            Slog.w(TAG, "Remote exception in addCallback: ", e);
        }
    }

    /**
     * Request listening state change by, but not limited to, external process.
     * @see PocketService#handleSetListeningExternal(boolean)
     */
    public void setListeningExternal(boolean listen) {
        if (mService == null) {
            return;
        }
        try {
            mService.setListeningExternal(listen);
        } catch (RemoteException e) {
            Slog.w(TAG, "Remote exception in setListeningExternal: ", e);
        }
        // Clear timeout when user hides pocket lock with long press power.
        if (mPocketViewTimerActive && !listen) {
            mHandler.removeCallbacks(mPocketLockTimeout);
            mPocketViewTimerActive = false;
        }
    }

    /**
     * Return whether device is in pocket.
     * @see PocketService#isDeviceInPocket()
     * @return
     */
    public boolean isDeviceInPocket() {
        if (mService == null) {
            return false;
        }
        try {
            return mService.isDeviceInPocket();
        } catch (RemoteException e) {
            Slog.w(TAG, "Remote exception in isDeviceInPocket: ", e);
        }
        return false;
    }

    /** Custom methods **/
    public void setPocketLockVisible(boolean visible) {
        if (!visible) {
            mHandler.removeCallbacks(mPocketLockTimeout);
            mPocketViewTimerActive = false;
        }
        if (mService == null) {
            return;
        }
        try {
            mService.setPocketLockVisible(visible);
        } catch (RemoteException e) {
            Slog.w(TAG, "Remote exception in setPocketLockVisible: ", e);
        }
    }

    public boolean isPocketLockVisible() {
        if (mService == null) {
            return false;
        }
        try {
            return mService.isPocketLockVisible();
        } catch (RemoteException e) {
            Slog.w(TAG, "Remote exception in isPocketLockVisible: ", e);
        }
        return false;
    }

    public static boolean isSupported(Context context) {
        return context.getResources().getBoolean(R.bool.config_pocketModeSupported);
    }
}
