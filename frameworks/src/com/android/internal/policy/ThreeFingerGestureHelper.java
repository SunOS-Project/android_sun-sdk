/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.internal.policy;

import android.content.Context;
import android.graphics.PointF;
import android.util.SparseArray;
import android.view.MotionEvent;

public class ThreeFingerGestureHelper {

    public static final long MAX_VALID_FINGER_DOWN_INTERVAL = 500L;
    public static final float MAX_DISTANCE = 500.0f;

    public static final int MESSAGE_TYPE_SHOW = 0;
    public static final int MESSAGE_TYPE_FINGER_UP = 1;
    public static final int MESSAGE_TYPE_FINGER_MOVE = 2;
    public static final int MESSAGE_TYPE_DISMISS = 3;

    private final Context mContext;

    public ThreeFingerGestureHelper(Context context) {
        mContext = context;
    }

    public static void getPoints(MotionEvent event, SparseArray<PointF> points) {
        if (points != null) {
            points.clear();
            for (int i = 0; i < event.getPointerCount(); i++) {
                points.put(event.getPointerId(i), new PointF(event.getX(i), event.getY(i)));
            }
        }
    }

    private static float getShorterDis(PointF p1, PointF p2, PointF p3) {
        if (p1 != null && p2 != null && p3 != null) {
            return Math.min(squaredHypot(p1, p2), squaredHypot(p1, p3));
        }
        return Float.MAX_VALUE;
    }

    public static float squaredHypot(PointF p1, PointF p2) {
        final float dx = p2.x - p1.x;
        final float dy = p2.y - p1.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    public static boolean validStartingPoints(SparseArray<PointF> points) {
        if (points == null || points.size() != 3) {
            return false;
        }
        final PointF p1 = points.valueAt(0);
        final PointF p2 = points.valueAt(1);
        final PointF p3 = points.valueAt(2);
        final float d1 = getShorterDis(p1, p2, p3);
        final float d2 = getShorterDis(p2, p1, p3);
        final float d3 = getShorterDis(p3, p1, p2);
        if (d1 >= MAX_DISTANCE || d2 >= MAX_DISTANCE || d3 >= MAX_DISTANCE) {
            return false;
        }
        return true;
    }

    public boolean checkThreeFingerGesture(MotionEvent event) {
        if (event.getActionMasked() != MotionEvent.ACTION_POINTER_DOWN) {
            return false;
        }
        if (event.getPointerCount() != 3) {
            return false;
        }
        if (event.getEventTime() - event.getDownTime() >= MAX_VALID_FINGER_DOWN_INTERVAL) {
            return false;
        }
        final SparseArray<PointF> points = new SparseArray();
        getPoints(event, points);
        return validStartingPoints(points);
    }
}
