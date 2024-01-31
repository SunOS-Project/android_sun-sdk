/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.server.wm;

import static android.app.ActivityTaskManager.INVALID_TASK_ID;

import static org.nameless.content.ContextExt.APP_FOCUS_MANAGER_SERVICE;
import static org.nameless.os.DebugConstants.DEBUG_WMS_TOP_APP;

import android.app.WindowConfiguration;
import android.content.ComponentName;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;

import java.util.ArrayList;

import org.nameless.server.NamelessSystemExService;
import org.nameless.view.IAppFocusManagerService;
import org.nameless.view.IAppFocusObserver;

public class TopActivityRecorder {

    private static final String TAG = "TopActivityRecorder";

    private static class InstanceHolder {
        private static TopActivityRecorder INSTANCE = new TopActivityRecorder();
    }

    public static TopActivityRecorder getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private final Object mFocusLock = new Object();
    private final Object mObserverLock = new Object();

    private final Handler mHandler = new Handler();

    private final ArrayList<AppFocusObserver> mObservers = new ArrayList<>();

    private final class AppFocusObserver {
        final IAppFocusObserver mObserver;
        final boolean mObserveActivity;
        final IBinder.DeathRecipient mDeathRecipient;

        AppFocusObserver(IAppFocusObserver observer,
                boolean observeActivity,
                IBinder.DeathRecipient deathRecipient) {
            mObserver = observer;
            mObserveActivity = observeActivity;
            mDeathRecipient = deathRecipient;
        }
    }

    private final class AppFocusManagerService extends IAppFocusManagerService.Stub {
        @Override
        public ComponentName getTopFullscreenComponent() {
            synchronized (mFocusLock) {
                return getTopFullscreenComponentLocked();
            }
        }

        @Override
        public boolean registerAppFocusObserver(IAppFocusObserver observer, boolean observeActivity) {
            final IBinder observerBinder = observer.asBinder();
            IBinder.DeathRecipient dr = new IBinder.DeathRecipient() {
                @Override
                public void binderDied() {
                    synchronized (mObserverLock) {
                        for (int i = 0; i < mObservers.size(); i++) {
                            if (observerBinder == mObservers.get(i).mObserver.asBinder()) {
                                AppFocusObserver removed = mObservers.remove(i);
                                IBinder binder = removed.mObserver.asBinder();
                                if (binder != null) {
                                    binder.unlinkToDeath(this, 0);
                                }
                                i--;
                            }
                        }
                    }
                }
            };

            synchronized (mObserverLock) {
                try {
                    observer.asBinder().linkToDeath(dr, 0);
                    mObservers.add(new AppFocusObserver(observer, observeActivity, dr));
                } catch (RemoteException e) {
                    // Client died, no cleanup needed.
                    return false;
                }
                return true;
            }
        }

        @Override
        public boolean unregisterAppFocusObserver(IAppFocusObserver observer) {
            boolean found = false;
            final IBinder observerBinder = observer.asBinder();
            synchronized (mObserverLock) {
                for (int i = 0; i < mObservers.size(); i++) {
                    found = true;
                    AppFocusObserver focusObserver = mObservers.get(i);
                    if (observerBinder == focusObserver.mObserver.asBinder()) {
                        AppFocusObserver removed = mObservers.remove(i);
                        IBinder binder = removed.mObserver.asBinder();
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

    private ActivityInfo mTopFullscreenActivity = null;

    private NamelessSystemExService mSystemExService;
    private WindowManagerService mWms;

    public void initSystemExService(NamelessSystemExService service) {
        mSystemExService = service;
        mSystemExService.publishBinderService(APP_FOCUS_MANAGER_SERVICE, new AppFocusManagerService());
    }

    void initWms(WindowManagerService wms) {
        mWms = wms;
    }

    void onAppFocusChanged(ActivityRecord focus, Task task) {
        synchronized (mFocusLock) {
            final DisplayContent dc = mWms.getDefaultDisplayContentLocked();
            final ActivityRecord newFocus = focus != null ? focus : dc.topRunningActivity();
            if (newFocus == null) {
                return;
            }
            final Task newTask = task != null ? task : newFocus.getTask();
            if (newTask == null) {
                return;
            }
            final int windowingMode = newTask.getWindowConfiguration().getWindowingMode();
            if (windowingMode == WindowConfiguration.WINDOWING_MODE_UNDEFINED
                    || windowingMode == WindowConfiguration.WINDOWING_MODE_FULLSCREEN) {
                final ComponentName oldComponent = getTopFullscreenComponentLocked();
                final ComponentName newComponent = newFocus.mActivityComponent;
                if (!newComponent.equals(oldComponent)) {
                    if (mTopFullscreenActivity != null &&
                            mTopFullscreenActivity.task == newTask) {
                        mTopFullscreenActivity.componentName = newFocus.mActivityComponent;
                        mTopFullscreenActivity.packageName = newFocus.packageName;
                    } else {
                        mTopFullscreenActivity = new ActivityInfo(newFocus, newTask);
                    }
                    logD("Top fullscreen window activity changed to " + newFocus);
                    mHandler.post(() -> {
                        notifyFullscreenComponentChanged(oldComponent, newComponent);
                    });
                }
            }
        }
    }

    private ComponentName getTopFullscreenComponentLocked() {
        if (mTopFullscreenActivity == null) {
            return null;
        }
        return mTopFullscreenActivity.componentName;
    }

    private void notifyFullscreenComponentChanged(ComponentName oldComponent, ComponentName newComponent) {
        final String newPackageName = newComponent.getPackageName();
        final String newActivityName = newComponent.getClassName();
        final boolean packageChanged = oldComponent == null ||
                !newPackageName.equals(oldComponent.getPackageName());
        final boolean activityChanged = oldComponent == null ||
                !newActivityName.equals(oldComponent.getClassName());
        synchronized (mObserverLock) {
            if (packageChanged) {
                mSystemExService.onTopFullscreenPackageChanged(newPackageName);
            }
            for (AppFocusObserver observer : mObservers) {
                if (!packageChanged && !observer.mObserveActivity) {
                    continue;
                }
                try {
                    observer.mObserver.onFullscreenFocusChanged(newPackageName, newActivityName);
                } catch (RemoteException | RuntimeException e) {
                    logE("Failed to notify fullscreen component change");
                }
            }
        }
    }

    private static final class ActivityInfo {
        ComponentName componentName;
        String packageName;
        Task task;
        boolean isHome;

        ActivityInfo(ActivityRecord r, Task task) {
            this.componentName = r.mActivityComponent;
            this.packageName = r.packageName;
            this.task = task;
            this.isHome = r.isActivityTypeHome();
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("componentName=").append(componentName);
            sb.append(", task=").append(task);
            sb.append(", isHome=").append(isHome);
            return sb.toString();
        }
    }

    private static void logD(String msg) {
        if (DEBUG_WMS_TOP_APP) {
            Slog.d(TAG, msg);
        }
    }

    private static void logE(String msg) {
        Slog.e(TAG, msg);
    }
}
