/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.server.policy.gesture;

import android.view.MotionEvent;

interface IGestureListener {

    boolean interceptMotionBeforeQueueing(MotionEvent motionEvent);
}
