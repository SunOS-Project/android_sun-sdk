/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.server.sensors;

import static android.os.Process.THREAD_PRIORITY_DEFAULT;

import static org.sun.content.ContextExt.SENSOR_BLOCK_MANAGER_SERVICE;
import static org.sun.hardware.SensorBlockManager.APP_FIRST_SCREEN_MS;
import static org.sun.hardware.SensorBlockManager.SHAKE_SENSORS_ALLOW;
import static org.sun.hardware.SensorBlockManager.SHAKE_SENSORS_BLOCK_ALWAYS;
import static org.sun.hardware.SensorBlockManager.SHAKE_SENSORS_BLOCK_FIRST_SCREEN;
import static org.sun.hardware.SensorBlockManager.isShakeSensorsConfigValid;
import static org.sun.hardware.SensorBlockManager.shakeSensorsConfigToString;
import static org.sun.os.DebugConstants.DEBUG_SENSOR;
import static org.sun.provider.SettingsExt.System.SHAKE_SENSORS_BLACKLIST_CONFIG;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;

import com.android.server.ServiceThread;

import org.sun.hardware.ISensorBlockService;
import org.sun.server.SunSystemExService;

public final class SensorBlockController {

    private static final String TAG = "SensorBlockController";

    private static class InstanceHolder {
        private static SensorBlockController INSTANCE = new SensorBlockController();
    }

    public static SensorBlockController getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private static final int MSG_UNBLOCK_SHAKE_SENSOR = 1;

    private final ArrayMap<String, Integer> mShakeSensorsConfig = new ArrayMap<>();
    private final ArraySet<String> mShakeSensorsBlockingPackages = new ArraySet<>();

    private final Handler mHandler;
    private final ServiceThread mServiceThread;

    private final Object mLock = new Object();

    private SunSystemExService mSystemExService;

