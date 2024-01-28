/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.server.am;

import static org.nameless.server.policy.DozeController.DOZE_INTENT;

public class ActivityManagerServiceExt {

    private static class InstanceHolder {
        private static final ActivityManagerServiceExt INSTANCE = new ActivityManagerServiceExt();
    }

    public static ActivityManagerServiceExt getInstance() {
        return InstanceHolder.INSTANCE;
    }

    public boolean allowBroadcastFromSystem(String action) {
        if (DOZE_INTENT.equals(action)) {
            return true;
        }
        return false;
    }
}
