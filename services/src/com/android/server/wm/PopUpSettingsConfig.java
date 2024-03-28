/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.server.wm;

import static org.nameless.provider.SettingsExt.System.POP_UP_DOUBLE_TAP_ACTION;
import static org.nameless.provider.SettingsExt.System.POP_UP_KEEP_MUTE_IN_MINI;
import static org.nameless.provider.SettingsExt.System.POP_UP_NOTIFICATION_BLACKLIST;
import static org.nameless.provider.SettingsExt.System.POP_UP_SINGLE_TAP_ACTION;
import static org.nameless.view.PopUpViewManager.TAP_ACTION_EXIT;
import static org.nameless.view.PopUpViewManager.TAP_ACTION_NOTHING;
import static org.nameless.view.PopUpViewManager.TAP_ACTION_PIN_WINDOW;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;

import com.android.internal.util.nameless.PopUpSettingsHelper;

import java.util.HashSet;

import org.nameless.view.PopUpViewManager;

class PopUpSettingsConfig {

    private static class InstanceHolder {
        private static final PopUpSettingsConfig INSTANCE = new PopUpSettingsConfig();
    }

    static PopUpSettingsConfig getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private final HashSet<String> mUserNotificationBlacklist = new HashSet<>();

    private Context mContext;
    private Handler mHandler;
    private SettingsObserver mObserver;

    private boolean mKeepMuteInMini = true;
    private int mSingleTapAction = TAP_ACTION_PIN_WINDOW;
    private int mDoubleTapAction = TAP_ACTION_EXIT;

    void init(Context context, Handler handler) {
        mContext = context;
        mHandler = handler;
        mObserver = new SettingsObserver(handler);
        mObserver.observe();
        updateAll();
    }

    private void updatePopUpKeepMuteInMini() {
        mKeepMuteInMini = PopUpSettingsHelper.isKeepMuteInMiniEnabled(mContext);
    }

    boolean shouldMuteInMiniWindow() {
        return mKeepMuteInMini;
    }

    private void updatePopUpSingleTapAction() {
        mSingleTapAction = PopUpSettingsHelper.getSingleTapAction(mContext);
        DimmerWindow.getInstance().setSingleTapOnly(
                mDoubleTapAction == TAP_ACTION_NOTHING || mSingleTapAction == mDoubleTapAction);
    }

    int getSingleTapAction() {
        return mSingleTapAction;
    }

    private void updatePopUpDoubleTapAction() {
        mDoubleTapAction = PopUpSettingsHelper.getDoubleTapAction(mContext);
        DimmerWindow.getInstance().setSingleTapOnly(
                mDoubleTapAction == TAP_ACTION_NOTHING || mSingleTapAction == mDoubleTapAction);
    }

    int getDoubleTapAction() {
        return mDoubleTapAction;
    }

    private void updateNotificationBlacklist() {
        mUserNotificationBlacklist.clear();
        final String blacklist = PopUpSettingsHelper.getNotificationJumpBlacklist(mContext);
        if (TextUtils.isEmpty(blacklist)) {
            return;
        }
        final String[] apps = blacklist.split(";");
        for (String app : apps) {
            mUserNotificationBlacklist.add(app);
        }
    }

    boolean inNotificationBlacklist(String packageName) {
        return PopUpViewManager.inSystemNotificationBlacklist(packageName) ||
                mUserNotificationBlacklist.contains(packageName);
    }

    void updateAll() {
        mHandler.post(() -> {
            updatePopUpKeepMuteInMini();
            updatePopUpSingleTapAction();
            updatePopUpDoubleTapAction();
            updateNotificationBlacklist();
        });
    }

    private final class SettingsObserver extends ContentObserver {

        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            final ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(
                    Settings.System.getUriFor(POP_UP_KEEP_MUTE_IN_MINI),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(POP_UP_SINGLE_TAP_ACTION),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(POP_UP_DOUBLE_TAP_ACTION),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(POP_UP_NOTIFICATION_BLACKLIST),
                    false, this, UserHandle.USER_ALL);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            switch (uri.getLastPathSegment()) {
                case POP_UP_KEEP_MUTE_IN_MINI:
                    updatePopUpKeepMuteInMini();
                    break;
                case POP_UP_SINGLE_TAP_ACTION:
                    updatePopUpSingleTapAction();
                    break;
                case POP_UP_DOUBLE_TAP_ACTION:
                    updatePopUpDoubleTapAction();
                    break;
                case POP_UP_NOTIFICATION_BLACKLIST:
                    updateNotificationBlacklist();
                    break;
            }
        }
    }
}
