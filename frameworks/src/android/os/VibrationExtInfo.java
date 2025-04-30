/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package android.os;

import android.annotation.NonNull;
import android.annotation.Nullable;

import vendor.sun.hardware.vibratorExt.Effect;

public class VibrationExtInfo implements Parcelable {

    public static final int CLICK = Effect.CLICK;
    // For SettingsLib preferences as they can't access hidden api
    public static final int SWITCH_TOGGLE = Effect.SWITCH_TOGGLE;

    public static final Parcelable.Creator<VibrationExtInfo> CREATOR =
            new Parcelable.Creator<VibrationExtInfo>() {
        @Override
        public VibrationExtInfo createFromParcel(Parcel in) {
            return new VibrationExtInfo(in);
        }

        @Override
        public VibrationExtInfo[] newArray(int size) {
            return new VibrationExtInfo[size];
        }
    };

    private int mEffectId;
    private int mFallbackEffectId;
    private float mAmplitude;
    private String mReason;
    private VibrationAttributes mAttrs;

    private VibrationExtInfo() {
        mEffectId = CLICK;
        mFallbackEffectId = -1;
        mAmplitude = 1.0f;
        mReason = null;
        mAttrs = null;
    }

    private VibrationExtInfo(@NonNull Parcel in) {
        mEffectId = in.readInt();
        mFallbackEffectId = in.readInt();
        mAmplitude = in.readFloat();
        mReason = in.readString();
        mAttrs = in.readTypedObject(VibrationAttributes.CREATOR);
    }

    public int getEffectId() {
        return mEffectId;
    }

    public int getFallbackEffectId() {
        return mFallbackEffectId;
    }

    public float getAmplitude() {
        return mAmplitude;
    }

    public @Nullable String getReason() {
        return mReason;
    }

    public @Nullable VibrationAttributes getVibrationAttributes() {
        return mAttrs;
    }

    public void setEffectId(int effectId) {
        mEffectId = effectId;
    }

    public void setFallbackEffectId(int fallbackEffectId) {
        mFallbackEffectId = fallbackEffectId;
    }

    public void setAmplitude(float amplitude) {
        mAmplitude = amplitude;
    }

    public void setReason(@NonNull String reason) {
        mReason = reason;
    }

    public void setVibrationAttributes(@Nullable VibrationAttributes attrs) {
        mAttrs = attrs;
    }

    public static class Builder {
        private int mEffectId;
        private int mFallbackEffectId;
        private float mAmplitude;
        private String mReason;
        private VibrationAttributes mAttrs;

        public Builder() {
            mEffectId = CLICK;
            mFallbackEffectId = -1;
            mAmplitude = 1.0f;
            mReason = null;
            mAttrs = null;
        }

        public Builder(@NonNull VibrationExtInfo info) {
            mEffectId = info.getEffectId();
            mFallbackEffectId = info.getFallbackEffectId();
            mAmplitude = info.getAmplitude();
            mReason = info.getReason();
            mAttrs = info.getVibrationAttributes();
        }

        public @NonNull VibrationExtInfo build() {
            VibrationExtInfo info = new VibrationExtInfo();
            info.mEffectId = mEffectId;
            info.mFallbackEffectId = mFallbackEffectId;
            info.mAmplitude = mAmplitude;
            info.mReason = mReason;
            info.mAttrs = mAttrs;
            return info;
        }

        public @NonNull Builder setEffectId(int effectId) {
            mEffectId = effectId;
            return this;
        }

        public @NonNull Builder setFallbackEffectId(int fallbackEffectId) {
            mFallbackEffectId = fallbackEffectId;
            return this;
        }

        public @NonNull Builder setAmplitude(float amplitude) {
            mAmplitude = amplitude;
            return this;
        }

        public @NonNull Builder setReason(String reason) {
            mReason = reason;
            return this;
        }

        public @NonNull Builder setVibrationAttributes(VibrationAttributes attrs) {
            mAttrs = attrs;
            return this;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mEffectId);
        dest.writeInt(mFallbackEffectId);
        dest.writeFloat(mAmplitude);
        dest.writeString(mReason);
        dest.writeTypedObject(mAttrs, flags);
    }

    @Override
    public String toString() {
        return "{mEffectId=" + mEffectId
                + ", mFallbackEffectId=" + mFallbackEffectId
                + ", mAmplitude=" + mAmplitude
                + ", mReason=" + (mReason != null ? mReason : "null")
                + ", mAttrs=" + (mAttrs != null ? mAttrs : "null")
                + "}";
    }
}
