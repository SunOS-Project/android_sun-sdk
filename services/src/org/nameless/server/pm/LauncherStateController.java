/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.server.pm;

import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DEFAULT;
import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
import static android.os.Process.THREAD_PRIORITY_DEFAULT;

import static org.nameless.os.DebugConstants.DEBUG_LAUNCHER;

import android.app.AppGlobals;
import android.content.Context;
import android.content.om.IOverlayManager;
import android.content.pm.IPackageManager;
import android.content.pm.UserInfo;
import android.os.Handler;
import android.os.IUserManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Slog;

import com.android.internal.util.nameless.LauncherUtils;

import com.android.server.ServiceThread;

import java.util.ArrayList;
import java.util.List;

import org.nameless.server.NamelessSystemExService;

public class LauncherStateController {

    private static final String TAG = "LauncherStateController";

    private final Handler mHandler;
    private final ServiceThread mServiceThread;

    private IOverlayManager mOm;
    private IPackageManager mPm;
    private IUserManager mUm;
    private NamelessSystemExService mSystemExService;
    private String mOpPackageName;
    private String mCachedLauncher;
    private ArrayList<String> mLauncherList;
    private boolean mSupportedLauncher;

    private static class InstanceHolder {
        private static LauncherStateController INSTANCE = new LauncherStateController();
    }

    private LauncherStateController() {
        mServiceThread = new ServiceThread(TAG, THREAD_PRIORITY_DEFAULT, false);
        mServiceThread.start();
        mHandler = new Handler(mServiceThread.getLooper());
    }

    public static LauncherStateController getInstance() {
        return InstanceHolder.INSTANCE;
    }

    public void initSystemExService(NamelessSystemExService systemExService) {
        mSystemExService = systemExService;
    }

    public void onSystemServicesReady() {
        mHandler.post(() -> {
            mPm = AppGlobals.getPackageManager();
            mOm = IOverlayManager.Stub.asInterface(ServiceManager.getService(Context.OVERLAY_SERVICE));
            mUm = IUserManager.Stub.asInterface(ServiceManager.getService(Context.USER_SERVICE));
            mOpPackageName = mSystemExService.getContext().getOpPackageName();
            mCachedLauncher = LauncherUtils.getCachedLauncher();
            mLauncherList = LauncherUtils.getLauncherList(mSystemExService.getContext());
            mSupportedLauncher = mLauncherList.contains(mCachedLauncher);
        });
    }

    public void onBootCompleted() {
        mHandler.post(() -> {
            List<UserInfo> users = null;
            try {
                users = mUm.getUsers(false, false, false);
            } catch (RemoteException e) {
            }
            if (users == null || users.size() == 0) {
                Slog.e(TAG, "Failed to fetch users list");
                return;
            }
            for (UserInfo user : users) {
                updatePackageState(user.id);
            }
        });
    }

    public void onUserStarting(int userId) {
        mHandler.post(() -> {
            updatePackageState(userId);
        });
    }

    private void updatePackageState(int userId) {
        if (!mSupportedLauncher) {
            Slog.e(TAG, "updatePackageState, exit due to launcher unsupported");
            return;
        }
        if (userId < 0) {
            return;
        }

        for (String launcher : mLauncherList) {
            final boolean enabled = launcher.equals(mCachedLauncher);
            if (DEBUG_LAUNCHER) {
                Slog.d(TAG, "updatePackageState, " + (enabled ? "enable " : "disable ")
                        + launcher + " for userId " + userId);
            }
            setPackageEnabled(launcher, enabled, userId);
            setOverlayEnabled(launcher, enabled, userId);
        }
    }

    private void setPackageEnabled(String pkg, boolean enabled, int userId) {
        try {
            mPm.setApplicationEnabledSetting(pkg, enabled
                    ? COMPONENT_ENABLED_STATE_DEFAULT
                    : COMPONENT_ENABLED_STATE_DISABLED,
                    0, userId, mOpPackageName);
        } catch (Exception e) {
            Slog.e(TAG, "Failed to set " + pkg + " enabled state to " + enabled);
        }
    }

    private void setOverlayEnabled(String pkg, boolean enabled, int userId) {
        if (!LauncherUtils.LAWNCHAIR_PACKAGE_NAME.equals(pkg)) {
            return;
        }
        for (String overlay : LauncherUtils.LAWNCHAIR_OVERLAYS) {
            try {
                mOm.setEnabled(overlay, enabled, userId);
            } catch (Exception e) {
                Slog.e(TAG, "Failed to set " + overlay + " enabled state to " + enabled);
            }
        }
    }
}
