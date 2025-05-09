/*
 * Copyright (C) 2022 The Kaleidoscope Open Source Project
 * Copyright (C) 2023 The Nameless-AOSP Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.media;

import android.annotation.NonNull;

/** @hide */
public class AppVolume {
    private final String mPackageName;
    private final int mUid;
    private final float mVolume;
    private final boolean mMute;
    private final boolean mActive;

    AppVolume(String packageName, int uid, float volume, boolean mute, boolean active) {
        mPackageName = packageName;
        mUid = uid;
        mVolume = volume;
        mMute = mute;
        mActive = active;
    }

    @NonNull
    public String getPackageName() {
        return mPackageName;
    }

    public int getUid() {
        return mUid;
    }

    public float getVolume() {
        return mVolume;
    }

    public boolean isMuted() {
        return mMute;
    }

    public boolean isActive() {
        return mActive;
    }
}
