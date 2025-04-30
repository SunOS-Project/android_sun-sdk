/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.server.am;

import static android.content.Intent.ACTION_SCREEN_CAMERA_GESTURE;

import static com.android.internal.util.sun.CustomUtils.INTENT_RESET_CLONE_USER_ID;

import static org.sun.content.OnlineConfigManager.ACTION_UPDATE_CONFIG;
import static org.sun.server.policy.DozeController.DOZE_INTENT;
import static org.sun.view.PopUpViewManager.ACTION_PIN_CURRENT_APP;
import static org.sun.view.PopUpViewManager.ACTION_START_MINI_WINDOW;
import static org.sun.view.PopUpViewManager.ACTION_START_PINNED_WINDOW;

import java.util.Set;

class ActivityManagerServiceExt {

    private static class InstanceHolder {
        private static final ActivityManagerServiceExt INSTANCE = new ActivityManagerServiceExt();
    }

    private static final Set<String> SYSTEM_BROADCAST_WHITELIST = Set.of(
        ACTION_PIN_CURRENT_APP,
        ACTION_SCREEN_CAMERA_GESTURE,
        ACTION_START_MINI_WINDOW,
        ACTION_START_PINNED_WINDOW,
        ACTION_UPDATE_CONFIG,
        DOZE_INTENT,
        INTENT_RESET_CLONE_USER_ID,
        "com.dolby.intent.action.DAP_PARAMS_UPDATE"
    );

    static ActivityManagerServiceExt getInstance() {
        return InstanceHolder.INSTANCE;
    }

    boolean allowBroadcastFromSystem(String action) {
        return SYSTEM_BROADCAST_WHITELIST.contains(action);
    }
}
