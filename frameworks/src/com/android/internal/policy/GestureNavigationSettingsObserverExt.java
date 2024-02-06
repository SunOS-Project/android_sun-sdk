/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.internal.policy;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.UserHandle;
import android.provider.Settings;

import org.nameless.provider.SettingsExt;

/** @hide */
public class GestureNavigationSettingsObserverExt {

    private static class InstanceHolder {
        private static final GestureNavigationSettingsObserverExt INSTANCE = new GestureNavigationSettingsObserverExt();
    }

    public static GestureNavigationSettingsObserverExt getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private Context mContext;

    void init(Context context) {
        mContext = context;
    }

    void register(ContentResolver cr, ContentObserver observer) {
        cr.registerContentObserver(Settings.System.getUriFor(
                SettingsExt.System.BACK_GESTURE_ARROW), false,
                observer, UserHandle.USER_ALL);
    }

    public boolean isBackGestureArrowVisible() {
        return Settings.System.getIntForUser(mContext.getContentResolver(),
                SettingsExt.System.BACK_GESTURE_ARROW,
                1, UserHandle.USER_CURRENT) != 0;
    }
}
