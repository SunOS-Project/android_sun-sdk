/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.server.wm;

import static org.nameless.content.ContextExt.ROTATE_MANAGER_SERVICE;
import static org.nameless.os.DebugConstants.DEBUG_DISPLAY_ROTATE;
import static org.nameless.os.RotateManager.ROTATE_FOLLOW_SYSTEM;
import static org.nameless.os.RotateManager.ROTATE_FORCE_OFF;
import static org.nameless.os.RotateManager.ROTATE_FORCE_ON;
import static org.nameless.os.RotateManager.configToString;
import static org.nameless.os.RotateManager.isConfigValid;
import static org.nameless.provider.SettingsExt.System.AUTO_ROTATE_CONFIG_CUSTOM;

import android.content.Context;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Slog;
import android.view.Surface;

import com.android.internal.view.RotationPolicy;

import java.util.ArrayList;
import java.util.HashMap;

import org.nameless.display.DisplayFeatureManager;
import org.nameless.os.IRotateConfigListener;
import org.nameless.os.IRotateManagerService;
import org.nameless.server.NamelessSystemExService;

public class DisplayRotationExt {

    private static final String TAG = "DisplayRotationExt";

    private final Handler mHandler = new Handler();
    private final Object mLock = new Object();

    private final DisplayFeatureManager mDisplayFeatureManager =
            DisplayFeatureManager.getInstance();

    private NamelessSystemExService mSystemExService;

    private static class InstanceHolder {
        private static DisplayRotationExt INSTANCE = new DisplayRotationExt();
    }

    public static DisplayRotationExt getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private final class RotateConfigListener {
        final IRotateConfigListener mListener;
        final IBinder.DeathRecipient mDeathRecipient;

        RotateConfigListener(IRotateConfigListener listener,
                IBinder.DeathRecipient deathRecipient) {
            mListener = listener;
            mDeathRecipient = deathRecipient;
        }
    }

    private final ArrayList<RotateConfigListener> mListeners = new ArrayList<>();
    private final HashMap<String, Integer> mAppRotateConfigMap = new HashMap<>();

    private boolean mRotateLockedSystem = false;
    private int mRotateConfig = ROTATE_FOLLOW_SYSTEM;

    private final class RotateManagerService extends IRotateManagerService.Stub {
        @Override
        public int getRotateConfigForPackage(String packageName) {
            synchronized (mLock) {
                return mAppRotateConfigMap.getOrDefault(packageName, ROTATE_FOLLOW_SYSTEM);
            }
        }

        @Override
        public void setRotateConfigForPackage(String packageName, int config) {
            synchronized (mLock) {
                if (config != ROTATE_FOLLOW_SYSTEM) {
                    logD("setRotateConfigForPackage, packageName: "
                            + packageName + ", config: " + configToString(config));
                    mAppRotateConfigMap.put(packageName, config);
                    mHandler.post(() -> saveConfigIntoSettingsLocked(UserHandle.USER_CURRENT));
                } else if (mAppRotateConfigMap.containsKey(packageName)) {
                    logD("unsetRotateConfigForPackage, packageName: " + packageName);
                    mAppRotateConfigMap.remove(packageName);
                    mHandler.post(() -> saveConfigIntoSettingsLocked(UserHandle.USER_CURRENT));
                }
                updateAutoRotateLocked(mSystemExService.getTopFullscreenPackage());
            }
        }

        @Override
        public int getCurrentRotateConfig() {
            synchronized (mLock) {
                logD("getCurrentRotateConfig, ret: " + configToString(mRotateConfig));
                return mRotateConfig;
            }
        }

