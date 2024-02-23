/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.app;

import org.nameless.app.GameModeInfo;
import org.nameless.app.IGameModeInfoListener;

/** @hide */
interface IGameModeManagerService {

    boolean addGame(in String packageName);

    boolean removeGame(in String packageName);

    boolean isAppGame(in String packageName);

    GameModeInfo getGameModeInfo();

    boolean registerGameModeInfoListener(in IGameModeInfoListener listener);

    boolean unregisterGameModeInfoListener(in IGameModeInfoListener listener);
}
