/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.content;

import java.util.List;

import org.sun.content.IOnlineConfigurable;

/** @hide */
interface IOnlineConfigManagerService {

    List<IOnlineConfigurable> getRegisteredClients();

    boolean registerOnlineConfigurable(in IOnlineConfigurable configurable);

    boolean unregisterOnlineConfigurable(in IOnlineConfigurable configurable);
}
