/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.os;

import org.nameless.os.IRotateConfigListener;

/** @hide */
interface IRotateManagerService {

    /* Get user preferred auto-rotate config for specific package */
    int getRotateConfigForPackage(in String packageName);

    /* Set user preferred auto-rotate config for specific package */
    void setRotateConfigForPackage(in String packageName, in int rotateConfig);

    /* Get auto-rotate config set by current package */
    int getCurrentRotateConfig();

    /* Register listener to listen rotate config change */
    boolean registerRotateConfigListener(in IRotateConfigListener listener);

    /* Unregister listener to listen rotate config change */
    boolean unregisterRotateConfigListener(in IRotateConfigListener listener);
}
