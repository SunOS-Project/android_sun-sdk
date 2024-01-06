/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.server.policy;

import com.android.internal.app.AssistUtils;

public class PhoneWindowManagerExt {

    private AssistUtils mAssistUtils;

    private static class InstanceHolder {
        private static final PhoneWindowManagerExt INSTANCE = new PhoneWindowManagerExt();
    }

    public static PhoneWindowManagerExt getInstance() {
        return InstanceHolder.INSTANCE;
    }

    public void init(PhoneWindowManager pw) {
        mAssistUtils = new AssistUtils(pw.mContext);
    }

    public boolean hasAssistant(int currentUserId) {
        return mAssistUtils.getAssistComponentForUser(currentUserId) != null;
    }
}
