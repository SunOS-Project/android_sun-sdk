/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.view;

import android.content.ComponentName;

import org.nameless.view.IAppFocusObserver;
import org.nameless.view.TopAppInfo;

/** @hide */
interface IAppFocusManagerService {

    /* Get current top app info (Fullscreen & Mini-window task considered) */
    TopAppInfo getTopAppInfo();

    /* Get current top app info (Fullscreen task only) */
    TopAppInfo getTopFullscreenAppInfo();

    /* Check if current has mini-window focus */
    boolean hasMiniWindowFocus();

    /* Register observer to observe app focus change */
    boolean registerAppFocusObserver(in IAppFocusObserver listener, in boolean observeActivity);

    /* Unegister observer to observe app focus change */
    boolean unregisterAppFocusObserver(in IAppFocusObserver listener);
}
