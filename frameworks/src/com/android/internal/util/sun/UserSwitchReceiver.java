/*
 * Copyright (C) 2023 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.internal.util.sun;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;

/** @hide */
public abstract class UserSwitchReceiver extends BroadcastReceiver {

    private final Context mContext;
    private final Handler mHandler;

    private boolean mListening = false;

    public UserSwitchReceiver(Context context) {
        this(context, null);
    }

    public UserSwitchReceiver(Context context, Handler handler) {
        mContext = context;
        mHandler = handler;
    }

    public void setListening(boolean listening) {
        if (mListening != listening) {
            if (listening) {
                final IntentFilter intentFilter = new IntentFilter(Intent.ACTION_USER_SWITCHED);
                if (mHandler != null) {
                    mContext.registerReceiver(this, intentFilter, null, mHandler);
                } else {
                    mContext.registerReceiver(this, intentFilter);
                }
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
