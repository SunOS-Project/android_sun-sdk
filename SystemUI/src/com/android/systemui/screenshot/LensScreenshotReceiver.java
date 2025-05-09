/*
 * Copyright (C) 2022 ShapeShiftOS
 * Copyright (C) 2022-2024 Nameless-AOSP
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.screenshot;

import static com.android.systemui.screenshot.ScreenshotController.ACTION_TYPE_LENS;
import static com.android.systemui.screenshot.ScreenshotController.EXTRA_ID;
import static com.android.systemui.screenshot.ScreenshotController.EXTRA_SMART_ACTIONS_ENABLED;
import static com.android.systemui.screenshot.ScreenshotController.SCREENSHOT_URI_ID;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import com.android.internal.util.sun.CustomUtils;

import com.android.systemui.dagger.qualifiers.Background;
import com.android.systemui.res.R;

import java.util.concurrent.Executor;

import javax.inject.Inject;

public class LensScreenshotReceiver extends BroadcastReceiver {

    private static final String GSA_PACKAGE = "com.google.android.googlequicksearchbox";
    private static final String LENS_SHARE_ACTIVITY = "com.google.android.apps.search.lens.LensShareEntryPointActivity";

    private final ScreenshotSmartActions mScreenshotSmartActions;
    private final Executor mBackgroundExecutor;

    @Inject
    public LensScreenshotReceiver(ScreenshotSmartActions screenshotSmartActions,
            @Background Executor backgroundExecutor) {
        mScreenshotSmartActions = screenshotSmartActions;
        mBackgroundExecutor = backgroundExecutor;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.hasExtra(SCREENSHOT_URI_ID)) {
            return;
        }

        final Uri uri = Uri.parse(intent.getStringExtra(SCREENSHOT_URI_ID));
        mBackgroundExecutor.execute(() -> {
            // action to execute goes here
            if (!isGsaAvailable(context)) {
                Toast.makeText(context, R.string.google_lens_unavailable, Toast.LENGTH_SHORT).show();
                return;
            }
            final ClipData clipdata = new ClipData(
                    new ClipDescription("content",
                    new String[]{"image/png"}),
                    new ClipData.Item(uri));
            final Intent share = new Intent();
            share.setAction(Intent.ACTION_SEND)
                    .setComponent(new ComponentName(GSA_PACKAGE, LENS_SHARE_ACTIVITY))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .setType("image/png")
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .setClipData(clipdata);
            context.startActivity(share);
        });

        if (intent.getBooleanExtra(EXTRA_SMART_ACTIONS_ENABLED, false)) {
            mScreenshotSmartActions.notifyScreenshotAction(
                    intent.getStringExtra(EXTRA_ID), ACTION_TYPE_LENS, false, null);
        }
    }

    private boolean isGsaAvailable(Context context) {
        return CustomUtils.isPackageInstalled(context, GSA_PACKAGE, false /* ignoreState */);
    }
}
