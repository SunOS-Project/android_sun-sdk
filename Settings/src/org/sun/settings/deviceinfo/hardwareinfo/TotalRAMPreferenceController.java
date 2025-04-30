/*
 * Copyright (C) 2019 The Android Open Source Project
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

package org.sun.settings.deviceinfo.hardwareinfo;

import android.app.ActivityManager;
import android.content.Context;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.slices.Sliceable;

import java.text.DecimalFormat;

public class TotalRAMPreferenceController extends BasePreferenceController {

    public TotalRAMPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return mContext.getResources().getBoolean(R.bool.config_show_device_model)
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean useDynamicSliceSummary() {
        return true;
    }

    @Override
    public boolean isSliceable() {
        return true;
    }

    @Override
    public CharSequence getSummary() {
        final ActivityManager am = mContext.getSystemService(ActivityManager.class);
        final ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(memInfo);

        final DecimalFormat ramDecimalForm = new DecimalFormat("#.#");
        final double kb = (double) memInfo.totalMem / 1024d;
        final double mb = (double) memInfo.totalMem / 1024d / 1024d;
        final double gb = (double) memInfo.totalMem / 1024d / 1024d / 1024d;
        String ramString = "";
        if (gb > 1d) {
            ramString = ramDecimalForm.format(gb).concat(" GB");
        } else if (mb > 1d) {
            ramString = ramDecimalForm.format(mb).concat(" MB");
        } else {
            ramString = ramDecimalForm.format(kb).concat(" KB");
        }
        return ramString;
    }
}
