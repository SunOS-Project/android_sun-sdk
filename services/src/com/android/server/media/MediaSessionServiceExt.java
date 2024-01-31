/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.server.media;

import static org.nameless.provider.SettingsExt.System.ADAPTIVE_PLAYBACK_ENABLED;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;

class MediaSessionServiceExt {

    private static class InstanceHolder {
        private static final MediaSessionServiceExt INSTANCE = new MediaSessionServiceExt();
    }

    static MediaSessionServiceExt getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private ContentResolver mResolver;
    private Handler mHandler;

    private boolean mAdaptivePlaybackEnabled;

    void init(Context context, Handler handler) {
        mResolver = context.getContentResolver();
        mHandler = handler;
    }

    void onBootCompleted() {
        final ContentObserver settingsObserver = new ContentObserver(mHandler) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                switch (uri.getLastPathSegment()) {
                    case ADAPTIVE_PLAYBACK_ENABLED:
                        updateAdaptivePlayback();
                        break;
                }
            }
        };

        mResolver.registerContentObserver(
                Settings.System.getUriFor(ADAPTIVE_PLAYBACK_ENABLED),
                false, settingsObserver);
        updateSettings();
    }

    boolean isAdaptivePlaybackEnabled() {
        return mAdaptivePlaybackEnabled;
    }

    private void updateSettings() {
        updateAdaptivePlayback();
    }

    private void updateAdaptivePlayback() {
        mAdaptivePlaybackEnabled = Settings.System.getIntForUser(
                mResolver, ADAPTIVE_PLAYBACK_ENABLED,
                0, UserHandle.USER_CURRENT) == 1;
    }
}
