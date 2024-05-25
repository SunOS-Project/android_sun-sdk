/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.app;

import android.os.Parcel;
import android.os.Parcelable;

import org.nameless.app.GameModeManager;

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
    private boolean mDanmakuNotification;
    private boolean mDisableAutoBrightness;
    private boolean mDisableHeadsUp;
    private boolean mDisableThreeFinger;
    private boolean mLockGesture;
    private boolean mLockStatusbar;
    private boolean mMuteNotification;
    private boolean mSuppressFullscreenIntent;
    private int mCallAction;

    private GameModeInfo() {
        mInGame = false;
        mDanmakuNotification = false;
        mDisableAutoBrightness = false;
        mDisableHeadsUp = false;
        mDisableThreeFinger = false;
        mLockGesture = false;
        mLockStatusbar = false;
        mMuteNotification = false;
        mSuppressFullscreenIntent = false;
        mCallAction = GameModeManager.IN_GAME_CALL_NO_ACTION;
    }

    private GameModeInfo(Parcel in) {
        mInGame = in.readBoolean();
        mDanmakuNotification = in.readBoolean();
        mDisableAutoBrightness = in.readBoolean();
        mDisableHeadsUp = in.readBoolean();
        mDisableThreeFinger = in.readBoolean();
        mLockGesture = in.readBoolean();
        mLockStatusbar = in.readBoolean();
        mMuteNotification = in.readBoolean();
        mSuppressFullscreenIntent = in.readBoolean();
        mCallAction = in.readInt();
    }

    public boolean isInGame() {
        return mInGame;
    }

    public boolean isDanmakuNotificationEnabled() {
        return mInGame && mDanmakuNotification;
    }

    public boolean shouldDisableAutoBrightness() {
        return mInGame && mDisableAutoBrightness;
    }

    public boolean shouldDisableHeadsUp() {
        return mInGame && mDisableHeadsUp;
    }

    public boolean shouldDisableThreeFingerGesture() {
        return mInGame && mDisableThreeFinger;
    }

    public boolean shouldLockGesture() {
        return mInGame && mLockGesture;
    }

    public boolean shouldLockStatusbar() {
        return mInGame && mLockStatusbar;
    }

    public boolean shouldMuteNotification() {
        return mInGame && mMuteNotification;
    }

    public boolean shouldSuppressFullscreenIntent() {
        return mInGame && mSuppressFullscreenIntent;
    }

    public int getCallAction() {
        if (!mInGame) {
            return GameModeManager.IN_GAME_CALL_NO_ACTION;
        }
        return mCallAction;
    }

    public static class Builder {
        private boolean mInGame;
        private boolean mDanmakuNotification;
        private boolean mDisableAutoBrightness;
        private boolean mDisableHeadsUp;
        private boolean mDisableThreeFinger;
        private boolean mLockGesture;
        private boolean mLockStatusbar;
        private boolean mMuteNotification;
        private boolean mSuppressFullscreenIntent;
        private int mCallAction;

        public Builder() {
            mInGame = false;
            mDanmakuNotification = false;
            mDisableAutoBrightness = false;
            mDisableHeadsUp = false;
            mDisableThreeFinger = false;
            mLockGesture = false;
            mLockStatusbar = false;
            mMuteNotification = false;
            mSuppressFullscreenIntent = false;
            mCallAction = GameModeManager.IN_GAME_CALL_NO_ACTION;
        }

        public Builder(GameModeInfo info) {
            mInGame = info.mInGame;
            mDanmakuNotification = info.mDanmakuNotification;
            mDisableAutoBrightness = info.mDisableAutoBrightness;
            mDisableHeadsUp = info.mDisableHeadsUp;
            mDisableThreeFinger = info.mDisableThreeFinger;
            mLockGesture = info.mLockGesture;
            mLockStatusbar = info.mLockStatusbar;
            mMuteNotification = info.mMuteNotification;
            mSuppressFullscreenIntent = info.mSuppressFullscreenIntent;
            mCallAction = info.mCallAction;
        }

        public GameModeInfo build() {
            GameModeInfo info = new GameModeInfo();
            info.mInGame = mInGame;
            info.mDanmakuNotification = mDanmakuNotification;
            info.mDisableAutoBrightness = mDisableAutoBrightness;
            info.mDisableHeadsUp = mDisableHeadsUp;
            info.mDisableThreeFinger = mDisableThreeFinger;
            info.mLockGesture = mLockGesture;
            info.mLockStatusbar = mLockStatusbar;
            info.mMuteNotification = mMuteNotification;
            info.mSuppressFullscreenIntent = mSuppressFullscreenIntent;
            info.mCallAction = mCallAction;
            return info;
        }

        public Builder setInGame(boolean inGame) {
            mInGame = inGame;
            return this;
        }

        public Builder setDanmakuNotificationEnabled(boolean enabled) {
            mDanmakuNotification = enabled;
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

        public Builder setDisableThreeFingerGesture(boolean disableThreeFinger) {
            mDisableThreeFinger = disableThreeFinger;
            return this;
        }

        public Builder setLockGesture(boolean lockGesture) {
            mLockGesture = lockGesture;
            return this;
        }

        public Builder setLockStatusbar(boolean lockStatusbar) {
            mLockStatusbar = lockStatusbar;
            return this;
        }

        public Builder setMuteNotification(boolean muteNotification) {
            mMuteNotification = muteNotification;
            return this;
        }

        public Builder setSuppressFullscreenIntent(boolean suppress) {
            mSuppressFullscreenIntent = suppress;
            return this;
        }

        public Builder setCallAction(int action) {
            mCallAction = action;
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
        dest.writeBoolean(mDanmakuNotification);
        dest.writeBoolean(mDisableAutoBrightness);
        dest.writeBoolean(mDisableHeadsUp);
        dest.writeBoolean(mDisableThreeFinger);
        dest.writeBoolean(mLockGesture);
        dest.writeBoolean(mLockStatusbar);
        dest.writeBoolean(mMuteNotification);
        dest.writeBoolean(mSuppressFullscreenIntent);
        dest.writeInt(mCallAction);
    }

    @Override
    public String toString() {
        return "{mInGame=" + mInGame
                + ", mDanmakuNotification=" + mDanmakuNotification
                + ", mDisableAutoBrightness=" + mDisableAutoBrightness
                + ", mDisableHeadsUp=" + mDisableHeadsUp
                + ", mDisableThreeFinger=" + mDisableThreeFinger
                + ", mLockGesture=" + mLockGesture
                + ", mLockStatusbar=" + mLockStatusbar
                + ", mMuteNotification=" + mMuteNotification
                + ", mSuppressFullscreenIntent=" + mSuppressFullscreenIntent
                + ", mCallAction=" + GameModeManager.callActionToString(mCallAction)
                + "}";
    }
}
