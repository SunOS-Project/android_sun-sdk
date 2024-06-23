/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.oplus.os;

import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Slog;

public class WaveformEffect implements Parcelable {

    public static final int EFFECT_INVALID = -1;

    public static final int STRENGTH_DEFAULT = -1;
    public static final int STRENGTH_LIGHT = 0;
    public static final int STRENGTH_MEDIUM = 1;
    public static final int STRENGTH_STRONG = 2;

    /** @hide */
    public static final Parcelable.Creator<WaveformEffect> CREATOR =
            new Parcelable.Creator<WaveformEffect>() {
        @Override
        public WaveformEffect createFromParcel(Parcel in) {
            return new WaveformEffect(in);
        }

        @Override
        public WaveformEffect[] newArray(int size) {
            return new WaveformEffect[size];
        }
    };

    private boolean mAsynchronous;
    private boolean mEffectLoop;
    private int mEffectStrength;
    private int mEffectType;
    private boolean mStrengthSettingEnabled;
    private int mUsageHint;

    private WaveformEffect() {
        mAsynchronous = false;
        mEffectLoop = false;
        mEffectStrength = STRENGTH_DEFAULT;
        mEffectType = EFFECT_INVALID;
        mStrengthSettingEnabled = false;
        mUsageHint = 0;
    }

    private WaveformEffect(Parcel in) {
        mAsynchronous = in.readBoolean();
        mEffectLoop = in.readBoolean();
        mEffectStrength = in.readInt();
        mEffectType = in.readInt();
        mStrengthSettingEnabled = in.readBoolean();
        mUsageHint = in.readInt();
    }

    public boolean getAsynchronous() {
        return mAsynchronous;
    }

    public boolean getEffectLoop() {
        return mEffectLoop;
    }

    public int getEffectStrength() {
        return mEffectStrength;
    }

    public int getEffectType() {
        return mEffectType;
    }

    public boolean getStrengthSettingEnabled() {
        return mStrengthSettingEnabled;
    }

    public int getUsageHint() {
        return mUsageHint;
    }

    public static class Builder {
        private boolean mAsynchronous;
        private boolean mEffectLoop;
        private int mEffectStrength;
        private int mEffectType;
        private boolean mStrengthSettingEnabled;
        private int mUsageHint;

        public Builder() {
            mAsynchronous = false;
            mEffectLoop = false;
            mEffectStrength = STRENGTH_DEFAULT;
            mEffectType = EFFECT_INVALID;
            mStrengthSettingEnabled = false;
            mUsageHint = 0;
        }

        public Builder(WaveformEffect effect) {
            mAsynchronous = effect.getAsynchronous();
            mEffectLoop = effect.getEffectLoop();
            mEffectStrength = effect.getEffectStrength();
            mEffectType = effect.getEffectType();
            mStrengthSettingEnabled = effect.getStrengthSettingEnabled();
            mUsageHint = effect.getUsageHint();
        }

        public @NonNull WaveformEffect build() {
            WaveformEffect effect = new WaveformEffect();
            effect.mAsynchronous = mAsynchronous;
            effect.mEffectLoop = mEffectLoop;
            effect.mEffectStrength = mEffectStrength;
            effect.mEffectType = mEffectType;
            effect.mStrengthSettingEnabled = mStrengthSettingEnabled;
            effect.mUsageHint = mUsageHint;
            return effect;
        }

        public @NonNull Builder setAsynchronous(boolean async) {
            mAsynchronous = async;
            return this;
        }

        public @NonNull Builder setEffectLoop(boolean loop) {
            mEffectLoop = loop;
            return this;
        }

        public @NonNull Builder setEffectStrength(int strength) {
            mEffectStrength = strength;
            return this;
        }

        public @NonNull Builder setEffectType(int type) {
            mEffectType = type;
            return this;
        }

        public @NonNull Builder setStrengthSettingEnabled(boolean enabled) {
            mStrengthSettingEnabled = enabled;
            return this;
        }

        public @NonNull Builder setUsageHint(int usageHint) {
            mUsageHint = usageHint;
            return this;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeBoolean(mAsynchronous);
        dest.writeBoolean(mEffectLoop);
        dest.writeInt(mEffectStrength);
        dest.writeInt(mEffectType);
        dest.writeBoolean(mStrengthSettingEnabled);
        dest.writeInt(mUsageHint);
    }

    @Override
    public String toString() {
        return "{mAsynchronous=" + mAsynchronous
                + ", mEffectLoop=" + mEffectLoop
                + ", mEffectStrength=" + mEffectStrength
                + ", mEffectType=" + mEffectType
                + ", mStrengthSettingEnabled=" + mStrengthSettingEnabled
                + ", mUsageHint=" + mUsageHint
                + "}";
    }
}
