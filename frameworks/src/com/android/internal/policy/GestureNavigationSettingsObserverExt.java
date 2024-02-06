/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.internal.policy;

import static com.android.internal.policy.GestureLongSwipeUtils.ACTION_EMPTY;
import static com.android.internal.policy.GestureLongSwipeUtils.ACTION_END;
import static com.android.internal.policy.GestureLongSwipeUtils.DEFAULT_LONG_SWIPE_THRESHOLD_LAND;
import static com.android.internal.policy.GestureLongSwipeUtils.DEFAULT_LONG_SWIPE_THRESHOLD_PORT;

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
        cr.registerContentObserver(Settings.System.getUriFor(
                SettingsExt.System.LEFT_LONG_BACK_SWIPE_ACTION), false,
                observer, UserHandle.USER_ALL);
        cr.registerContentObserver(Settings.System.getUriFor(
                SettingsExt.System.RIGHT_LONG_BACK_SWIPE_ACTION), false,
                observer, UserHandle.USER_ALL);
        cr.registerContentObserver(Settings.System.getUriFor(
                SettingsExt.System.LONG_BACK_SWIPE_THRESHOLD_PORT), false,
                observer, UserHandle.USER_ALL);
        cr.registerContentObserver(Settings.System.getUriFor(
                SettingsExt.System.LONG_BACK_SWIPE_THRESHOLD_LAND), false,
                observer, UserHandle.USER_ALL);
    }

    public boolean isBackGestureArrowVisible() {
        return Settings.System.getIntForUser(mContext.getContentResolver(),
                SettingsExt.System.BACK_GESTURE_ARROW,
                1, UserHandle.USER_CURRENT) != 0;
    }

    public int getLongSwipeLeftAction() {
        final int action = Settings.System.getIntForUser(mContext.getContentResolver(),
                SettingsExt.System.LEFT_LONG_BACK_SWIPE_ACTION,
                ACTION_EMPTY, UserHandle.USER_CURRENT);
        if (action >= 0 && action <= ACTION_END) {
            return action;
        }
        return ACTION_EMPTY;
    }

    public int getLongSwipeRightAction() {
        final int action = Settings.System.getIntForUser(mContext.getContentResolver(),
                SettingsExt.System.RIGHT_LONG_BACK_SWIPE_ACTION,
                ACTION_EMPTY, UserHandle.USER_CURRENT);
        if (action >= 0 && action <= ACTION_END) {
            return action;
        }
        return ACTION_EMPTY;
    }

    public float getLongSwipePortraitThreshold() {
        return Settings.System.getFloatForUser(mContext.getContentResolver(),
                SettingsExt.System.LONG_BACK_SWIPE_THRESHOLD_PORT,
                DEFAULT_LONG_SWIPE_THRESHOLD_PORT, UserHandle.USER_CURRENT);
    }

    public float getLongSwipeLandscapeThreshold() {
        return Settings.System.getFloatForUser(mContext.getContentResolver(),
                SettingsExt.System.LONG_BACK_SWIPE_THRESHOLD_LAND,
                DEFAULT_LONG_SWIPE_THRESHOLD_LAND, UserHandle.USER_CURRENT);
    }
}
