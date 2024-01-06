/*
 * Copyright (C) 2023 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.internal.util.nameless;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public abstract class UserSwitchReceiver extends BroadcastReceiver {

    private final Context mContext;

    private boolean mListening = false;

    public UserSwitchReceiver(Context context) {
        mContext = context;
    }

    public void setListening(boolean listening) {
        if (mListening != listening) {
            if (listening) {
                final IntentFilter intentFilter = new IntentFilter(Intent.ACTION_USER_SWITCHED);
                mContext.registerReceiver(this, intentFilter);
                mListening = true;
            } else {
                mContext.unregisterReceiver(this);
                mListening = false;
            }
        }
    }

    public abstract void onUserSwitched();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_USER_SWITCHED.equals(intent.getAction())) {
            onUserSwitched();
        }
    }
}
