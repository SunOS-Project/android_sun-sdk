/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package android.app;

import static org.sun.os.DebugConstants.DEBUG_OP_LM;

import java.util.Set;

import org.sun.content.ContextExt;

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
            if (DEBUG_OP_LM) {
                return false;
            }
            return !LINEAR_MOTOR_VIBRATOR_WHITELIST.contains(packageName);
        }
        return false;
    }
}
