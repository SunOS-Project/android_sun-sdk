/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package android.app;

import java.util.Set;

/** @hide */
class ApplicationPackageManagerExt {

    static final int HAS_FEATURE_UNHANDLED = -1;
    static final int HAS_FEATURE_NO = 0;
    static final int HAS_FEATURE_YES = 1;

    private static final Set<String> FEATURES_NEXUS = Set.of(
        "com.google.android.apps.photos.NEXUS_PRELOAD",
        "com.google.android.apps.photos.nexus_preload",
        "com.google.android.feature.PIXEL_EXPERIENCE",
        "com.google.android.feature.GOOGLE_BUILD",
        "com.google.android.feature.GOOGLE_EXPERIENCE"
    );

    private static final Set<String> FEATURES_PIXEL = Set.of(
        "com.google.android.apps.photos.PIXEL_2019_PRELOAD",
        "com.google.android.apps.photos.PIXEL_2019_MIDYEAR_PRELOAD",
        "com.google.android.apps.photos.PIXEL_2018_PRELOAD",
        "com.google.android.apps.photos.PIXEL_2017_PRELOAD",
        "com.google.android.feature.PIXEL_2021_MIDYEAR_EXPERIENCE",
        "com.google.android.feature.PIXEL_2020_EXPERIENCE",
        "com.google.android.feature.PIXEL_2020_MIDYEAR_EXPERIENCE",
        "com.google.android.feature.PIXEL_2019_EXPERIENCE",
        "com.google.android.feature.PIXEL_2019_MIDYEAR_EXPERIENCE",
        "com.google.android.feature.PIXEL_2018_EXPERIENCE",
        "com.google.android.feature.PIXEL_2017_EXPERIENCE",
        "com.google.android.feature.PIXEL_EXPERIENCE",
        "com.google.android.feature.GOOGLE_BUILD",
        "com.google.android.feature.GOOGLE_EXPERIENCE"
    );

    private static final Set<String> FEATURES_PIXEL_OTHERS = Set.of(
        "com.google.android.feature.ASI",
        "com.google.android.feature.ANDROID_ONE_EXPERIENCE",
        "com.google.android.feature.GOOGLE_FI_BUNDLED",
        "com.google.android.feature.LILY_EXPERIENCE",
        "com.google.android.feature.TURBO_PRELOAD",
        "com.google.android.feature.WELLBEING",
        "com.google.lens.feature.IMAGE_INTEGRATION",
        "com.google.lens.feature.CAMERA_INTEGRATION",
        "com.google.photos.trust_debug_certs",
        "com.google.android.feature.AER_OPTIMIZED",
        "com.google.android.feature.NEXT_GENERATION_ASSISTANT",
        "android.software.game_service",
        "com.google.android.feature.EXCHANGE_6_2",
        "com.google.android.apps.dialer.call_recording_audio",
        "com.google.android.apps.dialer.SUPPORTED"
    );

    private static final Set<String> FEATURES_TENSOR = Set.of(
        "com.google.android.feature.PIXEL_2024_EXPERIENCE",
        "com.google.android.feature.PIXEL_2024_MIDYEAR_EXPERIENCE",
        "com.google.android.feature.PIXEL_2023_EXPERIENCE",
        "com.google.android.feature.PIXEL_2023_MIDYEAR_EXPERIENCE",
        "com.google.android.feature.PIXEL_2022_EXPERIENCE",
        "com.google.android.feature.PIXEL_2022_MIDYEAR_EXPERIENCE",
        "com.google.android.feature.PIXEL_2021_EXPERIENCE"
    );

    private ApplicationPackageManagerExt() {}

    static int hasSystemFeature(String name) {
        if (FEATURES_TENSOR.contains(name)) {
            return HAS_FEATURE_NO;
        }
        final String packageName = ActivityThread.currentPackageName();
        if ("com.google.android.apps.photos".equals(packageName)) {
            if (FEATURES_PIXEL.contains(name)) return HAS_FEATURE_NO;
            if (FEATURES_PIXEL_OTHERS.contains(name)) return HAS_FEATURE_YES;
            if (FEATURES_NEXUS.contains(name)) return HAS_FEATURE_YES;
        }
        if ("com.google.android.googlequicksearchbox".equals(packageName)) {
            if (FEATURES_PIXEL.contains(name)) return HAS_FEATURE_YES;
            if (FEATURES_PIXEL_OTHERS.contains(name)) return HAS_FEATURE_YES;
            if (FEATURES_NEXUS.contains(name)) return HAS_FEATURE_YES;
        }
        if (FEATURES_PIXEL.contains(name)) return HAS_FEATURE_YES;
        if (FEATURES_PIXEL_OTHERS.contains(name)) return HAS_FEATURE_YES;
        return HAS_FEATURE_UNHANDLED;
    }
}
