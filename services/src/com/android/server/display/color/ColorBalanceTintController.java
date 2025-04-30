/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.display.color;

import static org.sun.provider.SettingsExt.Secure.DISPLAY_COLOR_BALANCE_BLUE;
import static org.sun.provider.SettingsExt.Secure.DISPLAY_COLOR_BALANCE_GREEN;
import static org.sun.provider.SettingsExt.Secure.DISPLAY_COLOR_BALANCE_RED;

import android.content.Context;
import android.graphics.Color;
import android.hardware.display.ColorDisplayManager;
import android.opengl.Matrix;
import android.provider.Settings;

import java.util.Arrays;

/** Control the color transform for global color balance. */
final class ColorBalanceTintController extends TintController {

    private static final int LEVEL_COLOR_MATRIX_COLOR_BALANCE = 400;

    private final float[] mMatrix = new float[16];

    @Override
    public void setUp(Context context, boolean needsLinear) {
    }

    @Override
    public float[] getMatrix() {
        return Arrays.copyOf(mMatrix, mMatrix.length);
    }

    @Override
    public void setMatrix(int rgb) {
        Matrix.setIdentityM(mMatrix, 0);
        mMatrix[0] = ((float) Color.red(rgb)) / 255.0f;
        mMatrix[5] = ((float) Color.green(rgb)) / 255.0f;
        mMatrix[10] = ((float) Color.blue(rgb)) / 255.0f;
    }

    @Override
    public int getLevel() {
        return LEVEL_COLOR_MATRIX_COLOR_BALANCE;
    }

    @Override
    public boolean isAvailable(Context context) {
        return ColorDisplayManager.isColorTransformAccelerated(context);
    }

    void updateBalance(Context context, int userId) {
        final int red = Settings.Secure.getIntForUser(context.getContentResolver(),
                channelToKey(0), 255, userId);
        final int green = Settings.Secure.getIntForUser(context.getContentResolver(),
                channelToKey(1), 255, userId);
        final int blue = Settings.Secure.getIntForUser(context.getContentResolver(),
                channelToKey(2), 255, userId);

        final int rgb = Color.rgb(red, green, blue);
        setMatrix(rgb);
    }

    static String channelToKey(int channel) {
        switch (channel) {
            case 0:
                return DISPLAY_COLOR_BALANCE_RED;
            case 1:
                return DISPLAY_COLOR_BALANCE_GREEN;
            case 2:
                return DISPLAY_COLOR_BALANCE_BLUE;
            default:
                throw new IllegalArgumentException("Unknown channel: " + channel);
        }
    }
}
