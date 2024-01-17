/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.systemui.statusbar.policy;

import android.annotation.Nullable;
import android.content.Context;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.nameless.view.AppFocusManager;
import org.nameless.view.IAppFocusObserver;

@Singleton
public class ForegroundActivityListener {

    private final AppFocusManager mAppFocusManager;

    private @Nullable String mFullscreenPackageName;
    private @Nullable String mFullscreenActivityName;

    private final IAppFocusObserver.Stub mAppFocusObserver = new IAppFocusObserver.Stub() {
        @Override
        public void onFullscreenFocusChanged(String packageName, String activityName) {
            mFullscreenPackageName = packageName;
            mFullscreenActivityName = activityName;
        }
    };

    @Inject
    public ForegroundActivityListener(Context context) {
        mAppFocusManager = context.getSystemService(AppFocusManager.class);
        mAppFocusManager.registerAppFocusObserver(mAppFocusObserver, true);
    }

    @Nullable
    public String getForegroundFullscreenPackageName() {
        return mFullscreenPackageName;
    }

    @Nullable
    public String getForegroundFullscreenActivityName() {
        return mFullscreenActivityName;
    }
}
