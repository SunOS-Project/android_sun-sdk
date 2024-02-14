/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.server.sensors;

import static org.nameless.content.ContextExt.SENSOR_BLOCK_MANAGER_SERVICE;
import static org.nameless.hardware.SensorBlockManager.APP_FIRST_SCREEN_MS;
import static org.nameless.hardware.SensorBlockManager.SHAKE_SENSORS_ALLOW;
import static org.nameless.hardware.SensorBlockManager.SHAKE_SENSORS_BLOCK_ALWAYS;
import static org.nameless.hardware.SensorBlockManager.SHAKE_SENSORS_BLOCK_FIRST_SCREEN;
import static org.nameless.hardware.SensorBlockManager.isShakeSensorsConfigValid;
import static org.nameless.hardware.SensorBlockManager.shakeSensorsConfigToString;
import static org.nameless.os.DebugConstants.DEBUG_SENSOR;
import static org.nameless.provider.SettingsExt.System.SHAKE_SENSORS_BLACKLIST_CONFIG;

import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Slog;

import java.util.HashMap;
import java.util.HashSet;

import org.nameless.hardware.ISensorBlockService;
import org.nameless.server.NamelessSystemExService;

public final class SensorBlockController {

    private static final String TAG = "SensorBlockController";

    private static class InstanceHolder {
        private static SensorBlockController INSTANCE = new SensorBlockController();
    }

    public static SensorBlockController getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private static final int MSG_SAVE_SETTINGS = 0;
    private static final int MSG_UNBLOCK_SHAKE_SENSOR = 1;

    private final HashMap<String, Integer> mShakeSensorsConfig = new HashMap<>();
    private final HashSet<String> mShakeSensorsBlockingPackages = new HashSet<>();

    private final Handler mHandler = new H();
    private final Object mLock = new Object();

    private NamelessSystemExService mSystemExService;

    private String mTopPackageName = "";

    private final class H extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SAVE_SETTINGS:
                    saveConfigIntoSettingsLocked();
                    break;
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
                    if (mTopPackageName.equals(packageName)) {
                        mShakeSensorsBlockingPackages.add(packageName);
                        mHandler.sendMessageDelayed(mHandler.obtainMessage(
                                MSG_UNBLOCK_SHAKE_SENSOR, packageName), APP_FIRST_SCREEN_MS);
                    }
                } else if (config == SHAKE_SENSORS_BLOCK_ALWAYS) {
                    mShakeSensorsBlockingPackages.add(packageName);
                }
                mHandler.sendEmptyMessage(MSG_SAVE_SETTINGS);
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

    public void initSystemExService(NamelessSystemExService service) {
        mSystemExService = service;
        mSystemExService.publishBinderService(SENSOR_BLOCK_MANAGER_SERVICE, new SensorBlockManagerService());
    }

    public void onBootCompleted() {
        synchronized (mLock) {
            if (DEBUG_SENSOR) {
                Slog.d(TAG, "onBootCompleted");
            }
            parseSettingsIntoMapLocked(UserHandle.USER_CURRENT);
        }
    }

    public void onUserSwitching(int newUserId) {
        synchronized (mLock) {
            if (DEBUG_SENSOR) {
                Slog.d(TAG, "onUserSwitching, newUserId: " + newUserId);
            }
            parseSettingsIntoMapLocked(newUserId);
        }
    }

    public void onPackageRemoved(String packageName) {
        synchronized (mLock) {
            if (DEBUG_SENSOR) {
                Slog.d(TAG, "onPackageRemoved, packageName: " + packageName);
            }
            if (mShakeSensorsConfig.containsKey(packageName)) {
                mShakeSensorsConfig.remove(packageName);
                mShakeSensorsBlockingPackages.remove(packageName);
                mHandler.sendEmptyMessage(MSG_SAVE_SETTINGS);
            }
        }
    }

    public void updateTopPackage(String packageName) {
        synchronized (mLock) {
            if (DEBUG_SENSOR) {
                Slog.d(TAG, "updateTopPackage, from=" + mTopPackageName + ", to=" + packageName);
            }
            if (mHandler.hasMessages(MSG_UNBLOCK_SHAKE_SENSOR)) {
                mHandler.removeMessages(MSG_UNBLOCK_SHAKE_SENSOR);
            }
            mTopPackageName = packageName;
            final int config = mShakeSensorsConfig.getOrDefault(mTopPackageName, SHAKE_SENSORS_ALLOW);
            if (DEBUG_SENSOR) {
                Slog.d(TAG, "updateTopPackage, newConfig=" + shakeSensorsConfigToString(config));
            }
            if (config != SHAKE_SENSORS_ALLOW) {
                mShakeSensorsBlockingPackages.add(mTopPackageName);
                if (config == SHAKE_SENSORS_BLOCK_FIRST_SCREEN) {
                    mHandler.sendMessageDelayed(mHandler.obtainMessage(
                            MSG_UNBLOCK_SHAKE_SENSOR, mTopPackageName), APP_FIRST_SCREEN_MS);
                }
            }
        }
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