    private final class H extends Handler {
        H(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UNBLOCK_SHAKE_SENSOR:
                    String packageName = (String) msg.obj;
                    synchronized (mLock) {
                        if (DEBUG_SENSOR) {
                            Slog.d(TAG, "package=" + packageName + ", app first screen ended, unblock shake sensors");
                        }
                        mShakeSensorsBlockingPackages.remove(packageName);
                    }
                    break;
            }
        }
    }

    private final class SensorBlockManagerService extends ISensorBlockService.Stub {
        @Override
        public int getShakeSensorsConfigForPackage(String packageName) {
            synchronized (mLock) {
                return mShakeSensorsConfig.getOrDefault(packageName, SHAKE_SENSORS_ALLOW);
            }
        }

        @Override
        public void setShakeSensorsConfigForPackage(String packageName, int config) {
            synchronized (mLock) {
                if (DEBUG_SENSOR) {
                    Slog.d(TAG, "setShakeSensorsConfig, package=" + packageName
                            + ", config=" + shakeSensorsConfigToString(config));
                }
                mShakeSensorsConfig.put(packageName, config);
                if (config == SHAKE_SENSORS_ALLOW) {
                    mShakeSensorsBlockingPackages.remove(packageName);
                } else if (config == SHAKE_SENSORS_BLOCK_FIRST_SCREEN) {
                    if (mSystemExService.getTopFullscreenPackage().equals(packageName)) {
                        mShakeSensorsBlockingPackages.add(packageName);
                        mHandler.sendMessageDelayed(mHandler.obtainMessage(
                                MSG_UNBLOCK_SHAKE_SENSOR, packageName), APP_FIRST_SCREEN_MS);
                    }
                } else if (config == SHAKE_SENSORS_BLOCK_ALWAYS) {
                    mShakeSensorsBlockingPackages.add(packageName);
                }
                mHandler.post(() -> saveConfigIntoSettingsLocked());
            }
        }

        @Override
        public boolean shouldBlockShakeSensorsNow(String packageName) {
            synchronized (mLock) {
                final boolean block = mShakeSensorsBlockingPackages.contains(packageName);
                if (DEBUG_SENSOR) {
                    Slog.d(TAG, "shouldBlockShakeSensorsNow, package=" + packageName + ", block=" + block);
                }
                return block;
            }
        }
    }

    private SensorBlockController() {
        mServiceThread = new ServiceThread(TAG, THREAD_PRIORITY_DEFAULT, false);
        mServiceThread.start();
        mHandler = new H(mServiceThread.getLooper());
    }

    public void initSystemExService(SunSystemExService service) {
        mSystemExService = service;
        mSystemExService.publishBinderService(SENSOR_BLOCK_MANAGER_SERVICE, new SensorBlockManagerService());
    }

    public void onSystemServicesReady() {
        mHandler.post(() -> {
            synchronized (mLock) {
                if (DEBUG_SENSOR) {
                    Slog.d(TAG, "onSystemServicesReady");
                }
                parseSettingsIntoMapLocked(UserHandle.USER_CURRENT);
            }
        });
    }

    public void onUserSwitching(int newUserId) {
        mHandler.post(() -> {
            synchronized (mLock) {
                if (DEBUG_SENSOR) {
                    Slog.d(TAG, "onUserSwitching, newUserId: " + newUserId);
                }
                parseSettingsIntoMapLocked(newUserId);
            }
        });
    }

    public void onPackageRemoved(String packageName) {
        mHandler.post(() -> {
            synchronized (mLock) {
                if (DEBUG_SENSOR) {
                    Slog.d(TAG, "onPackageRemoved, packageName: " + packageName);
                }
                if (mShakeSensorsConfig.containsKey(packageName)) {
                    mShakeSensorsConfig.remove(packageName);
                    mShakeSensorsBlockingPackages.remove(packageName);
                    saveConfigIntoSettingsLocked();
                }
            }
        });
    }

    public void onTopFullscreenPackageChanged(String packageName) {
        mHandler.post(() -> {
            synchronized (mLock) {
                if (DEBUG_SENSOR) {
                    Slog.d(TAG, "onTopFullscreenPackageChanged: " + packageName);
                }
                if (mHandler.hasMessages(MSG_UNBLOCK_SHAKE_SENSOR)) {
                    mHandler.removeMessages(MSG_UNBLOCK_SHAKE_SENSOR);
                }
                final int config = mShakeSensorsConfig.getOrDefault(packageName, SHAKE_SENSORS_ALLOW);
                if (DEBUG_SENSOR) {
                    Slog.d(TAG, "onTopFullscreenPackageChanged, newConfig=" +
                            shakeSensorsConfigToString(config));
                }
                if (config != SHAKE_SENSORS_ALLOW) {
                    mShakeSensorsBlockingPackages.add(packageName);
                    if (config == SHAKE_SENSORS_BLOCK_FIRST_SCREEN) {
                        mHandler.sendMessageDelayed(mHandler.obtainMessage(
                                MSG_UNBLOCK_SHAKE_SENSOR, packageName), APP_FIRST_SCREEN_MS);
                    }
                }
            }
        });
    }

    private void parseSettingsIntoMapLocked(int userId) {
        mShakeSensorsConfig.clear();
        final String settings = Settings.System.getStringForUser(mSystemExService.getContentResolver(),
                SHAKE_SENSORS_BLACKLIST_CONFIG, userId);
        if (TextUtils.isEmpty(settings)) {
            return;
        }
        final String[] configs = settings.split(";");
        for (String config : configs) {
            final String[] splited = config.split(",");
            if (splited.length != 2) {
                continue;
            }
            int blockConfig = SHAKE_SENSORS_ALLOW;
            try {
                blockConfig = Integer.parseInt(splited[1]);
            } catch (NumberFormatException e) {}
            if (isShakeSensorsConfigValid(blockConfig)) {
                mShakeSensorsConfig.put(splited[0], blockConfig);
                if (DEBUG_SENSOR) {
                    Slog.d(TAG, "parseSettingsIntoMapLocked, added packageName: "
                            + splited[0] + ", config: " + shakeSensorsConfigToString(blockConfig));
                }
            }
        }
    }

    private void saveConfigIntoSettingsLocked() {
        StringBuilder sb = new StringBuilder();
        for (String app : mShakeSensorsConfig.keySet()) {
            sb.append(app).append(",");
            sb.append(String.valueOf(mShakeSensorsConfig.get(app))).append(";");
        }
        Settings.System.putStringForUser(mSystemExService.getContentResolver(),
                SHAKE_SENSORS_BLACKLIST_CONFIG, sb.toString(), UserHandle.USER_CURRENT);
        if (DEBUG_SENSOR) {
            Slog.d(TAG, "saveConfigIntoSettingsLocked, config: " + sb.toString());
        }
    }
}
