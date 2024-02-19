/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.content;

/** @hide */
interface IOnlineConfigurable {

    /** Get config version. When config version < framework version, update won't be processed */
    int getVersion();

    /** Online config uri */
    String getOnlineConfigUri();

    /** Config path that built in system */
    String getSystemConfigPath();

    /** Config path that downloaded from online */
    String getLocalConfigPath();

    /** Called when local config is downloaded and verified that can be updated */
    void onConfigUpdated();
}
