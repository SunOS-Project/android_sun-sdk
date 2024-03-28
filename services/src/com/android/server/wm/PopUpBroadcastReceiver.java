/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.server.wm;

import static android.app.ActivityTaskManager.INVALID_TASK_ID;
import static android.app.WindowConfiguration.WINDOWING_MODE_PINNED_WINDOW_EXT;
import static android.app.WindowConfiguration.WINDOWING_MODE_MINI_WINDOW_EXT;

import static org.nameless.view.PopUpViewManager.ACTION_PIN_CURRENT_APP;
import static org.nameless.view.PopUpViewManager.ACTION_START_MINI_WINDOW;
import static org.nameless.view.PopUpViewManager.ACTION_START_PINNED_WINDOW;
import static org.nameless.view.PopUpViewManager.EXTRA_ACTIVITY_NAME;
import static org.nameless.view.PopUpViewManager.EXTRA_PACKAGE_NAME;
import static org.nameless.view.PopUpViewManager.FEATURE_SUPPORTED;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;

import com.android.internal.util.nameless.CustomUtils;

public class PopUpBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "PopUpBroadcastReceiver";

    private static final boolean ALLOW_START_TOP = false;

    private static class InstanceHolder {
        private static final PopUpBroadcastReceiver INSTANCE = new PopUpBroadcastReceiver();
    }

    public static PopUpBroadcastReceiver getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private boolean mBootCompleted = false;

    void init(Context context, Handler handler) {
        if (FEATURE_SUPPORTED) {
            final IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_START_MINI_WINDOW);
            filter.addAction(ACTION_START_PINNED_WINDOW);
            filter.addAction(ACTION_PIN_CURRENT_APP);
            context.registerReceiverForAllUsers(this, filter, null, handler);
        }

        mBootCompleted = true;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if (ACTION_PIN_CURRENT_APP.equals(action)) {
            if (TopActivityRecorder.getInstance().hasMiniWindow()) {
                return;
            }
            if (TopActivityRecorder.getInstance().isTopFullscreenActivityHome()) {
                return;
            }
            final int topTaskId = TopActivityRecorder.getInstance().getTopFullscreenTaskId();
            if (topTaskId != INVALID_TASK_ID) {
                PopUpWindowController.getInstance().notifyNextRecentIsPin();
                PopUpAppStarter.getInstance().startActivity(topTaskId, PopUpAppStarter.getPinnedWindowBundle());
            }
            return;
        }

        final String packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME);
        if (TextUtils.isEmpty(packageName)) {
            return;
        }
        if (!CustomUtils.isPackageInstalled(context, packageName, false)) {
            return;
        }
        if (TopActivityRecorder.getInstance().isPackageAtTop(packageName) && !ALLOW_START_TOP) {
            return;
        }

        final String activityName = intent.getStringExtra(EXTRA_ACTIVITY_NAME);

        Bundle bundle = null;
        switch (action) {
            case ACTION_START_MINI_WINDOW:
                bundle = PopUpAppStarter.getMiniWindowBundle();
                break;
            case ACTION_START_PINNED_WINDOW:
                bundle = PopUpAppStarter.getPinnedWindowBundle();
                break;
        }
        if (bundle != null) {
            PopUpAppStarter.getInstance().startActivity(packageName, activityName, bundle);
        }
    }
}
