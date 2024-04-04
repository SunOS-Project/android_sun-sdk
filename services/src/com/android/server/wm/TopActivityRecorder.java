/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.server.wm;

import static android.app.ActivityTaskManager.INVALID_TASK_ID;
import static android.os.Process.THREAD_PRIORITY_DEFAULT;

import static com.android.server.wm.PopUpWindowController.PACKAGE_NAME_SYSTEM_TOOL;

import static org.nameless.content.ContextExt.APP_FOCUS_MANAGER_SERVICE;
import static org.nameless.os.DebugConstants.DEBUG_WMS_TOP_APP;

import android.app.WindowConfiguration;
import android.content.ComponentName;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;

import com.android.server.ServiceThread;

import java.util.ArrayList;

import org.nameless.server.NamelessSystemExService;
import org.nameless.view.IAppFocusManagerService;
import org.nameless.view.IAppFocusObserver;
import org.nameless.view.TopAppInfo;

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

    private final Handler mHandler;
    private final ServiceThread mServiceThread;

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
        public TopAppInfo getTopAppInfo() {
            synchronized (mFocusLock) {
                final int n = mTopMiniWindowActivity.size();
                final ActivityInfo info = n > 0 ? mTopMiniWindowActivity.get(n - 1)
                        : mTopFullscreenActivity;
                if (info == null) {
                    return null;
                }
                final TopAppInfo.Builder builder = new TopAppInfo.Builder();
                builder.setComponentName(info.componentName);
                final Task task = info.task;
                if (task != null) {
                    builder.setTaskId(task.mTaskId);
                    builder.setWindowingMode(task.getWindowConfiguration().getWindowingMode());
                } else {
                    builder.setTaskId(INVALID_TASK_ID);
                    builder.setWindowingMode(WindowConfiguration.WINDOWING_MODE_UNDEFINED);
                }
                final TopAppInfo ret = builder.build();
                logD("getTopAppInfo, ret=" + ret);
                return ret;
            }
        }

        @Override
        public TopAppInfo getTopFullscreenAppInfo() {
            synchronized (mFocusLock) {
                if (mTopFullscreenActivity == null) {
                    return null;
                }
                final TopAppInfo.Builder builder = new TopAppInfo.Builder();
                builder.setComponentName(mTopFullscreenActivity.componentName);
                final Task task = mTopFullscreenActivity.task;
                if (task != null) {
                    builder.setTaskId(task.mTaskId);
                    builder.setWindowingMode(task.getWindowConfiguration().getWindowingMode());
                } else {
                    builder.setTaskId(INVALID_TASK_ID);
                    builder.setWindowingMode(WindowConfiguration.WINDOWING_MODE_UNDEFINED);
                }
                final TopAppInfo ret = builder.build();
                logD("getTopFullscreenAppInfo, ret=" + ret);
                return ret;
            }
        }

        @Override
        public boolean hasMiniWindowFocus() {
            return hasMiniWindow();
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
    private ActivityInfo mTopPinnedWindowActivity = null;
    private ArrayList<ActivityInfo> mTopMiniWindowActivity = new ArrayList<>();

    private NamelessSystemExService mSystemExService;
    private WindowManagerService mWms;

    private TopActivityRecorder() {
        mServiceThread = new ServiceThread(TAG, THREAD_PRIORITY_DEFAULT, false);
        mServiceThread.start();
        mHandler = new Handler(mServiceThread.getLooper());
    }

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
            if (WindowConfiguration.isMiniExtWindowMode(windowingMode)) {
                boolean hasTask = false;
                for (ActivityInfo ai : mTopMiniWindowActivity) {
                    if (ai.task == newTask) {
                        hasTask = true;
                        ai.componentName = newFocus.mActivityComponent;
                        ai.packageName = newFocus.packageName;
                        break;
                    }
                }
                if (!hasTask) {
                    mTopMiniWindowActivity.add(new ActivityInfo(newFocus, newTask));
                }
                logD("Top mini-window activity changed to " + newFocus + ", addedTaskBefore=" + hasTask);
                DimmerWindow.getInstance().setTask(newTask);
            } else if (windowingMode == WindowConfiguration.WINDOWING_MODE_UNDEFINED
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
                    mHandler.post(() -> notifyFullscreenComponentChanged(oldComponent, newComponent));
                }
            }
        }
    }

    void updateTopPinnedWindowActivity(ActivityRecord newActivity) {
        synchronized (mFocusLock) {
            if (mTopPinnedWindowActivity != null &&
                    newActivity != null &&
                    mTopPinnedWindowActivity.task == newActivity.getTask()) {
                mTopPinnedWindowActivity.componentName = newActivity.mActivityComponent;
                mTopPinnedWindowActivity.packageName = newActivity.packageName;
            } else {
                mTopPinnedWindowActivity = new ActivityInfo(newActivity, newActivity.getTask());
            }
            if (mTopPinnedWindowActivity.task != null) {
                PinnedWindowOverlayController.getInstance().setTask(mTopPinnedWindowActivity.task);
                logD("Top pinned-window activity changed to " + newActivity);
            } else {
                PinnedWindowOverlayController.getInstance().setTask(null);
                logD("Top pinned-window activity changed to null");
            }
        }
    }

    private ComponentName getTopFullscreenComponentLocked() {
        if (mTopFullscreenActivity == null) {
            return null;
        }
        return mTopFullscreenActivity.componentName;
    }

    String getTopFullscreenPackage() {
        synchronized (mFocusLock) {
            if (mTopFullscreenActivity == null) {
                return "";
            }
            return mTopFullscreenActivity.packageName;
        }
    }

    Task getTopFullscreenTask() {
        synchronized (mFocusLock) {
            if (mTopFullscreenActivity == null) {
                return null;
            }
            return mTopFullscreenActivity.task;
        }
    }

    int getTopFullscreenTaskId() {
        synchronized (mFocusLock) {
            if (mTopFullscreenActivity == null) {
                return INVALID_TASK_ID;
            }
            if (mTopFullscreenActivity.task == null) {
                return INVALID_TASK_ID;
            }
            return mTopFullscreenActivity.task.mTaskId;
        }
    }

    String getTopPinnedWindowPackage() {
        synchronized (mFocusLock) {
            if (mTopPinnedWindowActivity == null) {
                return "";
            }
            return mTopPinnedWindowActivity.packageName;
        }
    }

    String getTopMiniWindowPackage() {
        synchronized (mFocusLock) {
            final int n = mTopMiniWindowActivity.size();
            if (n == 0) {
                return "";
            }
            return mTopMiniWindowActivity.get(n - 1).packageName;
        }
    }

    private Task getTopMiniWindowTaskLocked() {
        final int n = mTopMiniWindowActivity.size();
        if (n == 0) {
            return null;
        }
        return mTopMiniWindowActivity.get(n - 1).task;
    }

    boolean isTopFullscreenActivityHome() {
        synchronized (mFocusLock) {
            if (mTopFullscreenActivity == null) {
                return false;
            }
            return mTopFullscreenActivity.isHome;
        }
    }

    boolean isPackageAtTop(String packageName) {
        return getTopFullscreenPackage().equals(packageName) ||
                getTopMiniWindowPackage().equals(packageName) ||
                getTopPinnedWindowPackage().equals(packageName);
    }

    boolean hasPinnedWindow() {
        synchronized (mFocusLock) {
            return mTopPinnedWindowActivity != null;
        }
    }

    public boolean hasMiniWindow() {
        synchronized (mFocusLock) {
            return mTopMiniWindowActivity.size() > 0;
        }
    }

    private String getPackageNameFromTask(Task task) {
        final ActivityRecord taskActivity = task.getActivity((r) -> true);
        if (taskActivity != null) {
            return taskActivity.packageName;
        }
        return "";
    }

    void removeMiniWindowTask(Task task) {
        synchronized (mFocusLock) {
            final int n = mTopMiniWindowActivity.size();
            for (int i = n - 1; i >= 0; --i) {
                if (mTopMiniWindowActivity.get(i).task == task) {
                    final ActivityInfo ai = mTopMiniWindowActivity.remove(i);
                    logD("removeMiniWindowTask: " + ai);
                    if (n == 1) {
                        DimmerWindow.getInstance().setTask(null);
                    } else {
                        DimmerWindow.getInstance().setTask(mTopMiniWindowActivity.get(n - 2).task);
                    }
                    return;
                }
            }
            logD("removeMiniWindowTask, unable to find task: " + task);
        }
    }

    void moveTopMiniToPinned(Task task) {
        synchronized (mFocusLock) {
            final int n = mTopMiniWindowActivity.size();
            if (n == 0) {
                return;
            }
            final Task prevPinnedTask = PinnedWindowOverlayController.getInstance().getTask();
            if (prevPinnedTask != null) {
                prevPinnedTask.setAlwaysOnTop(false);
            }
            mTopPinnedWindowActivity = new ActivityInfo(mTopMiniWindowActivity.get(n - 1));
            logD("moveTopMiniToPinned: " + mTopPinnedWindowActivity);
            mTopMiniWindowActivity.clear();
            DimmerWindow.getInstance().setTask(null);
            mHandler.postDelayed(() -> {
                PinnedWindowOverlayController.getInstance().setTask(task);
                if (prevPinnedTask != null) {
                    PopUpWindowController.getInstance().moveActivityTaskToBack(prevPinnedTask,
                            PopUpWindowController.MOVE_TO_BACK_NEW_PIN);
                }
            }, WindowChangeAnimationSpecExt.ANIMATION_DURATION_MODE_CHANGING);
        }
    }

    boolean moveTopPinnedToMini() {
        synchronized (mFocusLock) {
            if (mTopPinnedWindowActivity == null) {
                return false;
            }
            final Task prevMiniTask = getTopMiniWindowTaskLocked();
            mTopMiniWindowActivity.clear();
            mTopMiniWindowActivity.add(new ActivityInfo(mTopPinnedWindowActivity));
            logD("moveTopPinnedToMini: " + mTopPinnedWindowActivity);
            DimmerWindow.getInstance().setTask(mTopPinnedWindowActivity.task);
            mTopPinnedWindowActivity = null;
            PinnedWindowOverlayController.getInstance().setTask(null);
            mHandler.postDelayed(() -> {
                if (prevMiniTask != null) {
                    PopUpWindowController.getInstance().moveActivityTaskToBack(prevMiniTask,
                            PopUpWindowController.MOVE_TO_BACK_NEW_MINI);
                }
            }, WindowChangeAnimationSpecExt.ANIMATION_DURATION_MODE_CHANGING);
            return prevMiniTask != null;
        }
    }

    void clearMiniWindow() {
        synchronized (mFocusLock) {
            logD("clearMiniWindow");
            mTopMiniWindowActivity.clear();
            DimmerWindow.getInstance().setTask(null);
        }
    }

    void clearPinnedWindow() {
        synchronized (mFocusLock) {
            logD("clearPinnedWindow");
            mTopPinnedWindowActivity = null;
            PinnedWindowOverlayController.getInstance().setTask(null);
        }
    }

    void onForceStopPackage(String packageName) {
        synchronized (mFocusLock) {
            for (int i = mTopMiniWindowActivity.size() - 1; i >= 0; --i) {
                if (packageName.equals(mTopMiniWindowActivity.get(i).packageName)) {
                    mTopMiniWindowActivity.remove(i);
                }
            }
        }
    }

    boolean isTaskSystemTool(Task task) {
        if (task == null) {
            return false;
        }
        return PACKAGE_NAME_SYSTEM_TOOL.equals(getPackageNameFromTask(task));
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

        ActivityInfo(ActivityInfo other) {
            this.componentName = other.componentName;
            this.packageName = other.packageName;
            this.task = other.task;
            this.isHome = other.isHome;
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
