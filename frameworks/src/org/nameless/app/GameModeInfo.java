/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.app;

import android.os.Parcel;
import android.os.Parcelable;

/** @hide */
public class GameModeInfo implements Parcelable {

    public static final Parcelable.Creator<GameModeInfo> CREATOR =
            new Parcelable.Creator<GameModeInfo>() {
        @Override
        public GameModeInfo createFromParcel(Parcel in) {
            return new GameModeInfo(in);
        }

        @Override
        public GameModeInfo[] newArray(int size) {
            return new GameModeInfo[size];
        }
    };

    private boolean mInGame;
    private boolean mDisableAutoBrightness;
    private boolean mDisableHeadsUp;
    private boolean mStayAwake;
    private boolean mSuppressFullscreenIntent;

    private GameModeInfo() {
        mInGame = false;
        mDisableAutoBrightness = false;
        mDisableHeadsUp = false;
        mStayAwake = false;
        mSuppressFullscreenIntent = false;
    }

    private GameModeInfo(Parcel in) {
        mInGame = in.readBoolean();
        mDisableAutoBrightness = in.readBoolean();
        mDisableHeadsUp = in.readBoolean();
        mStayAwake = in.readBoolean();
        mSuppressFullscreenIntent = in.readBoolean();
    }

    public boolean isInGame() {
        return mInGame;
    }

    public boolean shouldDisableAutoBrightness() {
        return mInGame && mDisableAutoBrightness;
    }

    public boolean shouldDisableHeadsUp() {
        return mInGame && mDisableHeadsUp;
    }

    public boolean shouldStayAwake() {
        return mInGame && mStayAwake;
    }

    public boolean shouldSuppressFullscreenIntent() {
        return mInGame && mSuppressFullscreenIntent;
    }

    public static class Builder {
        private boolean mInGame;
        private boolean mDisableAutoBrightness;
        private boolean mDisableHeadsUp;
        private boolean mStayAwake;
        private boolean mSuppressFullscreenIntent;

        public Builder() {
            mInGame = false;
            mDisableAutoBrightness = false;
            mDisableHeadsUp = false;
            mStayAwake = false;
            mSuppressFullscreenIntent = false;
        }

        public Builder(GameModeInfo info) {
            mInGame = info.isInGame();
            mDisableAutoBrightness = info.shouldDisableAutoBrightness();
            mDisableHeadsUp = info.shouldDisableHeadsUp();
            mStayAwake = info.shouldStayAwake();
            mSuppressFullscreenIntent = info.shouldSuppressFullscreenIntent();
        }

        public GameModeInfo build() {
            GameModeInfo info = new GameModeInfo();
            info.mInGame = mInGame;
            info.mDisableAutoBrightness = mDisableAutoBrightness;
            info.mDisableHeadsUp = mDisableHeadsUp;
            info.mStayAwake = mStayAwake;
            info.mSuppressFullscreenIntent = mSuppressFullscreenIntent;
            return info;
        }

        public Builder setInGame(boolean inGame) {
            mInGame = inGame;
            return this;
        }

        public Builder setDisableAutoBrightness(boolean disableAutoBrightness) {
            mDisableAutoBrightness = disableAutoBrightness;
            return this;
        }

        public Builder setDisableHeadsUp(boolean disableHeadsUp) {
            mDisableHeadsUp = disableHeadsUp;
            return this;
        }

        public Builder setStayAwake(boolean stayAwake) {
            mStayAwake = stayAwake;
            return this;
        }

        public Builder setSuppressFullscreenIntent(boolean suppress) {
            mSuppressFullscreenIntent = suppress;
            return this;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeBoolean(mInGame);
        dest.writeBoolean(mDisableAutoBrightness);
        dest.writeBoolean(mDisableHeadsUp);
        dest.writeBoolean(mStayAwake);
        dest.writeBoolean(mSuppressFullscreenIntent);
    }

    @Override
    public String toString() {
        return "{mInGame=" + mInGame
                + ", mDisableAutoBrightness=" + mDisableAutoBrightness
                + ", mDisableHeadsUp=" + mDisableHeadsUp
                + ", mStayAwake=" + mStayAwake
                + ", mSuppressFullscreenIntent=" + mSuppressFullscreenIntent
                + "}";
    }
}
