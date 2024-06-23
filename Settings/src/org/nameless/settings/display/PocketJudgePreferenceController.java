/*
 * Copyright (C) 2018 Paranoid Android
 * Copyright (C) 2024 The Nameless-AOSP Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.nameless.settings.display;

import static org.nameless.provider.SettingsExt.System.POCKET_JUDGE;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;

import com.android.settings.core.TogglePreferenceController;

public class PocketJudgePreferenceController extends TogglePreferenceController {

    private final Context mContext;

    public PocketJudgePreferenceController(Context context, String key) {
        super(context, key);
        mContext = context;
    }

    @Override
    public boolean isChecked() {
        return Settings.System.getIntForUser(mContext.getContentResolver(),
                POCKET_JUDGE, 1, UserHandle.USER_CURRENT) == 1;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.System.putIntForUser(mContext.getContentResolver(),
                POCKET_JUDGE, isChecked ? 1 : 0, UserHandle.USER_CURRENT);
    }

    @Override
    public int getAvailabilityStatus() {
        return mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_pocketModeSupported)
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return 0;
    }
}
