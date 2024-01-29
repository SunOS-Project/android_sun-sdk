/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package android.provider.settings.backup;

import android.compat.annotation.UnsupportedAppUsage;

import org.nameless.provider.SettingsExt.Secure;

public class SecureSettingsExt {

    @UnsupportedAppUsage
    public static final String[] SETTINGS_TO_BACKUP = {
        Secure.SCREEN_OFF_UDFPS_ENABLED,
        Secure.QSTILE_REQUIRES_UNLOCKING,
        Secure.ADVANCED_REBOOT,
        Secure.DOZE_FOR_NOTIFICATIONS,
    };
}
