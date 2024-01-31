/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.server.wm;

import static com.android.server.wm.SurfaceAnimator.ANIMATION_TYPE_SCREEN_ROTATION;

import static org.nameless.os.DebugConstants.DEBUG_POP_UP;

import android.graphics.Rect;
import android.util.Slog;
import android.view.SurfaceControl;

class SurfaceFreezerExt {

    private static final String TAG = "SurfaceFreezerExt";

    private final SurfaceFreezer mFreezer;
    private final SurfaceFreezer.Freezable mAnimatable;
    private final WindowManagerService mWmService;

    private int mPreFreezedWindowingMode;
    private TaskWindowSurfaceInfo mFreezeTaskWindowSurfaceInfo;

    boolean mNoWindowModeAnim;

    SurfaceFreezerExt(SurfaceFreezer freezer, SurfaceFreezer.Freezable animatable,
            WindowManagerService service) {
        mFreezer = freezer;
        mAnimatable = animatable;
        mWmService = service;
    }

    SurfaceFreezer.Snapshot getSnapshot() {
        return mFreezer.mSnapshot;
    }

    TaskWindowSurfaceInfo getFreezeTaskWindowSurfaceInfo() {
        final TaskWindowSurfaceInfo info = mFreezeTaskWindowSurfaceInfo;
        mFreezeTaskWindowSurfaceInfo = null;
        return info;
    }

    boolean hasFreezeTaskWindowSurfaceInfo() {
        return mFreezeTaskWindowSurfaceInfo != null;
    }

    void setPreFreezedWindowingMode(int windowingMode) {
        mPreFreezedWindowingMode = windowingMode;
    }

    void freeze(SurfaceControl.Transaction t, Rect startBounds,
            TaskWindowSurfaceInfo taskWindowSurfaceInfo) {
        reset(t);
        mFreezer.mFreezeBounds.set(startBounds);
        mFreezeTaskWindowSurfaceInfo = new TaskWindowSurfaceInfo(taskWindowSurfaceInfo, mPreFreezedWindowingMode);
        if (DEBUG_POP_UP) {
            Slog.d(TAG, "freeze(): startBounds=" + startBounds);
        }
        mFreezer.mLeash = SurfaceAnimatorExt.createAnimationLeash(
                mAnimatable, mAnimatable.getSurfaceControl(), t,
                ANIMATION_TYPE_SCREEN_ROTATION, startBounds.width(), startBounds.height(),
                false, mWmService.mTransactionFactory, mFreezeTaskWindowSurfaceInfo);
        mAnimatable.onAnimationLeashCreated(t, mFreezer.mLeash);
        t.apply();
    }

    void transitionFreeze(Rect startBounds, TaskWindowSurfaceInfo info) {
        if (!hasFreezeTaskWindowSurfaceInfo()) {
            if (DEBUG_POP_UP) {
                Slog.d(TAG, "transitionFreeze(): startBounds=" + startBounds +
                        ", mPreFreezedWindowingMode=" + mPreFreezedWindowingMode);
            }
            mFreezer.mFreezeBounds.set(startBounds);
            mFreezeTaskWindowSurfaceInfo = new TaskWindowSurfaceInfo(info, mPreFreezedWindowingMode);
        }
    }

    private void reset(SurfaceControl.Transaction t) {
        if (mFreezer.mSnapshot != null) {
            mFreezer.mSnapshot.destroy(t);
            mFreezer.mSnapshot = null;
        }
        if (mFreezer.mLeash != null) {
            t.remove(mFreezer.mLeash);
            mFreezer.mLeash = null;
        }
    }
}
