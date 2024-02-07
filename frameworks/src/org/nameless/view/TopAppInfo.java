/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.view;

import static android.app.ActivityTaskManager.INVALID_TASK_ID;
import static android.app.WindowConfiguration.WINDOWING_MODE_UNDEFINED;

import android.content.ComponentName;
import android.os.Parcel;
import android.os.Parcelable;

/** @hide */
public class TopAppInfo implements Parcelable {

    public static final Parcelable.Creator<TopAppInfo> CREATOR =
            new Parcelable.Creator<TopAppInfo>() {
        @Override
        public TopAppInfo createFromParcel(Parcel in) {
            return new TopAppInfo(in);
        }

        @Override
        public TopAppInfo[] newArray(int size) {
            return new TopAppInfo[size];
        }
    };

    private ComponentName mComponentName;
    private int mTaskId;
    private int mWindowingMode;

    private TopAppInfo() {
        mComponentName = null;
        mTaskId = INVALID_TASK_ID;
        mWindowingMode = WINDOWING_MODE_UNDEFINED;
    }

    private TopAppInfo(Parcel in) {
        mComponentName = in.readTypedObject(ComponentName.CREATOR);
        mTaskId = in.readInt();
        mWindowingMode = in.readInt();
    }

    public ComponentName getComponentName() {
        return mComponentName;
    }

    public String getPackageName() {
        if (mComponentName == null) {
            return null;
        }
        return mComponentName.getPackageName();
    }

    public String getActivityName() {
        if (mComponentName == null) {
            return null;
        }
        return mComponentName.getClassName();
    }

    public int getTaskId() {
        return mTaskId;
    }

    public int getWindowingMode() {
        return mWindowingMode;
    }

    public static class Builder {
        private ComponentName mComponentName;
        private int mTaskId;
        private int mWindowingMode;

        public Builder() {
            mComponentName = null;
            mTaskId = INVALID_TASK_ID;
            mWindowingMode = WINDOWING_MODE_UNDEFINED;
        }

        public Builder(TopAppInfo info) {
            mComponentName = info.getComponentName();
            mTaskId = info.getTaskId();
            mWindowingMode = info.getWindowingMode();
        }

        public TopAppInfo build() {
            TopAppInfo info = new TopAppInfo();
            info.mComponentName = mComponentName;
            info.mTaskId = mTaskId;
            info.mWindowingMode = mWindowingMode;
            return info;
        }

        public Builder setComponentName(ComponentName componentName) {
            mComponentName = componentName;
            return this;
        }

        public Builder setTaskId(int taskId) {
            mTaskId = taskId;
            return this;
        }

        public Builder setWindowingMode(int windowingMode) {
            mWindowingMode = windowingMode;
            return this;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedObject(mComponentName, flags);
        dest.writeInt(mTaskId);
        dest.writeInt(mWindowingMode);
    }

    @Override
    public String toString() {
        return "{mComponentName=" + (mComponentName != null ? mComponentName : "null")
                + ", mTaskId=" + mTaskId
                + ", mWindowingMode=" + mWindowingMode
                + "}";
    }
}
