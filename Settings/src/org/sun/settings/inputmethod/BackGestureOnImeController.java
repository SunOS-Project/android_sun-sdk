/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.settings.inputmethod;

import android.content.Context;
import android.inputmethodservice.InputMethodServiceExt;
import android.os.UserHandle;
import android.provider.Settings;

import com.android.settings.core.TogglePreferenceController;

public class BackGestureOnImeController extends TogglePreferenceController {

    private static final int GESTURAL_MODE = 2;

    public BackGestureOnImeController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return Settings.Secure.getIntForUser(mContext.getContentResolver(),
                Settings.Secure.NAVIGATION_MODE, GESTURAL_MODE,
                UserHandle.USER_CURRENT) == GESTURAL_MODE ?
                AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isChecked() {
        return InputMethodServiceExt.getAllowBackGestureOnIme();
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        InputMethodServiceExt.setAllowBackGestureOnIme(isChecked);
        return true;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return 0;
    }
}
