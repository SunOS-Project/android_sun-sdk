/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.server.pm;

import static android.app.ApplicationExitInfo.REASON_OTHER;
import static android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
import static android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;

import static org.sun.os.DebugConstants.DEBUG_PMS;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.ArraySet;
import android.util.Slog;
import android.view.DisplayCutout;
import android.view.InsetsSource;
import android.view.InsetsState;
import android.view.WindowInsets.Type;

import com.android.internal.R;
import com.android.internal.pm.pkg.component.ComponentMutateUtils;
import com.android.internal.pm.pkg.component.ParsedActivity;

import com.android.server.pm.PackageManagerService.IPackageManagerImpl;
import com.android.server.pm.pkg.AndroidPackage;
import com.android.server.pm.pkg.PackageStateInternal;

import java.util.Collections;
import java.util.List;

public class ForceFullController {

    private static class InstanceHolder {
        private static ForceFullController INSTANCE = new ForceFullController();
    }

    public static ForceFullController getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private static final String TAG = "ForceFullController";

    private static final int ID_DISPLAY_CUTOUT_LEFT = InsetsSource.createId(null, 0, Type.displayCutout());
    private static final int ID_DISPLAY_CUTOUT_TOP = InsetsSource.createId(null, 1, Type.displayCutout());
    private static final int ID_DISPLAY_CUTOUT_RIGHT = InsetsSource.createId(null, 2, Type.displayCutout());
    private static final int ID_DISPLAY_CUTOUT_BOTTOM = InsetsSource.createId(null, 3, Type.displayCutout());

    private final ArraySet<String> mBlacklistApps = new ArraySet<>();

    private PackageManagerService mPms;
    private IPackageManagerImpl mImpl;

    void initPms(PackageManagerService pms, IPackageManagerImpl impl) {
        mPms = pms;
        mImpl = impl;
        Collections.addAll(mBlacklistApps, mPms.mContext.getResources().getStringArray(R.array.config_forceFullBlacklistApps));
    }

    boolean isForceFull(String packageName) {
        if (mBlacklistApps.contains(packageName)) {
            return false;
        }
        final Computer computer = mPms.snapshotComputer();
        final PackageStateInternal ps = computer.getPackageStateInternal(packageName);
        if (ps == null) {
            return false;
        }
        final boolean ret = ps.isForceFull();
        if (DEBUG_PMS) {
            Slog.d(TAG, "isForceFull, pkg=" + packageName + ", isForceFull" + ret);
        }
        return ret;
    }

    boolean setForceFull(String packageName, boolean forceFull) {
        final Computer computer = mPms.snapshotComputer();
        final AndroidPackage pkg = computer.getPackage(packageName);
        final PackageStateInternal ps = computer.getPackageStateInternal(packageName);
        boolean newForceFull = forceFull;
        if (mBlacklistApps.contains(packageName)) {
            if (DEBUG_PMS) {
                Slog.d(TAG, "setForceFull, pkg is in blacklist");
            }
            newForceFull = false;
        }
        if (pkg == null || ps == null || ps.isForceFull() == newForceFull) {
            return false;
        }
        if (DEBUG_PMS) {
            Slog.d(TAG, "setForceFull, pkg=" + packageName + ", forceFull=" + newForceFull);
        }
        setMaxAspectRatio(pkg, newForceFull);
        final boolean finalForceFull = newForceFull;
        mPms.commitPackageStateMutation(null, packageName, state -> {
            state.setForceFull(finalForceFull);
        });
        mPms.killApplication(packageName, ps.getAppId(), "forceFull", REASON_OTHER);
        mPms.scheduleWriteSettings();
        return true;
    }

    void setMaxAspectRatio(AndroidPackage pkg, boolean forceFull) {
        if (mBlacklistApps.contains(pkg.getPackageName())) {
            return;
        }
        float maxAspect = forceFull ? 3.0f : 1.86f;
        if (pkg.getTargetSdkVersion() >= 26) {
            maxAspect = 0.0f;
        }
        final float maxAspectRatio = pkg.getMaxAspectRatio();
        if (maxAspectRatio != 0.0f) {
            maxAspect = maxAspectRatio;
        } else {
            final Bundle metaData = pkg.getMetaData();
            if (metaData != null && metaData.containsKey("android.max_aspect")) {
                maxAspect = metaData.getFloat("android.max_aspect", maxAspect);
            }
        }
        final List<ParsedActivity> activities = pkg.getActivities();
        for (int i = 0; i < activities.size(); i++) {
            final ParsedActivity parsedActivity = activities.get(i);
            ComponentMutateUtils.setMaxAspectRatio(parsedActivity, parsedActivity.getResizeMode(),
                    parsedActivity.getMetaData().getFloat("android.max_aspect", maxAspect));
        }
    }

    public InsetsState adjustInsetsForWindow(ActivityInfo info, InsetsState state) {
        if (info == null) {
            return state;
        }
        if (mBlacklistApps.contains(info.packageName)) {
            return state;
        }
        if (info.applicationInfo.isForceFull()) {
            state = new InsetsState(state);
            state.removeSource(ID_DISPLAY_CUTOUT_LEFT);
            state.removeSource(ID_DISPLAY_CUTOUT_TOP);
            state.removeSource(ID_DISPLAY_CUTOUT_RIGHT);
            state.removeSource(ID_DISPLAY_CUTOUT_BOTTOM);
            state.setDisplayCutout(DisplayCutout.NO_CUTOUT);
        }
        return state;
    }

    public int getCutoutMode(int mode, ActivityInfo info, int width, int height) {
        if (info == null) {
            return mode;
        }
        if (mBlacklistApps.contains(info.packageName)) {
            return mode;
        }
        if (info.applicationInfo.isForceFull()) {
            if (width > height) {
                return LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
            }
            return LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        return mode;
    }
}
