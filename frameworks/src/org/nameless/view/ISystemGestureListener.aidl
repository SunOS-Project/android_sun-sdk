/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.view;

import android.view.MotionEvent;

/** @hide */
interface ISystemGestureListener {

    oneway void onGestureCanceled(int gesture);

    oneway void onGesturePreTrigger(int gesture, in MotionEvent event);

    boolean onGesturePreTriggerBefore(int gesture, in MotionEvent event);

    oneway void onGestureTriggered(int gesture, in MotionEvent event);
}
