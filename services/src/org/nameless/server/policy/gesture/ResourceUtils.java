/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.server.policy.gesture;

import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import com.android.internal.R;

public class ResourceUtils {

    public static int getGameModeGestureValidDistance(Resources res) {
        return res.getDimensionPixelSize(R.dimen.game_mode_gesture_valid_distance);
    }

    public static int getGameModePortraitAreaBottom(Resources res) {
        return res.getDimensionPixelSize(R.dimen.game_mode_gesture_portrait_area_bottom);
    }

    public static int getGameModeLandscapeAreaBottom(Resources res) {
        return res.getDimensionPixelSize(R.dimen.game_mode_gesture_landscape_area_bottom);
    }

    public static int getWindowModeGestureValidDistance(Resources res) {
        return pxFromDp(res.getDimensionPixelSize(
                R.dimen.windowing_mode_gesture_valid_distance), res.getDisplayMetrics());
    }

    private static int pxFromDp(float size, DisplayMetrics metrics) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, size, metrics));
    }
}
