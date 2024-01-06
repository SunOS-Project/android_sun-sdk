/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.provider;

/** @hide */
public class SettingsExt {

    public static class System {
    }

    public static class Secure {

        /**
         * Whether UDFPS is active while the screen is off.
         *
         * <p>1 if true, 0 or unset otherwise.
         *
         * @hide
         */
        public static final String SCREEN_OFF_UDFPS_ENABLED = "screen_off_udfps_enabled";
    }

    public static class Global {
    }
}
