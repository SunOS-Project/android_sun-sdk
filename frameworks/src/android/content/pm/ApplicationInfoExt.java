/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package android.content.pm;

import java.util.Set;

/** @hide */
class ApplicationInfoExt {

    private static final Set<String> HIDDEN_API_WHITELIST_APPS = Set.of(
        "com.realme.link"
    );

    private ApplicationInfoExt() {}

    static boolean isAllowedToUseHiddenApis(ApplicationInfo info) {
        return HIDDEN_API_WHITELIST_APPS.contains(info.packageName);
    }
}
