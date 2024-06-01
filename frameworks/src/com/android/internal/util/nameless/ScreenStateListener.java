/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.internal.util.nameless;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;

/**
 * A simple listener that handles screen off / on / unlocked intents.
 * Aim to properly handle screen on / unlocked for below conditions:
 * 1: Set screen lock to "None"
 * 2: Immediately turn on screen after screen off by timeout
 * 
 * On any conditions, will make sure callback called by this order when unlocked:
 * onScreenOn() -> onScreenUnlocked
 *
 * @hide
 */
public abstract class ScreenStateListener extends BroadcastReceiver {

    private static final long SCREEN_ON_UNLOCKED_DELAY = 50L;

    private static final int MSG_SCREEN_OFF = 0;
    private static final int MSG_SCREEN_ON = 1;
    private static final int MSG_SCREEN_UNLOCKED = 2;

    private final Context mContext;
    private final DelayHandler mDelayHandler;
    private final Handler mHandler;
    private final KeyguardManager mKeyguardManager;

    private boolean mListening = false;

    public ScreenStateListener(Context context) {
        this(context, null);
    }

    public ScreenStateListener(Context context, Handler handler) {
        mContext = context;
        mHandler = handler;
        mDelayHandler = new DelayHandler();
        mKeyguardManager = context.getSystemService(KeyguardManager.class);
    }

    public void setListening(boolean listening) {
        if (mListening != listening) {
            if (listening) {
                final IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
                intentFilter.addAction(Intent.ACTION_SCREEN_ON);
                intentFilter.addAction(Intent.ACTION_USER_PRESENT);
                mContext.registerReceiverForAllUsers(this, intentFilter, null, null);
                mListening = true;
            } else {
                mContext.unregisterReceiver(this);
                mListening = false;
            }
        }
    }

    public abstract void onScreenOff();
    public abstract void onScreenOn();
    public abstract void onScreenUnlocked();

    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()) {
            case Intent.ACTION_SCREEN_OFF:
                mDelayHandler.sendEmptyMessage(MSG_SCREEN_OFF);
                break;
            case Intent.ACTION_SCREEN_ON:
                mDelayHandler.sendEmptyMessageDelayed(MSG_SCREEN_ON, SCREEN_ON_UNLOCKED_DELAY);
                break;
            case Intent.ACTION_USER_PRESENT:
                mDelayHandler.sendEmptyMessageDelayed(MSG_SCREEN_UNLOCKED, SCREEN_ON_UNLOCKED_DELAY);
                break;
        }
    }

    private void doSomething(Runnable runnable) {
        if (mHandler != null) {
            mHandler.post(runnable);
        } else {
            runnable.run();
        }
    }

    private class DelayHandler extends Handler {
        DelayHandler() {
            super();
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case MSG_SCREEN_OFF:
                    mDelayHandler.removeMessages(MSG_SCREEN_ON);
                    mDelayHandler.removeMessages(MSG_SCREEN_UNLOCKED);
                    doSomething(() -> onScreenOff());
                    break;
                case MSG_SCREEN_ON:
                    mDelayHandler.removeMessages(MSG_SCREEN_UNLOCKED);
                    if (mKeyguardManager.isKeyguardLocked()) {
                        doSomething(() -> onScreenOn());
                    } else {
                        doSomething(() -> {
                            onScreenOn();
                            onScreenUnlocked();
                        });
                    }
                    break;
                case MSG_SCREEN_UNLOCKED:
                    if (mDelayHandler.hasMessages(MSG_SCREEN_ON)) {
                        break;
                    }
                    doSomething(() -> onScreenUnlocked());
                    break;
            }
        }
    }
}
