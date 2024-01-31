/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package android.app;

import java.util.Set;

import org.nameless.content.ContextExt;

/** @hide */
class ContextImplExt {

    private ContextImplExt() {}

    private static final Set<String> LINEAR_MOTOR_VIBRATOR_WHITELIST = Set.of(
        "com.oneplus.camera",
        "com.oneplus.gallery",
        "com.oplus.camera"
    );

    static boolean interceptGetSystemService(String serviceName, String packageName) {
        if (ContextExt.LINEARMOTOR_VIBRATOR_SERVICE.equals(serviceName)) {
            return !LINEAR_MOTOR_VIBRATOR_WHITELIST.contains(packageName);
        }
        return false;
    }
}
