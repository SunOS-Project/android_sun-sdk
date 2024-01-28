/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.os;

oneway interface IRotateConfigListener {

    void onRotateConfigChanged(int config);
}
