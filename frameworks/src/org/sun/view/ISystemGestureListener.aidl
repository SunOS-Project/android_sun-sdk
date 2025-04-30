/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.view;

import android.view.MotionEvent;

/** @hide */
interface ISystemGestureListener {

    const int GESTURE_WINDOW_MODE = 1;
    const int GESTURE_GAME_MODE = 2;
    const int GESTURE_LEFT_RIGHT = 3;

    oneway void onGestureCanceled(int gesture, in MotionEvent event);

    oneway void onGesturePreTrigger(int gesture, in MotionEvent event);

    boolean onGesturePreTriggerBefore(int gesture, in MotionEvent event);

    oneway void onGestureTriggered(int gesture, in MotionEvent event);
}
