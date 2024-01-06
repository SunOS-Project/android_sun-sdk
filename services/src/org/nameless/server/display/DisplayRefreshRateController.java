/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.server.display;

import static org.nameless.content.ContextExt.REFRESH_RATE_MANAGER_SERVICE;
import static org.nameless.os.DebugConstants.DEBUG_DISPLAY_RR;
import static org.nameless.provider.SettingsExt.System.EXTREME_REFRESH_RATE;
import static org.nameless.provider.SettingsExt.System.REFRESH_RATE_CONFIG_CUSTOM;

import android.content.Context;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Slog;

import com.android.internal.util.nameless.DisplayRefreshRateHelper;

import java.util.ArrayList;
import java.util.HashMap;

import org.nameless.display.IRefreshRateListener;
import org.nameless.display.IRefreshRateManagerService;
import org.nameless.server.NamelessSystemExService;

public final class DisplayRefreshRateController {

    private static final String TAG = "DisplayRefreshRateController";

    private final Handler mHandler = new Handler();
    private final Object mLock = new Object();

    private NamelessSystemExService mSystemExService;

    private static class InstanceHolder {
        private static DisplayRefreshRateController INSTANCE = new DisplayRefreshRateController();
    }

    public static DisplayRefreshRateController getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private final class RefreshRateListener {
        final IRefreshRateListener mListener;
        final IBinder.DeathRecipient mDeathRecipient;

        RefreshRateListener(IRefreshRateListener listener,
                IBinder.DeathRecipient deathRecipient) {
            mListener = listener;
            mDeathRecipient = deathRecipient;
        }
    }

    private final ArrayList<RefreshRateListener> mListeners = new ArrayList<>();
    private final HashMap<String, Integer> mAppRefreshRateConfigMap = new HashMap<>();

    private int mRequestedRefreshRate = -1;
    private int mRequestedMemcRefreshRate = -1;

    private boolean mExtremeMode = false;

    private DisplayRefreshRateHelper mHelper;

    private final class RefreshRateManagerService extends IRefreshRateManagerService.Stub {
        @Override
        public void requestMemcRefreshRate(int refreshRate) {
            synchronized (mLock) {
                if (mRequestedMemcRefreshRate != refreshRate) {
                    logD("requestMemcRefreshRate, refreshRate: " + refreshRate);
                    mRequestedMemcRefreshRate = refreshRate;
                    notifyMemcRefreshRateChanged();
                }
            }
        }

        @Override
        public void clearRequestedMemcRefreshRate() {
            synchronized (mLock) {
                if (mRequestedMemcRefreshRate > 0) {
                    logD("clearRequestedMemcRefreshRate");
                    mRequestedMemcRefreshRate = -1;
                    notifyMemcRefreshRateChanged();
                }
            }
        }

        @Override
        public int getRefreshRateForPackage(String packageName) {
            synchronized (mLock) {
                return mAppRefreshRateConfigMap.getOrDefault(packageName, -1);
            }
        }

        @Override
        public void setRefreshRateForPackage(String packageName, int refreshRate) {
            synchronized (mLock) {
                if (refreshRate > 0) {
                    logD("setRefreshRateForPackage, packageName: "
                            + packageName + ", refreshRate: " + refreshRate);
                    mAppRefreshRateConfigMap.put(packageName, refreshRate);
                    mHandler.post(() -> saveConfigIntoSettingsLocked(UserHandle.USER_CURRENT));
                } else if (mAppRefreshRateConfigMap.containsKey(packageName)) {
                    logD("unsetRefreshRateForPackage, packageName: " + packageName);
                    mAppRefreshRateConfigMap.remove(packageName);
                    mHandler.post(() -> saveConfigIntoSettingsLocked(UserHandle.USER_CURRENT));
                }
                updateRefreshRateLocked(mSystemExService.getTopFullscreenPackage());
            }
        }

        @Override
        public void unsetRefreshRateForPackage(String packageName) {
            synchronized (mLock) {
                if (mAppRefreshRateConfigMap.containsKey(packageName)) {
                    logD("unsetRefreshRateForPackage, packageName: " + packageName);
                    mAppRefreshRateConfigMap.remove(packageName);
                    mHandler.post(() -> saveConfigIntoSettingsLocked(UserHandle.USER_CURRENT));
                    updateRefreshRateLocked(mSystemExService.getTopFullscreenPackage());
                }
            }
        }

