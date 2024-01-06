/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.server.policy;

import com.android.internal.app.AssistUtils;

import org.nameless.audio.AlertSliderManager;

public class PhoneWindowManagerExt {

    private AssistUtils mAssistUtils;
    private PhoneWindowManager mPhoneWindowManager;

    private boolean mHasAlertSlider;

    private static class InstanceHolder {
        private static final PhoneWindowManagerExt INSTANCE = new PhoneWindowManagerExt();
    }

    public static PhoneWindowManagerExt getInstance() {
        return InstanceHolder.INSTANCE;
    }

    public void init(PhoneWindowManager pw) {
        mPhoneWindowManager = pw;
        mAssistUtils = new AssistUtils(pw.mContext);
        mHasAlertSlider = AlertSliderManager.hasAlertSlider(pw.mContext);
    }

    public boolean hasAssistant(int currentUserId) {
        return mAssistUtils.getAssistComponentForUser(currentUserId) != null;
    }

    public boolean interceptKeyBeforeQueueing(int keyCode, boolean down) {
        if (mHasAlertSlider && AlertSliderManager.maybeNotifyUpdate(
                mPhoneWindowManager.mContext, keyCode, down)) {
            return true;
        }
        return false;
    }
}
