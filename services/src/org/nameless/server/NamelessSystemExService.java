/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.server;

import static android.content.Intent.ACTION_PACKAGE_ADDED;
import static android.content.Intent.ACTION_PACKAGE_CHANGED;
import static android.content.Intent.ACTION_PACKAGE_FULLY_REMOVED;
import static android.content.Intent.ACTION_PACKAGE_REMOVED;
import static android.content.Intent.ACTION_PACKAGE_REPLACED;
import static android.content.Intent.ACTION_SCREEN_OFF;
import static android.content.Intent.ACTION_SCREEN_ON;
import static android.content.Intent.ACTION_USER_PRESENT;
import static android.content.Intent.EXTRA_REPLACING;
import static android.os.Process.THREAD_PRIORITY_DEFAULT;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.PowerManager;

import com.android.server.ServiceThread;
import com.android.server.SystemService;
import com.android.server.wm.TopActivityRecorder;

import org.nameless.display.DisplayFeatureManager;
import org.nameless.server.display.DisplayFeatureController;
import org.nameless.server.display.DisplayRefreshRateController;
import org.nameless.server.wm.DisplayResolutionController;

public class NamelessSystemExService extends SystemService {

    private static final String TAG = "NamelessSystemExService";

    private final ContentResolver mResolver;

    private final boolean mDisplayFeatureSupported =
            DisplayFeatureManager.getInstance().isSupported();

    private Handler mHandler;
    private ServiceThread mWorker;

    private PackageRemovedListener mPackageRemovedListener;
    private ScreenStateListener mScreenStateListener;

    private final Object mLock = new Object();

    private String mTopFullscreenPackage = "";

    public NamelessSystemExService(Context context) {
        super(context);
        mResolver = context.getContentResolver();
    }

    @Override
    public void onBootPhase(int phase) {
        if (phase == PHASE_SYSTEM_SERVICES_READY) {
            mPackageRemovedListener = new PackageRemovedListener();
            mScreenStateListener = new ScreenStateListener();
            if (mDisplayFeatureSupported) {
                DisplayFeatureController.getInstance().onSystemServicesReady();
            }
            return;
        }

        if (phase == PHASE_BOOT_COMPLETED) {
            if (mDisplayFeatureSupported) {
                DisplayFeatureController.getInstance().onBootCompleted();
            }
            DisplayRefreshRateController.getInstance().onBootCompleted();
            mPackageRemovedListener.register();
            mScreenStateListener.register();
            return;
        }
    }

    @Override
    public void onStart() {
        mWorker = new ServiceThread(TAG, THREAD_PRIORITY_DEFAULT, false);
        mWorker.start();
        mHandler = new Handler(mWorker.getLooper());

        TopActivityRecorder.getInstance().initSystemExService(this);
        if (mDisplayFeatureSupported) {
            DisplayFeatureController.getInstance().initSystemExService(this);
        }
        DisplayRefreshRateController.getInstance().initSystemExService(this);
        DisplayResolutionController.getInstance().initSystemExService(this);
    }

    @Override
    public void onUserSwitching(TargetUser from, TargetUser to) {
        final int newUserId = to.getUserIdentifier();
        DisplayRefreshRateController.getInstance().onUserSwitching(newUserId);
    }

    private void onPackageRemoved(String packageName) {
        DisplayRefreshRateController.getInstance().onPackageRemoved(packageName);
    }

    private void onScreenOff() {
        DisplayRefreshRateController.getInstance().onScreenOff();
    }

    private void onScreenOn() {
        DisplayRefreshRateController.getInstance().onScreenOn();
    }

    private void onScreenUnlocked() {
        onTopFullscreenPackageChanged(mTopFullscreenPackage);
    }

    public void onTopFullscreenPackageChanged(String packageName) {
        synchronized (mLock) {
            mTopFullscreenPackage = packageName;
            DisplayRefreshRateController.getInstance().updateRefreshRate(packageName);
        }
    }

    public ContentResolver getContentResolver() {
        return mResolver;
    }

    public Handler getHandler() {
        return mHandler;
    }

    public String getTopFullscreenPackage() {
        synchronized (mLock) {
            return mTopFullscreenPackage;
        }
    }

    private final class PackageRemovedListener extends BroadcastReceiver {

        private static final String EXTRA_UID = "EXTRA_UID";

        @Override
        public void onReceive(Context context, Intent intent) {
            final String packageName = getPackageName(intent);
            if (packageName == null) {
                return;
            }
            switch (intent.getAction()) {
                case ACTION_PACKAGE_FULLY_REMOVED:
                case ACTION_PACKAGE_REMOVED:
                    if (!intent.getBooleanExtra(EXTRA_REPLACING, false)) {
                        onPackageRemoved(packageName);
                    }
                    break;
            }
        }

        public void register() {
            final IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_PACKAGE_FULLY_REMOVED);
            filter.addAction(ACTION_PACKAGE_REMOVED);
            filter.addDataScheme("package");
            getContext().registerReceiverForAllUsers(this, filter, null, mHandler);
        }

        private static String getPackageName(Intent intent) {
            final Uri uri = intent.getData();
            return uri != null ? uri.getSchemeSpecificPart() : null;
        }
    }

    private final class ScreenStateListener extends BroadcastReceiver {

        private final KeyguardManager mKeyguardManager;
        private final PowerManager mPowerManager;

        private boolean mHandledUnlock = false;

        public ScreenStateListener() {
            mKeyguardManager = getContext().getSystemService(KeyguardManager.class);
            mPowerManager = getContext().getSystemService(PowerManager.class);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ACTION_SCREEN_OFF:
                    mHandledUnlock = false;
                    onScreenOff();
                    break;
                case ACTION_SCREEN_ON:
                    if (!mHandledUnlock && !mKeyguardManager.isKeyguardLocked()) {
                        onScreenUnlocked();
                    } else {
                        onScreenOn();
                    }
                    break;
                case ACTION_USER_PRESENT:
                    mHandledUnlock = true;
                    onScreenUnlocked();
                    break;
            }
        }

        public void register() {
            final IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_SCREEN_OFF);
            filter.addAction(ACTION_SCREEN_ON);
            filter.addAction(ACTION_USER_PRESENT);
            getContext().registerReceiverForAllUsers(this, filter, null, mHandler);
        }
    }
}
