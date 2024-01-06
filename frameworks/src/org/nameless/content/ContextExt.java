/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.content;

/** @hide */
public class ContextExt {

    private ContextExt() {}

    /**
     * Use with {@link #getSystemService} to retrieve a
     * {@link org.nameless.view.AppFocusManager} for managing top app focus.
     *
     * @hide
     * @see #getSystemService
     * @see org.nameless.view.AppFocusManager
     */
    public static final String APP_FOCUS_MANAGER_SERVICE = "app_focus";
}
