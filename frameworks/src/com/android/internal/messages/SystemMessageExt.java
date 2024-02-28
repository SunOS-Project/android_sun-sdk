/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.internal.messages;

/** @hide */
public class SystemMessageExt {

    private static final int CUSTOM_NOTIFICATION_ID_START = 2000;

    // Notify the user about optimized charge status.
    // package: android
    public static final int NOTE_OPTIMIZED_CHARGE = CUSTOM_NOTIFICATION_ID_START + 1;

    // Notify the user about wireless reversed charge status.
    // package: android
    public static final int NOTE_WIRELESS_REVERSED_CHARGE = CUSTOM_NOTIFICATION_ID_START + 2;
}
