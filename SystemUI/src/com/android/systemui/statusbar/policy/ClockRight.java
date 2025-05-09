/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.systemui.statusbar.policy;

import android.app.StatusBarManager;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

public class ClockRight extends Clock {

    private boolean mClockVisibleByPolicy = true;
    private boolean mClockVisibleByUser = true;

    public ClockRight(Context context) {
        this(context, null);
    }

    public ClockRight(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ClockRight(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setClockVisibilityByPolicy(boolean visible) {
        mClockVisibleByPolicy = visible;
        updateClockVisibility();
    }

    @Override
    protected void updateClockVisibility() {
        final boolean visible = mClockStyle == STYLE_CLOCK_RIGHT && mShowClock
                && mClockVisibleByPolicy && mClockVisibleByUser;
        try {
            mAutoHideHandler.removeCallbacksAndMessages(null);
        } catch (NullPointerException e) {
            // Do nothing
        }
        setVisibility(visible ? View.VISIBLE : View.GONE);
        if (mClockAutoHide && visible) {
            mAutoHideHandler.postDelayed(() -> autoHideClock(), mShowDuration * 1000);
        }
    }

    @Override
    public void disable(int displayId, int state1, int state2, boolean animate) {
        final boolean clockVisibleByPolicy = (state1 & StatusBarManager.DISABLE_CLOCK) == 0;
        if (clockVisibleByPolicy != mClockVisibleByPolicy) {
            setClockVisibilityByPolicy(clockVisibleByPolicy);
        }
    }
}
