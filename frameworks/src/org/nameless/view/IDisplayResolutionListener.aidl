/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.view;

/** @hide */
oneway interface IDisplayResolutionListener {

    void onDisplayResolutionChanged(int width, int height);
}
