/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.server.policy;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.os.UserHandle;
import android.provider.Settings;

import com.android.internal.app.AssistUtils;

import org.nameless.audio.AlertSliderManager;
import org.nameless.provider.SettingsExt;

public class PhoneWindowManagerExt {

    private AssistUtils mAssistUtils;
    private PhoneWindowManager mPhoneWindowManager;

    private boolean mHasAlertSlider;
    private boolean mClickPartialScreenshot;

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

    public void observe(ContentResolver resolver, ContentObserver observer) {
        resolver.registerContentObserver(Settings.System.getUriFor(
                SettingsExt.System.CLICK_PARTIAL_SCREENSHOT), false, observer,
                UserHandle.USER_ALL);
    }

    public void updateSettings(ContentResolver resolver) {
        mClickPartialScreenshot = Settings.System.getIntForUser(resolver,
                SettingsExt.System.CLICK_PARTIAL_SCREENSHOT, 0,
                UserHandle.USER_CURRENT) == 1;
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

    public boolean isClickPartialScreenshot() {
        return mClickPartialScreenshot;
    }
}
