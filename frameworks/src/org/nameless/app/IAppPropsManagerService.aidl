/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.app;

import java.util.Map;

/** @hide */
interface IAppPropsManagerService {

    Map<String, String> getAppSpoofMap(in String packageName, in String processName, boolean isGms);
}
