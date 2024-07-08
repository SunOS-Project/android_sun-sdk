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
     * {@link org.nameless.app.AppPropsManager} for managing app props spoof.
     *
     * @hide
     * @see #getSystemService
     * @see org.nameless.app.AppPropsManager
     */
    public static final String APP_PROPS_MANAGER_SERVICE = "app_props";

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
     * {@link org.nameless.app.GameModeManager} for managing game mode.
     *
     * @hide
     * @see #getSystemService
     * @see org.nameless.app.GameModeManager
     */
    public static final String GAME_MODE_SERVICE = "game_ext";

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
     * {@link org.nameless.content.OnlineConfigManager} for managing online configs.
     *
     * @hide
     * @see #getSystemService
     * @see org.nameless.content.OnlineConfigManager
     */
    public static final String ONLINE_CONFIG_MANAGER_SERVICE = "online_config";

    /**
     * Use with {@link #getSystemService} to retrieve a
     * {@link org.nameless.display.RefreshRateManager} for managing display refresh rate.
     *
     * @hide
     * @see #getSystemService
     * @see org.nameless.display.RefreshRateManager
     */
    public static final String REFRESH_RATE_MANAGER_SERVICE = "refresh_rate_ext";

    /**
     * Use with {@link #getSystemService} to retrieve a
     * {@link org.nameless.os.RotateManager} for managing display auto rotate.
     *
     * @hide
     * @see #getSystemService
     * @see org.nameless.os.RotateManager
     */
    public static final String ROTATE_MANAGER_SERVICE = "rotate_ext";

    /**
     * Use with {@link #getSystemService} to retrieve a
     * {@link org.nameless.hardware.SensorBlockManager} for managing sensor block state.
     *
     * @hide
     * @see #getSystemService
     * @see org.nameless.hardware.SensorBlockManager
     */
    public static final String SENSOR_BLOCK_MANAGER_SERVICE = "sensor_block";
}
