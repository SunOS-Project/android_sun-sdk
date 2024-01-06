/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.view;

import android.content.ComponentName;

/** @hide */
oneway interface IAppFocusObserver {

    void onFullscreenFocusChanged(String packageName, String activityName);
}
