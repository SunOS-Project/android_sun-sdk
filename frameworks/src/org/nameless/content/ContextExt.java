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

    /**
     * Use with {@link #getSystemService} to retrieve a
     * {@link org.nameless.view.DisplayResolutionManager} for managing display resolution.
     *
     * @hide
     * @see #getSystemService
     * @see org.nameless.view.DisplayResolutionManager
     */
    public static final String DISPLAY_RESOLUTION_MANAGER_SERVICE = "resolution_ext";

    /**
     * Use with {@link #getSystemService} to retrieve a
     * {@link com.oplus.os.LinearmotorVibrator} for accessing linear motor vibrator state.
     *
     * @hide
     * @see #getSystemService
     * @see com.oplus.os.LinearmotorVibrator
     */
    public static final String LINEARMOTOR_VIBRATOR_SERVICE = "linearmotor";

    /**
     * Use with {@link #getSystemService} to retrieve a
     * {@link org.nameless.os.PocketManager} for accessing and listening to device pocket state.
     *
     * @hide
     * @see #getSystemService
     * @see org.nameless.os.PocketManager
     */
    public static final String POCKET_SERVICE = "pocket";

    /**
     * Use with {@link #getSystemService} to retrieve a
     * {@link org.nameless.display.RefreshRateManager} for managing display refresh rate.
     *
     * @hide
     * @see #getSystemService
     * @see org.nameless.display.RefreshRateManager
     */
    public static final String REFRESH_RATE_MANAGER_SERVICE = "refresh_rate_ext";
}
