/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.systemui.volume;

import static org.nameless.provider.SettingsExt.System.PERSISTED_APP_VOLUME_DATA;

import android.content.ContentResolver;
import android.content.Context;
import android.media.AppVolume;
import android.media.AudioManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import java.util.HashMap;

public class AppVolumePersistHelper {

    private static final String TAG = "AppVolumePersistHelper";
    private static final boolean DEBUG = false;

    private final HashMap<AppInfo, Integer> mAppVolumeMap = new HashMap<>();

    private final ContentResolver mResolver;

    public AppVolumePersistHelper(Context context) {
        mResolver = context.getContentResolver();
        initAppVolumeMap();
    }

    private void initAppVolumeMap() {
        if (DEBUG) {
            Log.d(TAG, "initAppVolumeMap");
        }
        final String persistedData = Settings.System.getStringForUser(mResolver,
                PERSISTED_APP_VOLUME_DATA, UserHandle.USER_SYSTEM);
        if (TextUtils.isEmpty(persistedData)) {
            if (DEBUG) {
                Log.d(TAG, "Persisted data is empty. Skip initAppVolumeMap");
            }
            return;
        }
        final String[] datas = persistedData.split(";");
        for (String data : datas) {
            final String[] volumeInfo = data.split(",");
            final String packageName = volumeInfo[0];
            final int uid = Integer.parseInt(volumeInfo[1]);
            final int volume = Integer.parseInt(volumeInfo[2]);
            final AppInfo ai = new AppInfo(packageName, uid);
            mAppVolumeMap.put(ai, volume);
            if (DEBUG) {
                Log.d(TAG, "Added " + ai + ", volume: " + volume);
            }
        }
    }

    public void persistAppVolume(AppVolume av, float newVolume) {
        if (DEBUG) {
            Log.d(TAG, "persistAppVolume, " + appVolumeToString(av, newVolume));
        }
        final AppInfo ai = new AppInfo(av.getPackageName(), av.getUid());
        final int volume = (int) (newVolume * 100);
        if (mAppVolumeMap.getOrDefault(ai, 100) == volume) {
            if (DEBUG) {
                Log.d(TAG, "Skip persist data due to same volume");
            }
            return;
        }
        mAppVolumeMap.put(ai, volume);
        Settings.System.putStringForUser(mResolver, PERSISTED_APP_VOLUME_DATA,
                generateSettingsString(), UserHandle.USER_SYSTEM);
    }

    public void updateAllVolume(AudioManager audioManager, boolean applyAppVolume) {
        if (DEBUG) {
            Log.d(TAG, "updateAllVolume, applyAppVolume: " + applyAppVolume);
        }
        for (AppInfo ai : mAppVolumeMap.keySet()) {
            audioManager.setAppVolume(ai.packageName, ai.uid,
                    applyAppVolume ? mAppVolumeMap.get(ai) / 100.0f : 1f);
        }
    }

    private String generateSettingsString() {
        StringBuilder sb = new StringBuilder();
        for (AppInfo ai : mAppVolumeMap.keySet()) {
            sb.append(ai.packageName).append(",");
            sb.append(ai.uid).append(",");
            sb.append(mAppVolumeMap.get(ai)).append(";");
        }
        return sb.toString();
    }

    private String appVolumeToString(AppVolume av, float newVolume) {
        StringBuilder sb = new StringBuilder();
        sb.append(av.getPackageName()).append(",");
        sb.append(av.getUid()).append(",");
        sb.append((int) (newVolume * 100));
        return sb.toString();
    }

    private final class AppInfo {
        public String packageName;
        public int uid;

        public AppInfo(String packageName, int uid) {
            this.packageName = packageName;
            this.uid = uid;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof AppInfo) {
                final AppInfo other = (AppInfo) obj;
                return packageName.equals(other.packageName) && uid == other.uid;
            }
            return false;
        }

        @Override
        public int hashCode() {
            int result = packageName.hashCode();
            result = 31 * result + uid;
            return result;
        }

        @Override
        public String toString() {
            return "packageName: " + packageName + ", uid: " + uid;
        }
    }
}
