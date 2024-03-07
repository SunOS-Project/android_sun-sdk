/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.keyguard;

import static com.android.keyguard.KeyguardUpdateMonitor.BIOMETRIC_ACTION_UPDATE;

import android.content.Context;

import org.nameless.os.IPocketCallback;
import org.nameless.os.PocketManager;

class KeyguardUpdateMonitorExt {

    private static class InstanceHolder {
        private static KeyguardUpdateMonitorExt INSTANCE = new KeyguardUpdateMonitorExt();
    }

    static KeyguardUpdateMonitorExt getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private static final int MSG_POCKET_STATE_CHANGED = 501;

    private KeyguardUpdateMonitor mMonitor;
    private PocketManager mPocketManager;
    private IPocketCallback mPocketCallback;
    private boolean mIsDeviceInPocket;

    void init(KeyguardUpdateMonitor monitor, Context context) {
        mMonitor = monitor;
        mPocketManager = context.getSystemService(PocketManager.class);
        mPocketCallback = new IPocketCallback.Stub() {
            @Override
            public void onStateChanged(boolean isDeviceInPocket, int reason) {
                final boolean wasInPocket = mIsDeviceInPocket;
                if (reason == PocketManager.REASON_SENSOR) {
                    mIsDeviceInPocket = isDeviceInPocket;
                } else {
                    mIsDeviceInPocket = false;
                }
                if (wasInPocket != mIsDeviceInPocket) {
                    mMonitor.getHandler().sendEmptyMessage(MSG_POCKET_STATE_CHANGED);
                }
            }
        };
        mPocketManager.addCallback(mPocketCallback);
    }

    void destroy() {
        mPocketManager.removeCallback(mPocketCallback);
    }

    boolean handleMessage(int what) {
        switch (what) {
            case MSG_POCKET_STATE_CHANGED:
                mMonitor.updateFingerprintListeningState(BIOMETRIC_ACTION_UPDATE);
                return true;
        }
        return false;
    }

    boolean isDeviceInPocket() {
        return mIsDeviceInPocket;
    }
}
