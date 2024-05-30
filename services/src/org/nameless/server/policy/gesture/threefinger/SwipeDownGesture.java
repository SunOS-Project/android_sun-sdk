/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.server.policy.gesture.threefinger;

import static org.nameless.os.DebugConstants.DEBUG_THREE_FINGER;

import android.content.Context;
import android.graphics.PointF;
import android.os.Handler;
import android.util.Slog;
import android.util.SparseArray;
import android.view.MotionEvent;

import com.android.internal.policy.ThreeFingerGestureHelper;

import com.android.server.policy.PhoneWindowManagerExt;

public class SwipeDownGesture extends BaseThreeFingerGesture {

    private static final String TAG = "ThreeFinger::SwipeDown";

    private static final float MIN_WIDTH_PORTRAIT = 0.14f;
    private static final float MIN_WIDTH_LANDSCAPE = 0.14f;

    public SwipeDownGesture(Context context, Handler handler) {
        super(context, handler);
    }

    @Override
    protected String getTag() {
        return TAG;
    }

    private boolean checkPointsDistance(SparseArray<PointF> points) {
        final float width;
        final int minDis;
        if (isPortrait()) {
            width = (float) mScreenSize.getHeight();
            minDis = (int) (width * MIN_WIDTH_PORTRAIT);
        } else {
            width = (float) mScreenSize.getWidth();
            minDis = (int) (width * MIN_WIDTH_LANDSCAPE);
        }

        for (int i = 0; i < points.size(); i++) {
            if (points.valueAt(i).y - mPoints.valueAt(i).y < minDis) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected boolean handleFingerUpEvent(MotionEvent event) {
        if (!mHandleNeeded) {
            return false;
        }

        final SparseArray<PointF> points = new SparseArray<>();
        ThreeFingerGestureHelper.getPoints(event, points);

        if (checkPointsDistance(points)) {
            if (DEBUG_THREE_FINGER) {
                Slog.d(TAG, "take screenshot by three finger");
            }
            PhoneWindowManagerExt.getInstance().takeScreenshotIfSetupCompleted(true);
            return true;
        }

        if (DEBUG_THREE_FINGER) {
            Slog.d(TAG, "under threshold: portrait= " + isPortrait()
                    + ", duration= " + (event.getEventTime() - event.getDownTime())
                    + ", delta= [" + pointsToString(points) + "]");
        }
        return false;
    }

    private String pointsToString(SparseArray<PointF> points) {
        final float width;
        if (isPortrait()) {
            width = (float) mScreenSize.getHeight();
        } else {
            width = (float) mScreenSize.getWidth();
        }

        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < points.size(); i++) {
            final float disY = points.valueAt(i).y - mPoints.valueAt(i).y;
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(disY + "->" + (disY / width));
        }
        return sb.toString();
    }
}
