/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.server.policy.gesture;

import android.content.res.Resources;

import com.android.internal.R;

public class ResourceUtils {

    public static float getGameModeGestureValidDistance(Resources res, int shortEdge) {
        return res.getFloat(R.dimen.game_mode_gesture_valid_distance) * shortEdge;
    }

    public static float getGameModePortraitAreaBottom(Resources res, int shortEdge) {
        return res.getFloat(R.dimen.game_mode_gesture_portrait_area_bottom) * shortEdge;
    }

    public static float getGameModeLandscapeAreaBottom(Resources res, int shortEdge) {
        return res.getFloat(R.dimen.game_mode_gesture_landscape_area_bottom) * shortEdge;
    }

    public static float getWindowModeGestureValidDistance(Resources res, int shortEdge) {
        return res.getFloat(R.dimen.windowing_mode_gesture_valid_distance) * shortEdge;
    }
}
