/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.hardware;

/** @hide */
interface ISensorBlockService {

    /* Get user-set shake sensors block config for specific package */
    int getShakeSensorsConfigForPackage(in String packageName);

    /* Set shake sensors block config for specific package */
    void setShakeSensorsConfigForPackage(in String packageName, in int config);

    /* Get whether we should block shake sensors currently for specific package */
    boolean shouldBlockShakeSensorsNow(in String packageName);
}
