/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package android.provider.settings.backup;

import android.compat.annotation.UnsupportedAppUsage;

import org.nameless.provider.SettingsExt.Global;

public class GlobalSettingsExt {

    @UnsupportedAppUsage
    public static final String[] SETTINGS_TO_BACKUP = {
        Global.WAKE_WHEN_PLUGGED_OR_UNPLUGGED,
    };
}
