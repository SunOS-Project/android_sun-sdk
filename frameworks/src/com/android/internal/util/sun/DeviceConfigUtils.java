/*
 * Copyright (C) 2023 The Pixel Experience Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.util.sun;

import android.content.res.Resources;
import android.provider.Settings;
import android.util.Log;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.android.internal.util.ArrayUtils;

/** @hide */
public class DeviceConfigUtils {

    private static final String TAG = "DeviceConfigUtils";

    private static final boolean DEBUG = false;

    private static String[] getDeviceConfigsOverride() {
        final String[] globalDeviceConfigs =
            Resources.getSystem().getStringArray(com.android.internal.R.array.global_device_configs_override);
        final String[] deviceConfigs =
            Resources.getSystem().getStringArray(com.android.internal.R.array.device_configs_override);
        final String[] allDeviceConfigs = Arrays.copyOf(globalDeviceConfigs,
                globalDeviceConfigs.length + deviceConfigs.length);
        System.arraycopy(deviceConfigs, 0, allDeviceConfigs, globalDeviceConfigs.length, deviceConfigs.length);
        return allDeviceConfigs;
    }

    public static boolean shouldDenyDeviceConfigControl(String namespace, String property) {
        if (DEBUG) Log.d(TAG, "shouldAllowDeviceConfigControl, namespace=" + namespace + ", property=" + property);
        for (String p : getDeviceConfigsOverride()) {
            final String[] kv = p.split("=");
            final String fullKey = kv[0];
            final String[] nsKey = fullKey.split("/");
            if (nsKey[0].equals(namespace) && nsKey[1].equals(property)) {
                if (DEBUG) Log.d(TAG, "shouldAllowDeviceConfigControl, deny, namespace=" + namespace + ", property=" + property);
                return true;
            }
        }
        if (DEBUG) Log.d(TAG, "shouldAllowDeviceConfigControl, allow, namespace=" + namespace + ", property=" + property);
        return false;
    }

    public static void setDefaultProperties(String filterNamespace, String filterProperty) {
        if (DEBUG) Log.d(TAG, "setDefaultProperties");
        for (String p : getDeviceConfigsOverride()) {
            final String[] kv = p.split("=");
            final String fullKey = kv[0];
            final String[] nsKey = fullKey.split("/");

            final String namespace = nsKey[0];
            final String key = nsKey[1];

            if (filterNamespace != null && filterNamespace.equals(namespace)) {
                continue;
            }

            if (filterProperty != null && filterProperty.equals(key)) {
                continue;
            }

            String value = "";
            if (kv.length > 1) {
                value = kv[1];
            }
            Settings.Config.putString(namespace, key, value, false);
        }
    }
}
