/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.os;

import android.content.Context;

import com.android.internal.R;

public class PocketManager {

    public static boolean isSupported(Context context) {
        return context.getResources().getBoolean(R.bool.config_pocketModeSupported);
    }
}
