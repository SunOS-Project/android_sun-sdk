/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.app;

import android.content.ComponentName;

import java.util.Map;

/** @hide */
interface IAppPropsManagerService {

    Map<String, String> getAppSpoofMap(in ComponentName componentName);
}
