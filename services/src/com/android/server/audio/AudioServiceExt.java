/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.server.audio;

import static android.media.AudioManager.DEVICE_OUT_BLE_HEADSET;
import static android.media.AudioManager.DEVICE_OUT_BLUETOOTH_A2DP;
import static android.media.AudioManager.DEVICE_OUT_BLUETOOTH_A2DP_HEADPHONES;
import static android.media.AudioManager.DEVICE_OUT_BLUETOOTH_A2DP_SPEAKER;
import static android.media.AudioManager.DEVICE_OUT_USB_HEADSET;
import static android.media.AudioManager.DEVICE_OUT_WIRED_HEADPHONE;
import static android.media.AudioManager.DEVICE_OUT_WIRED_HEADSET;
import static android.media.AudioSystem.STREAM_MUSIC;

import android.content.Context;
import android.os.Vibrator;

import org.nameless.audio.AlertSliderManager;
import org.nameless.server.audio.AlertSliderController;

public class AudioServiceExt {

    private static class InstanceHolder {
        private static final AudioServiceExt INSTANCE = new AudioServiceExt();
    }

    public static AudioServiceExt getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private AudioService mService;

    private AlertSliderController mAlertSliderController = null;

    private boolean mSystemReady = false;
    private boolean mBtAudio = false;
    private boolean mWiredAudio = false;

    private boolean mIgnoreNextRingerModeChange = false;
    private int mOrigRingerMode = -1;

    void init(AudioService service, Context context, Vibrator vibrator) {
        mService = service;
        if (AlertSliderManager.hasAlertSlider(context)) {
            mAlertSliderController = new AlertSliderController(service, context, vibrator);
        }
    }

    void onSystemReady() {
        mSystemReady = true;
        if (mAlertSliderController != null) {
            mAlertSliderController.onSystemReady();
        }
    }

    void onRingerModeChanged(int ringerMode) {
        if (mIgnoreNextRingerModeChange) {
            mIgnoreNextRingerModeChange = false;
            return;
        }
        if (mOrigRingerMode != -1) {
            mOrigRingerMode = -1;
        }
        if (mSystemReady && mAlertSliderController != null) {
            mAlertSliderController.onRingerModeChanged(ringerMode);
        }
    }

    void onOutputDeviceChanged(int activeStreamType) {
        if (activeStreamType != STREAM_MUSIC) {
            return;
        }

        final int musicStreamDeviceMask = mService.getDeviceMaskForStream(STREAM_MUSIC);
        final boolean wasBtAudio = mBtAudio;
        mBtAudio = (musicStreamDeviceMask &
                (DEVICE_OUT_BLUETOOTH_A2DP |
                DEVICE_OUT_BLUETOOTH_A2DP_HEADPHONES |
                DEVICE_OUT_BLUETOOTH_A2DP_SPEAKER |
                DEVICE_OUT_BLE_HEADSET)) != 0;
        final boolean wasWiredAudio = mWiredAudio;
        mWiredAudio = (musicStreamDeviceMask &
                (DEVICE_OUT_USB_HEADSET |
                DEVICE_OUT_WIRED_HEADPHONE |
                DEVICE_OUT_WIRED_HEADSET)) != 0;

        if (wasBtAudio == mBtAudio && wasWiredAudio == mWiredAudio) {
            return;
        }

        if (mSystemReady && mAlertSliderController != null) {
            mAlertSliderController.onOutputDeviceChanged(mBtAudio, mWiredAudio);
        }
    }

    public void setTempRingerMode(int ringerMode) {
        if (mOrigRingerMode == -1) {
            mOrigRingerMode = mService.getRingerModeInternal();
        }
        mIgnoreNextRingerModeChange = true;
        mService.setRingerModeInternal(ringerMode, "android");
    }

    public void restoreRingerMode() {
        if (mOrigRingerMode != -1) {
            mService.setRingerModeInternal(mOrigRingerMode, "android");
        }
    }
}
