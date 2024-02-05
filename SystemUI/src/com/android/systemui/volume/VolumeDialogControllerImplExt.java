/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.volume;

import static org.nameless.provider.SettingsExt.System.ADAPTIVE_PLAYBACK_ENABLED;
import static org.nameless.provider.SettingsExt.System.ADAPTIVE_PLAYBACK_TIMEOUT;
import static org.nameless.provider.SettingsExt.System.VOLUME_PANEL_POSITION_LAND;
import static org.nameless.provider.SettingsExt.System.VOLUME_PANEL_POSITION_PORT;
import static org.nameless.provider.SettingsExt.System.VOLUME_PANEL_SHOW_APP_VOLUME;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.media.AppVolume;
import android.media.AudioManager;
import android.media.AudioSystem;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.KeyEvent;

import com.android.systemui.settings.UserTracker;

import org.nameless.systemui.volume.AppVolumePersistHelper;

class VolumeDialogControllerImplExt {

    private static class InstanceHolder {
        private static VolumeDialogControllerImplExt INSTANCE = new VolumeDialogControllerImplExt();
    }

    static VolumeDialogControllerImplExt getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private static final Object ADAPTIVE_PLAYBACK_TOKEN = new Object();

    private static final int POSITION_LEFT = 0;
    private static final int POSITION_RIGHT = 1;

    private AudioManager mAudioManager;
    private ContentResolver mResolver;
    private Context mContext;
    private Handler mHandler;
    private UserTracker mUserTracker;
    private VolumeDialogControllerImpl mImpl;

    private boolean mAdaptivePlaybackEnabled;
    private int mAdaptivePlaybackTimeout;
    private boolean mAdaptivePlaybackResumable;

    private boolean mVolumePanelPortLeft;
    private boolean mVolumePanelLandLeft;

    private AppVolumePersistHelper mAppVolumePersistHelper;
    private boolean mVolumePanelShowAppVolume;

    void init(VolumeDialogControllerImpl impl, Context context,
            Handler handler, AudioManager audioManager, UserTracker userTracker) {
        mImpl = impl;
        mAudioManager = audioManager;
        mContext = context;
        mHandler = handler;
        mUserTracker = userTracker;
        mResolver = context.getContentResolver();

        mAppVolumePersistHelper = new AppVolumePersistHelper(context);

        mHandler.post(() -> {
            updateSettings();
        });
    }

    void onUserChanged() {
        updateSettings();
    }

    void onRegisterSettings(ContentObserver observer) {
        mResolver.registerContentObserver(
                Settings.System.getUriFor(ADAPTIVE_PLAYBACK_ENABLED),
                false, observer, UserHandle.USER_ALL);
        mResolver.registerContentObserver(
                Settings.System.getUriFor(ADAPTIVE_PLAYBACK_TIMEOUT),
                false, observer, UserHandle.USER_ALL);
        mResolver.registerContentObserver(
                Settings.System.getUriFor(VOLUME_PANEL_POSITION_PORT),
                false, observer, UserHandle.USER_ALL);
        mResolver.registerContentObserver(
                Settings.System.getUriFor(VOLUME_PANEL_POSITION_LAND),
                false, observer, UserHandle.USER_ALL);
        mResolver.registerContentObserver(
                Settings.System.getUriFor(VOLUME_PANEL_SHOW_APP_VOLUME),
                false, observer, UserHandle.USER_ALL);
    }

    boolean onSettingsChanged(Uri uri) {
        switch (uri.getLastPathSegment()) {
            case ADAPTIVE_PLAYBACK_ENABLED:
            case ADAPTIVE_PLAYBACK_TIMEOUT:
                updateAdaptivePlayback();
                return false;
            case VOLUME_PANEL_POSITION_PORT:
            case VOLUME_PANEL_POSITION_LAND:
                updateVolumePanelPosition(true);
                return false;
            case VOLUME_PANEL_SHOW_APP_VOLUME:
                updateVolumePanelShowAppVolume(true);
                return false;
            default:
                return false;
        }
    }

    void onUpdateStreamLevel(int stream, int level) {
        if (mAdaptivePlaybackEnabled && stream == AudioSystem.STREAM_MUSIC
                && level == 0 && mAudioManager.isMusicActive()) {
            mAudioManager.dispatchMediaKeyEvent(
                    new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE));
            mAudioManager.dispatchMediaKeyEvent(
                    new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE));
            mAdaptivePlaybackResumable = true;
            mHandler.removeCallbacksAndMessages(ADAPTIVE_PLAYBACK_TOKEN);
            if (mAdaptivePlaybackTimeout > 0) {
                mHandler.postDelayed(() -> {
                    mAdaptivePlaybackResumable = false;
                }, ADAPTIVE_PLAYBACK_TOKEN, mAdaptivePlaybackTimeout);
            }
        } else if (stream == AudioSystem.STREAM_MUSIC && level > 0
                && mAdaptivePlaybackResumable) {
            mHandler.removeCallbacksAndMessages(ADAPTIVE_PLAYBACK_TOKEN);
            mAudioManager.dispatchMediaKeyEvent(
                    new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY));
            mAudioManager.dispatchMediaKeyEvent(
                    new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY));
            mAdaptivePlaybackResumable = false;
        }
    }

    boolean isVolumePanelOnLeft() {
        return isLandscape() ? mVolumePanelLandLeft : mVolumePanelPortLeft;
    }

    boolean isAppVolumeVisible() {
        return mVolumePanelShowAppVolume;
    }

    void persistAppVolume(AppVolume appVolume, float volume) {
        mAppVolumePersistHelper.persistAppVolume(appVolume, volume);
    }

    private boolean isLandscape() {
        return mContext.getResources().getConfiguration().orientation ==
                Configuration.ORIENTATION_LANDSCAPE;
    }

    private void updateSettings() {
        updateAdaptivePlayback();
        updateVolumePanelPosition(false);
        updateVolumePanelShowAppVolume(false);
        notifyChange();
    }

    private void updateAdaptivePlayback() {
        mAdaptivePlaybackEnabled = Settings.System.getIntForUser(
                mResolver, ADAPTIVE_PLAYBACK_ENABLED,
                0, mUserTracker.getUserId()) == 1;
        mAdaptivePlaybackTimeout = Settings.System.getIntForUser(
                mResolver, ADAPTIVE_PLAYBACK_TIMEOUT,
                30000, mUserTracker.getUserId());
    }

    private void updateVolumePanelPosition(boolean notify) {
        mVolumePanelPortLeft = Settings.System.getIntForUser(
                mResolver, VOLUME_PANEL_POSITION_PORT,
                POSITION_LEFT, mUserTracker.getUserId()) == POSITION_LEFT;
        mVolumePanelLandLeft = Settings.System.getIntForUser(
                mResolver, VOLUME_PANEL_POSITION_LAND,
                POSITION_RIGHT, mUserTracker.getUserId()) == POSITION_LEFT;
        if (notify) {
            notifyChange();
        }
    }

    private void updateVolumePanelShowAppVolume(boolean notify) {
        mVolumePanelShowAppVolume = Settings.System.getIntForUser(
                mResolver, VOLUME_PANEL_SHOW_APP_VOLUME,
                1, mUserTracker.getUserId()) == 1;
        mAppVolumePersistHelper.updateAllVolume(mAudioManager, mVolumePanelShowAppVolume);
        if (notify) {
            notifyChange();
        }
    }

    private void notifyChange() {
        mImpl.mCallbacks.onConfigurationChanged();
    }
}