        @Override
        public void setExtremeRefreshRateEnabled(boolean enabled) {
            synchronized (mLock) {
                if (mExtremeMode != enabled) {
                    logD("setExtremeRefreshRateEnabled, enabled: " + enabled);
                    mExtremeMode = enabled;
                    Settings.System.putIntForUser(mSystemExService.getContentResolver(), EXTREME_REFRESH_RATE,
                            enabled ? 1 : 0, UserHandle.USER_CURRENT);
                    if (enabled) {
                        mRequestedRefreshRate = getMaxAllowedRefreshRate();
                        notifyRefreshRateChanged();
                    } else {
                        updateRefreshRateLocked(mSystemExService.getTopFullscreenPackage());
                    }
                }
            }
        }

        @Override
        public int getRequestedRefreshRate() {
            synchronized (mLock) {
                logD("getRequestedRefreshRate, refreshRate: " + mRequestedRefreshRate);
                return mRequestedRefreshRate;
            }
        }

        @Override
        public int getRequestedMemcRefreshRate() {
            synchronized (mLock) {
                logD("getRequestedMemcRefreshRate, refreshRate: " + mRequestedMemcRefreshRate);
                return mRequestedMemcRefreshRate;
            }
        }

        @Override
        public boolean registerRefreshRateListener(IRefreshRateListener listener) {
            final IBinder listenerBinder = listener.asBinder();
            IBinder.DeathRecipient dr = new IBinder.DeathRecipient() {
                @Override
                public void binderDied() {
                    synchronized (mLock) {
                        for (int i = 0; i < mListeners.size(); i++) {
                            if (listenerBinder == mListeners.get(i).mListener.asBinder()) {
                                RefreshRateListener removed = mListeners.remove(i);
                                IBinder binder = removed.mListener.asBinder();
                                if (binder != null) {
                                    binder.unlinkToDeath(this, 0);
                                }
                                i--;
                            }
                        }
                    }
                }
            };

            synchronized (mLock) {
                try {
                    listener.asBinder().linkToDeath(dr, 0);
                    mListeners.add(new RefreshRateListener(listener, dr));
                    notifyRefreshRateChanged(listener);
                    notifyMemcRefreshRateChanged(listener);
                } catch (RemoteException e) {
                    // Client died, no cleanup needed.
                    return false;
                }
                return true;
            }
        }

        @Override
        public boolean unregisterRefreshRateListener(IRefreshRateListener listener) {
            boolean found = false;
            final IBinder listenerBinder = listener.asBinder();
            synchronized (mLock) {
                for (int i = 0; i < mListeners.size(); i++) {
                    found = true;
                    RefreshRateListener refreshRateListener = mListeners.get(i);
                    if (listenerBinder == refreshRateListener.mListener.asBinder()) {
                        RefreshRateListener removed = mListeners.remove(i);
                        IBinder binder = removed.mListener.asBinder();
                        if (binder != null) {
                            binder.unlinkToDeath(removed.mDeathRecipient, 0);
                        }
                        i--;
                    }
                }
            }
            return found;
        }
    }

    public void initSystemExService(NamelessSystemExService service) {
        mSystemExService = service;
        mSystemExService.publishBinderService(REFRESH_RATE_MANAGER_SERVICE, new RefreshRateManagerService());
    }

    public void onBootCompleted() {
        synchronized (mLock) {
            logD("onBootCompleted");
            mHelper = DisplayRefreshRateHelper.getInstance(mSystemExService.getContext());
            parseSettingsIntoMapLocked(UserHandle.USER_CURRENT);
            mExtremeMode = Settings.System.getIntForUser(mSystemExService.getContentResolver(),
                    EXTREME_REFRESH_RATE, 0, UserHandle.USER_CURRENT) != 0;
            if (mExtremeMode) {
                mRequestedRefreshRate = getMaxAllowedRefreshRate();
                notifyRefreshRateChanged();
            }
        }
    }

    public void onUserSwitching(int newUserId) {
        synchronized (mLock) {
            logD("onUserSwitching, newUserId: " + newUserId);
            parseSettingsIntoMapLocked(newUserId);

            mExtremeMode = Settings.System.getIntForUser(mSystemExService.getContentResolver(),
                    EXTREME_REFRESH_RATE, 0, newUserId) != 0;
            if (mExtremeMode) {
                mRequestedRefreshRate = getMaxAllowedRefreshRate();
                notifyRefreshRateChanged();
            } else {
                updateRefreshRateLocked(mSystemExService.getTopFullscreenPackage());
            }
        }
    }

    public void onPackageRemoved(String packageName) {
        synchronized (mLock) {
            logD("onPackageRemoved, packageName: " + packageName);
            if (mAppRefreshRateConfigMap.containsKey(packageName)) {
                logD("unsetRefreshRateForPackage, packageName: " + packageName);
                mAppRefreshRateConfigMap.remove(packageName);
                mHandler.post(() -> saveConfigIntoSettingsLocked(UserHandle.USER_CURRENT));
                updateRefreshRateLocked(mSystemExService.getTopFullscreenPackage());
            }
        }
    }

