/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.server.am;

import static org.nameless.server.policy.DozeController.DOZE_INTENT;

import android.content.Intent;

import com.android.server.wm.PopUpBroadcastReceiver;

class ActivityManagerServiceExt {

    private static class InstanceHolder {
        private static final ActivityManagerServiceExt INSTANCE = new ActivityManagerServiceExt();
    }

    static ActivityManagerServiceExt getInstance() {
        return InstanceHolder.INSTANCE;
    }

    boolean allowBroadcastFromSystem(String action) {
        if (DOZE_INTENT.equals(action)) {
            return true;
        }
        return false;
    }

    Intent hookIntentBeforeBroadcast(Intent intent) {
        intent = PopUpBroadcastReceiver.getInstance().hookMiFreeformIntent(intent);
        return intent;
    }
}
