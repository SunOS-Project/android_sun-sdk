/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.server.wm;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.UserHandle;
import android.provider.Settings;

import com.android.server.DisplayThread;

import org.nameless.provider.SettingsExt;
import org.nameless.server.policy.gesture.ThreeFingerGestureController;
import org.nameless.server.wm.DisplayResolutionController;
import org.nameless.view.DisplayResolutionManager;

class WindowManagerServiceExt {

    private static class InstanceHolder {
        private static final WindowManagerServiceExt INSTANCE = new WindowManagerServiceExt();
    }

    static WindowManagerServiceExt getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private static final Uri URI_IGNORE_FLAG_SECURE =
            Settings.Secure.getUriFor(SettingsExt.Secure.WINDOW_IGNORE_SECURE);

    private WindowManagerService mWms;

    private boolean mIgnoreFlagSecure;

    void init(WindowManagerService wms) {
        mWms = wms;
        TopActivityRecorder.getInstance().initWms(wms);
        PopUpWindowController.getInstance().init(wms.mContext, wms);
        DisplayResolutionController.getInstance().init(wms.mContext, wms);
        ThreeFingerGestureController.getInstance().init(wms.mContext, wms);
    }

    void systemReady() {
        DisplayResolutionController.getInstance().systemReady();
        PopUpWindowController.getInstance().systemReady();
    }

    void registerContentObserver(ContentObserver observer) {
        final ContentResolver cr = mWms.mContext.getContentResolver();
        cr.registerContentObserver(URI_IGNORE_FLAG_SECURE, false,
                observer, UserHandle.USER_ALL);
    }

    void loadSettings() {
        updateIgnoreFlagSecure();
    }

    boolean onSettingsChanged(Uri uri) {
        if (URI_IGNORE_FLAG_SECURE.equals(uri)) {
            updateIgnoreFlagSecure();
            return true;
        }
        return false;
    }

    void onUserSwitched() {
        loadSettings();
    }

    boolean shouldIgnoreFlagSecure() {
        return mIgnoreFlagSecure;
    }

    int getDensityWithScale(int density) {
        final int width = DisplayResolutionController.getInstance().getResolution().x;
        if (width > 0) {
            return (int) (density * DisplayResolutionManager.getDensityScale(width));
        }
        return density;
    }

    private void updateIgnoreFlagSecure() {
        mIgnoreFlagSecure = Settings.Secure.getIntForUser(
                mWms.mContext.getContentResolver(),
                SettingsExt.Secure.WINDOW_IGNORE_SECURE,
                0, mWms.mCurrentUserId) != 0;
    }
}
