/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.server;

import android.content.Context;

import com.android.server.SystemService;
import com.android.server.wm.TopActivityRecorder;

public class NamelessSystemExService extends SystemService {

    private static final String TAG = "NamelessSystemExService";

    private final Object mLock = new Object();

    private String mTopFullscreenPackage = "";

    public NamelessSystemExService(Context context) {
        super(context);
    }

    @Override
    public void onStart() {
        TopActivityRecorder.getInstance().initSystemExService(this);
    }

    @Override
    public void onUserSwitching(TargetUser from, TargetUser to) {
        onTopFullscreenPackageChanged(mTopFullscreenPackage);
    }

    public void onTopFullscreenPackageChanged(String packageName) {
        synchronized (mLock) {
            mTopFullscreenPackage = packageName;
        }
    }
}