    public void onScreenOff() {
        synchronized (mLock) {
            if (mRequestedRefreshRate != -1) {
                logD("onScreenOff, restore refresh rate");
                mRequestedRefreshRate = -1;
                notifyRefreshRateChanged();
            }
        }
    }

    public void onScreenOn() {
        synchronized (mLock) {
            if (mExtremeMode) {
                logD("onScreenOn, set refresh rate to highest");
                mRequestedRefreshRate = getMaxAllowedRefreshRate();
                notifyRefreshRateChanged();
            }
        }
    }

    public void updateRefreshRate(String packageName) {
        synchronized (mLock) {
            updateRefreshRateLocked(packageName);
        }
    }

    public void updateRefreshRateLocked(String packageName) {
        if (mExtremeMode) {
            logD("Skip update refresh rate due to in extreme mode");
        } else if (mAppRefreshRateConfigMap.containsKey(packageName)) {
            mRequestedRefreshRate = mAppRefreshRateConfigMap.get(packageName);
            notifyRefreshRateChanged();
            logD("Requested refresh rate " + mRequestedRefreshRate + " for package: " + packageName);
        } else if (mRequestedRefreshRate > 0) {
            mRequestedRefreshRate = -1;
            notifyRefreshRateChanged();
            logD("Restore refresh rate");
        }
    }

    private void notifyRefreshRateChanged() {
        logD("notifyRefreshRateChanged, refreshRate: " + mRequestedRefreshRate);
        for (RefreshRateListener listener : mListeners) {
            notifyRefreshRateChanged(listener.mListener);
        }
    }

    private void notifyRefreshRateChanged(IRefreshRateListener listener) {
        mHandler.post(() -> {
            try {
                listener.onRequestedRefreshRate(mRequestedRefreshRate);
            } catch (RemoteException | RuntimeException e) {
                logE("Failed to notify refresh rate changed");
            }
        });
    }

    private void notifyMemcRefreshRateChanged() {
        logD("notifyMemcRefreshRateChanged, refreshRate: " + mRequestedMemcRefreshRate);
        for (RefreshRateListener listener : mListeners) {
            notifyMemcRefreshRateChanged(listener.mListener);
        }
    }

    private void notifyMemcRefreshRateChanged(IRefreshRateListener listener) {
        mHandler.post(() -> {
            try {
                listener.onRequestedMemcRefreshRate(mRequestedMemcRefreshRate);
            } catch (RemoteException | RuntimeException e) {
                logE("Failed to notify memc refresh rate changed");
            }
        });
    }

    private int getMaxAllowedRefreshRate() {
        final ArrayList<Integer> supportedList = mHelper.getSupportedRefreshRateList();
        if (supportedList.size() > 0) {
            return supportedList.get(supportedList.size() - 1);
        }
        return -1;
    }

    private void parseSettingsIntoMapLocked(int userId) {
        mAppRefreshRateConfigMap.clear();
        final String settings = Settings.System.getStringForUser(mSystemExService.getContentResolver(),
                REFRESH_RATE_CONFIG_CUSTOM, userId);
        if (TextUtils.isEmpty(settings)) {
            return;
        }
        final String[] configs = settings.split(";");
        for (String config : configs) {
            final String[] splited = config.split(",");
            if (splited.length != 2) {
                continue;
            }
            int refreshRate = -1;
            try {
                refreshRate = Integer.parseInt(splited[1]);
            } catch (NumberFormatException e) {}
            if (mHelper.isRefreshRateValid(refreshRate)) {
                mAppRefreshRateConfigMap.put(splited[0], refreshRate);
                logD("parseSettingsIntoMapLocked, added packageName: "
                        + splited[0] + ", refreshRate: " + refreshRate);
            }
        }
    }

    private void saveConfigIntoSettingsLocked(int userId) {
        StringBuilder sb = new StringBuilder();
        for (String app : mAppRefreshRateConfigMap.keySet()) {
            sb.append(app).append(",");
            sb.append(String.valueOf(mAppRefreshRateConfigMap.get(app))).append(";");
        }
        Settings.System.putStringForUser(mSystemExService.getContentResolver(),
                REFRESH_RATE_CONFIG_CUSTOM, sb.toString(), userId);
        logD("saveConfigIntoSettingsLocked, config: " + sb.toString());
    }

    private static void logD(String msg) {
        if (DEBUG_DISPLAY_RR) {
            Slog.d(TAG, msg);
        }
    }

    private static void logE(String msg) {
        Slog.e(TAG, msg);
    }
}
