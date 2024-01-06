/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.view;

import android.content.ComponentName;

import org.nameless.view.IAppFocusObserver;

/** @hide */
interface IAppFocusManagerService {

    /* Get current top fullscreen component */
    ComponentName getTopFullscreenComponent();

    /* Register observer to observe app focus change */
    boolean registerAppFocusObserver(in IAppFocusObserver listener, in boolean observeActivity);

    /* Unegister observer to observe app focus change */
    boolean unregisterAppFocusObserver(in IAppFocusObserver listener);
}
