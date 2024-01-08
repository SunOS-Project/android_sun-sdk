/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.display;

/** @hide */
oneway interface IRefreshRateListener {

    void onRequestedRefreshRate(int refreshRate);

    void onRequestedMemcRefreshRate(int refreshRate);
}