        @Override
        public boolean registerRotateConfigListener(IRotateConfigListener listener) {
            final IBinder listenerBinder = listener.asBinder();
            IBinder.DeathRecipient dr = new IBinder.DeathRecipient() {
                @Override
                public void binderDied() {
                    synchronized (mLock) {
                        for (int i = 0; i < mListeners.size(); i++) {
                            if (listenerBinder == mListeners.get(i).mListener.asBinder()) {
                                RotateConfigListener removed = mListeners.remove(i);
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
                    mListeners.add(new RotateConfigListener(listener, dr));
                    notifyRotateConfigChanged(listener);
                } catch (RemoteException e) {
                    // Client died, no cleanup needed.
                    return false;
                }
                return true;
            }
        }

        @Override
        public boolean unregisterRotateConfigListener(IRotateConfigListener listener) {
            boolean found = false;
            final IBinder listenerBinder = listener.asBinder();
            synchronized (mLock) {
                for (int i = 0; i < mListeners.size(); i++) {
                    found = true;
                    RotateConfigListener rotateConfigListener = mListeners.get(i);
                    if (listenerBinder == rotateConfigListener.mListener.asBinder()) {
                        RotateConfigListener removed = mListeners.remove(i);
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
        mSystemExService.publishBinderService(ROTATE_MANAGER_SERVICE, new RotateManagerService());
    }

    public void onBootCompleted() {
        synchronized (mLock) {
            logD("onBootCompleted");
            parseSettingsIntoMapLocked(UserHandle.USER_CURRENT);
            mRotateLockedSystem = RotationPolicy.isRotationLocked(mSystemExService.getContext());
        }
    }

    public void onUserSwitching(int newUserId) {
        synchronized (mLock) {
            logD("onUserSwitching, newUserId: " + newUserId);
            parseSettingsIntoMapLocked(newUserId);
            updateAutoRotateLocked(mSystemExService.getTopFullscreenPackage());
        }
    }

    public void onPackageRemoved(String packageName) {
        synchronized (mLock) {
            logD("onPackageRemoved, packageName: " + packageName);
            if (mAppRotateConfigMap.containsKey(packageName)) {
                logD("unsetRotateConfigForPackage, packageName: " + packageName);
                mAppRotateConfigMap.remove(packageName);
                mHandler.post(() -> saveConfigIntoSettingsLocked(UserHandle.USER_CURRENT));
                updateAutoRotateLocked(mSystemExService.getTopFullscreenPackage());
            }
        }
    }

    public void onScreenOff() {
        synchronized (mLock) {
            if (mRotateConfig != ROTATE_FOLLOW_SYSTEM) {
                logD("onScreenOff, restore auto rotate");
                mRotateConfig = ROTATE_FOLLOW_SYSTEM;
                setAutoRotateLocked(false);
            }
        }
    }

    public void onShutdown() {
        synchronized (mLock) {
            if (mRotateConfig != ROTATE_FOLLOW_SYSTEM) {
                logD("onShutdown, restore auto rotate");
                mRotateConfig = ROTATE_FOLLOW_SYSTEM;
                setAutoRotateLocked(false);
            }
        }
    }

    public void updateRotation(int rotation) {
        switch (rotation) {
            case Surface.ROTATION_0:
            case Surface.ROTATION_180:
                mDisplayFeatureManager.setDisplayOrientation(0);
                break;
            case Surface.ROTATION_90:
                mDisplayFeatureManager.setDisplayOrientation(90);
                break;
            case Surface.ROTATION_270:
                mDisplayFeatureManager.setDisplayOrientation(270);
                break;
        }
    }

    public void updateAutoRotate(String packageName) {
        synchronized (mLock) {
            updateAutoRotateLocked(packageName);
        }
    }

    public void updateAutoRotateLocked(String packageName) {
        if (mAppRotateConfigMap.containsKey(packageName)) {
            final boolean updateSystem = mRotateConfig == ROTATE_FOLLOW_SYSTEM;
            mRotateConfig = mAppRotateConfigMap.get(packageName);
            setAutoRotateLocked(updateSystem);
            logD("updateAutoRotateLocked, config: " + configToString(mRotateConfig) + ", package: " + packageName);
        } else if (mRotateConfig != ROTATE_FOLLOW_SYSTEM) {
            mRotateConfig = ROTATE_FOLLOW_SYSTEM;
            setAutoRotateLocked(false);
            logD("updateAutoRotateLocked, restore auto rotate");
        }
    }

    private void setAutoRotateLocked(boolean updateSystem) {
        if (updateSystem) {
            mRotateLockedSystem = RotationPolicy.isRotationLocked(mSystemExService.getContext());
        }
        if (mRotateConfig == ROTATE_FOLLOW_SYSTEM) {
            RotationPolicy.setRotationLock(mSystemExService.getContext(), mRotateLockedSystem);
        } else {
            RotationPolicy.setRotationLock(mSystemExService.getContext(), mRotateConfig == ROTATE_FORCE_OFF);
        }
        notifyRotateConfigChanged();
    }

    private void notifyRotateConfigChanged() {
        logD("notifyRotateConfigChanged, config: " + configToString(mRotateConfig));
        for (RotateConfigListener listener : mListeners) {
            notifyRotateConfigChanged(listener.mListener);
        }
    }

    private void notifyRotateConfigChanged(IRotateConfigListener listener) {
        mHandler.post(() -> {
            try {
                listener.onRotateConfigChanged(mRotateConfig);
            } catch (RemoteException | RuntimeException e) {
                logE("Failed to notify rotate config changed");
            }
        });
    }

    private void parseSettingsIntoMapLocked(int userId) {
        mAppRotateConfigMap.clear();
        final String settings = Settings.System.getStringForUser(mSystemExService.getContentResolver(),
                AUTO_ROTATE_CONFIG_CUSTOM, userId);
        if (TextUtils.isEmpty(settings)) {
            return;
        }
        final String[] configs = settings.split(";");
        for (String config : configs) {
            final String[] splited = config.split(",");
            if (splited.length != 2) {
                continue;
            }
            int mode = -1;
            try {
                mode = Integer.parseInt(splited[1]);
            } catch (NumberFormatException e) {}
            if (isConfigValid(mode)) {
                mAppRotateConfigMap.put(splited[0], mode);
                logD("parseSettingsIntoMapLocked, added packageName: "
                        + splited[0] + ", config: " + configToString(mode));
            }
        }
    }

    private void saveConfigIntoSettingsLocked(int userId) {
        StringBuilder sb = new StringBuilder();
        for (String app : mAppRotateConfigMap.keySet()) {
            sb.append(app).append(",");
            sb.append(String.valueOf(mAppRotateConfigMap.get(app))).append(";");
        }
        Settings.System.putStringForUser(mSystemExService.getContentResolver(),
                AUTO_ROTATE_CONFIG_CUSTOM, sb.toString(), userId);
        logD("saveConfigIntoSettingsLocked, config: " + sb.toString());
    }

    private static void logD(String msg) {
        if (DEBUG_DISPLAY_ROTATE) {
            Slog.d(TAG, msg);
        }
    }

    private static void logE(String msg) {
        Slog.e(TAG, msg);
    }
}
