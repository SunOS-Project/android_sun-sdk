/*
 * Copyright (C) 2021 The Android AAC vibraiton extension
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

package android.os;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

/**
 * A DynamicEffect describes a haptic effect to tencent he perform {@link Vibrator}.
 */
public final class DynamicEffect extends VibrationEffect implements Parcelable {

    private static final String TAG = "DynamicEffect";

    public static final boolean DEBUG = false;

    private final String mPatternJson;

    /** @hide */
    public DynamicEffect(@NonNull Parcel in) {
        mPatternJson = null;
    }

    public DynamicEffect(@NonNull String patternJson) {
        mPatternJson = new String(patternJson);
    }

    public @NonNull String getPatternInfo() {
        return mPatternJson;
    }

    @Nullable
    public static DynamicEffect create(@Nullable String json) {
        if (TextUtils.isEmpty(json)){
            Log.e(TAG, "empty pattern, do nothing");
            return null;
        }
        return new DynamicEffect(json);
    }

    @Override
    public long[] computeCreateWaveformOffOnTimingsOrNull() {
        return null;
    }

    @Override
    public VibrationEffect cropToLengthOrNull(int length) {
        return null;
    }

    /** @hide */
    @Override
    public boolean areVibrationFeaturesSupported(@NonNull VibratorInfo vibratorInfo) {
        return false;
    }

    @Override
    public VibrationEffect applyRepeatingIndefinitely(boolean wantRepeating, int loopDelayMs) {
        return null;
    }

    @Override
    public String toDebugString() {
        return toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /** @hide */
    @Override
    public DynamicEffect resolve(int defaultAmplitude) {
        return this;
    }

    /** @hide */
    @Override
    public DynamicEffect scale(float scaleFactor) {
        return this;
    }

    @Override
    public void validate() {
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof String)) {
            return false;
        }
        final String other = (String) o;
        return (mPatternJson == other);
    }

    @Override
    public int hashCode() {
        int result = 17;
        if (mPatternJson != null) {
            result += 37 * (int) mPatternJson.hashCode();
        }
        return result;
    }

    @Override
    public long getDuration() {
        return 0;
    }

    @Override
    public String toString() {
        return "DynamicEffect{mPatternJson=" +  mPatternJson + "}";
    }

    @Override
    public void writeToParcel(@NonNull Parcel out, int flags) {
    }

    public static final @NonNull Parcelable.Creator<DynamicEffect> CREATOR =
        new Parcelable.Creator<DynamicEffect>() {
            @Override
            public DynamicEffect createFromParcel(@NonNull Parcel in) {
                // Skip the type token
                in.readInt();
                return new DynamicEffect(in);
            }
            @Override
            public @NonNull DynamicEffect[] newArray(int size) {
                return new DynamicEffect[size];
            }
        };

}
