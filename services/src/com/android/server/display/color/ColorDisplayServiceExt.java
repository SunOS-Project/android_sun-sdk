/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.server.display.color;

import static org.sun.display.DisplayFeatureManager.CUSTOM_DISPLAY_COLOR_MODE_START;
import static org.sun.provider.SettingsExt.Secure.DISPLAY_COLOR_BALANCE_BLUE;
import static org.sun.provider.SettingsExt.Secure.DISPLAY_COLOR_BALANCE_GREEN;
import static org.sun.provider.SettingsExt.Secure.DISPLAY_COLOR_BALANCE_RED;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.os.UserHandle;
import android.provider.Settings;

import org.sun.display.DisplayFeatureManager;

class ColorDisplayServiceExt {

    private static class InstanceHolder {
        private static ColorDisplayServiceExt INSTANCE = new ColorDisplayServiceExt();
    }

    static ColorDisplayServiceExt getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private static final int MSG_APPLY_DISPLAY_COLOR_BALANCE = 11;

    private final ColorBalanceTintController mColorBalanceTintController =
            new ColorBalanceTintController();
    private final DisplayFeatureManager mDisplayFeatureManager =
            DisplayFeatureManager.getInstance();

    private ColorDisplayService mColorDisplayService;
    private int mUserId;

    void init(ColorDisplayService cds, int userId) {
        mColorDisplayService = cds;
        mUserId = userId;
    }

    void setUp() {
        if (mColorBalanceTintController.isAvailable(mColorDisplayService.getContext())) {
            mColorDisplayService.mHandler.sendEmptyMessage(MSG_APPLY_DISPLAY_COLOR_BALANCE);
        }
    }

    void registerContentObserver(ContentObserver observer) {
        final ContentResolver cr = mColorDisplayService.getContext().getContentResolver();
        cr.registerContentObserver(Settings.Secure.getUriFor(DISPLAY_COLOR_BALANCE_RED),
                false /* notifyForDescendants */, observer, mUserId);
        cr.registerContentObserver(Settings.Secure.getUriFor(DISPLAY_COLOR_BALANCE_GREEN),
                false /* notifyForDescendants */, observer, mUserId);
        cr.registerContentObserver(Settings.Secure.getUriFor(DISPLAY_COLOR_BALANCE_BLUE),
                false /* notifyForDescendants */, observer, mUserId);
    }

    void onHandleMessage(int what) {
        switch (what) {
            case MSG_APPLY_DISPLAY_COLOR_BALANCE:
                mColorBalanceTintController.updateBalance(
                        mColorDisplayService.getContext(), mUserId);
                mColorDisplayService.applyTint(
                        mColorBalanceTintController, true);
                break;
        }
    }

    void onSettingsChanged(String setting) {
        switch (setting) {
            case DISPLAY_COLOR_BALANCE_RED:
            case DISPLAY_COLOR_BALANCE_BLUE:
            case DISPLAY_COLOR_BALANCE_GREEN:
                mColorDisplayService.mHandler.sendEmptyMessage(MSG_APPLY_DISPLAY_COLOR_BALANCE);
                break;
        }
    }

    void onUserChanged(int newUserId) {
        mUserId = newUserId;
    }

    boolean interceptDisplayColorModeChange(int mode) {
        if (mode >= CUSTOM_DISPLAY_COLOR_MODE_START) {
            setColorModeFeature(mode);
            return true;
        }
        return false;
    }

    void setColorModeFeature(int mode) {
        mDisplayFeatureManager.setColorMode(mode);
    }

    int getFirstCustomColorMode() {
        final int[] availableColorModes =
                mColorDisplayService.getContext().getResources().getIntArray(
                com.android.internal.R.array.config_availableColorModes);
        if (availableColorModes == null) {
            return -1;
        }
        for (int mode : availableColorModes) {
            if (mode >= CUSTOM_DISPLAY_COLOR_MODE_START) {
                return mode;
            }
        }
        return -1;
    }

    boolean setColorBalanceChannel(int channel, int value) {
        if (mUserId == UserHandle.USER_NULL) {
            return false;
        }

        final boolean putSuccess = Settings.Secure.putIntForUser(
                mColorDisplayService.getContext().getContentResolver(),
                ColorBalanceTintController.channelToKey(channel),
                value, mUserId);
        if (putSuccess) {
            mColorDisplayService.mHandler.sendEmptyMessage(MSG_APPLY_DISPLAY_COLOR_BALANCE);
        }

        return putSuccess;
    }

    int getColorBalanceChannel(int channel) {
        return Settings.Secure.getIntForUser(
                mColorDisplayService.getContext().getContentResolver(),
                ColorBalanceTintController.channelToKey(channel),
                255, mUserId);
    }
}
