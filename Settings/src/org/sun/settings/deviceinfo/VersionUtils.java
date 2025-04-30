/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.settings.deviceinfo;

import android.os.SystemProperties;

public class VersionUtils {

    public static String getSunVersion() {
        return SystemProperties.get("ro.sun.version.display","");
    }
}
