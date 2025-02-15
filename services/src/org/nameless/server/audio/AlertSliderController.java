/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.server.audio;

import static android.media.AudioManager.ADJUST_MUTE;
import static android.media.AudioManager.ADJUST_UNMUTE;
import static android.media.AudioManager.RINGER_MODE_NORMAL;
import static android.media.AudioManager.RINGER_MODE_SILENT;
import static android.media.AudioManager.RINGER_MODE_VIBRATE;
import static android.media.AudioSystem.DEVICE_OUT_EARPIECE;
import static android.media.AudioSystem.DEVICE_OUT_SPEAKER;
import static android.media.AudioSystem.DEVICE_OUT_SPEAKER_SAFE;
import static android.media.AudioSystem.STREAM_MUSIC;
import static android.os.Process.THREAD_PRIORITY_DEFAULT;

import static org.nameless.audio.AlertSliderManager.STATE_BOTTOM;
import static org.nameless.audio.AlertSliderManager.STATE_MIDDLE;
import static org.nameless.audio.AlertSliderManager.STATE_TOP;
import static org.nameless.audio.AlertSliderManager.STATE_UNKNOWN;
import static org.nameless.os.DebugConstants.DEBUG_AUDIO_SLIDER;

import static vendor.nameless.hardware.vibratorExt.Effect.ALERT_SLIDER_BOTTOM;
import static vendor.nameless.hardware.vibratorExt.Effect.ALERT_SLIDER_MIDDLE;
import static vendor.nameless.hardware.vibratorExt.Effect.DOUBLE_CLICK;
import static vendor.nameless.hardware.vibratorExt.Effect.HEAVY_CLICK;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.os.VibrationAttributes;
import android.os.VibrationExtInfo;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Slog;

import com.android.server.ServiceThread;
import com.android.server.audio.AudioService;

import org.nameless.audio.AlertSliderManager;
import org.nameless.provider.SettingsExt;

public class AlertSliderController {

    private static final String TAG = "AlertSliderController";

    private static final String CALLING_PACKAGE = "android";

    private static final int NO_CHANGE_VOLUME = 0;
    private static final int MUTE_VOLUME = 1;
    private static final int RESTORE_VOLUME = 2;

    private static final int MSG_DO_VIBRATE = 0;

    private static final long VIBRATE_DELAY_MS = 350L;

    private static final VibrationAttributes HARDWARE_FEEDBACK_VIBRATION_ATTRIBUTES =
            new VibrationAttributes.Builder(
                    VibrationAttributes.createForUsage(VibrationAttributes.USAGE_HARDWARE_FEEDBACK))
            .setFlags(VibrationAttributes.FLAG_BYPASS_USER_VIBRATION_INTENSITY_OFF)
            .build();

    private static final Object sLock = new Object();

    private final AudioService mService;
    private final ContentResolver mResolver;
    private final Context mContext;
    private final Vibrator mVibrator;

    private final Handler mHandler;
    private final ServiceThread mServiceThread;

    private final SettingsObserver mSettingsObserver;

    private boolean mSystemReady = false;

    private int mState = STATE_UNKNOWN;
    private int mRingerMode = -1;

    private boolean mBtAudio = false;
    private boolean mWiredAudio = false;

    private boolean mMuteMedia;
    private boolean mMuteForHeadset;
    private boolean mVibrateOnBluetooth;

    private boolean mInTmpRingerMode = false;

    public AlertSliderController(AudioService audioService, Context context, Vibrator vibrator) {
        mService = audioService;
        mContext = context;
        mResolver = context.getContentResolver();
        mVibrator = vibrator;

        mServiceThread = new ServiceThread(TAG, THREAD_PRIORITY_DEFAULT, false);
        mServiceThread.start();
        mHandler = new AlertSliderHandler(mServiceThread.getLooper());

        mSettingsObserver = new SettingsObserver(mHandler);
    }

    public void onOutputDeviceChanged(boolean isBtAudio, boolean isWiredAudio) {
        if (DEBUG_AUDIO_SLIDER) {
            Slog.d(TAG, "onOutputDeviceChanged, isBtAudio: " + isBtAudio
                    + ", isWiredAudio: " + isWiredAudio);
        }
        mBtAudio = isBtAudio;
        mWiredAudio = isWiredAudio;
        if (mSystemReady) {
            maybeUpdateMediaVolume(mService.getRingerModeExternal());
            maybeSwitchRingerMode();
        }
    }

    public void onRingerModeChanged(int ringerMode) {
        if (mRingerMode == ringerMode) {
            return;
        }
        if (DEBUG_AUDIO_SLIDER) {
            Slog.d(TAG, "onRingerModeChanged, ringerMode: " + ringerMode);
        }
        mRingerMode = ringerMode;
        mInTmpRingerMode = false;
        maybeUpdateMediaVolume();
    }

    public void onSystemReady() {
        mMuteMedia = Settings.Global.getInt(mResolver,
                SettingsExt.Global.ALERT_SLIDER_MUTE_MEDIA, 0) != 0;
        mMuteForHeadset = Settings.Global.getInt(mResolver,
                SettingsExt.Global.ALERT_SLIDER_APPLY_FOR_HEADSET, 0) != 0;
        mVibrateOnBluetooth = Settings.Global.getInt(mResolver,
                SettingsExt.Global.ALERT_SLIDER_VIBRATE_ON_BLUETOOTH, 0) != 0;

        final int ringerMode = mService.getRingerModeExternal();
        final int sliderState = AlertSliderManager.getSettingsState(mContext);

        if (DEBUG_AUDIO_SLIDER) {
            Slog.d(TAG, "onSystemReady, ringerMode: " + ringerMode + ", sliderState: " + sliderState);
        }

        if (sliderState == STATE_UNKNOWN || ringerMode == sliderState) {
            /* There are two conditions that we need to update state on boot:
             *
             * 1. First boot, persisted slider state is unknown. We need to correct ringer mode to current file state.
             * 2. Persisted ringer mode is same as persisted slider state. In case user changes slider when device is off, update to current file state.
             *
             * If persisted ringer mode is different from persisted slider state, don't update. User may manually changed ringer mode via other settings.
             */
            final int newState = AlertSliderManager.getFileState();
            Settings.Global.putInt(mResolver,
                    SettingsExt.Global.ALERT_SLIDER_STATE, newState);
            setStateInternal(newState, false);
        }

        mHandler.post(() -> mSettingsObserver.observe());

        mSystemReady = true;
    }

    private final class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            mResolver.registerContentObserver(Settings.Global.getUriFor(
                    SettingsExt.Global.ALERT_SLIDER_STATE),
                    false, this, UserHandle.USER_ALL);
            mResolver.registerContentObserver(Settings.Global.getUriFor(
                    SettingsExt.Global.ALERT_SLIDER_MUTE_MEDIA),
                    false, this, UserHandle.USER_ALL);
            mResolver.registerContentObserver(Settings.Global.getUriFor(
                    SettingsExt.Global.ALERT_SLIDER_APPLY_FOR_HEADSET),
                    false, this, UserHandle.USER_ALL);
            mResolver.registerContentObserver(Settings.Global.getUriFor(
                    SettingsExt.Global.ALERT_SLIDER_VIBRATE_ON_BLUETOOTH),
                    false, this, UserHandle.USER_ALL);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            synchronized (sLock) {
                switch (uri.getLastPathSegment()) {
                    case SettingsExt.Global.ALERT_SLIDER_STATE:
                        setStateInternal(AlertSliderManager.getSettingsState(mContext), true);
                        break;
                    case SettingsExt.Global.ALERT_SLIDER_MUTE_MEDIA:
                    case SettingsExt.Global.ALERT_SLIDER_APPLY_FOR_HEADSET:
                        updateMuteMedia();
                        break;
                    case SettingsExt.Global.ALERT_SLIDER_VIBRATE_ON_BLUETOOTH:
                        updateVibrateOnBluetooth();
                        break;
                }
            }
        }
    }

    private void updateMuteMedia() {
        mMuteMedia = Settings.Global.getInt(mResolver,
                SettingsExt.Global.ALERT_SLIDER_MUTE_MEDIA, 0) != 0;
        mMuteForHeadset = Settings.Global.getInt(mResolver,
                SettingsExt.Global.ALERT_SLIDER_APPLY_FOR_HEADSET, 0) != 0;
        maybeUpdateMediaVolume();
    }

    private void updateVibrateOnBluetooth() {
        mVibrateOnBluetooth = Settings.Global.getInt(mResolver,
                SettingsExt.Global.ALERT_SLIDER_VIBRATE_ON_BLUETOOTH, 0) != 0;
        maybeSwitchRingerMode();
    }

    private void setStateInternal(int state, boolean vibrate) {
        if (mState == state) {
            return;
        }
        if (DEBUG_AUDIO_SLIDER) {
            Slog.d(TAG, "setStateInternal, state: " + state + ", vibrate: " + vibrate);
        }
        final int ringerMode = stateToRingerMode(state);
        if (ringerMode == -1) {
            Slog.e(TAG, "Invalid state");
            return;
        }
        mService.setRingerModeInternal(ringerMode, CALLING_PACKAGE);
        if (vibrate && state != STATE_TOP) {
            if (mHandler.hasMessages(MSG_DO_VIBRATE)) {
                mHandler.removeMessages(MSG_DO_VIBRATE);
            }
            mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_DO_VIBRATE,
                    new VibrationExtInfo.Builder()
                        .setEffectId(state == STATE_BOTTOM ? ALERT_SLIDER_BOTTOM : ALERT_SLIDER_MIDDLE)
                        .setFallbackEffectId(state == STATE_BOTTOM ? HEAVY_CLICK : DOUBLE_CLICK)
                        .setVibrationAttributes(HARDWARE_FEEDBACK_VIBRATION_ATTRIBUTES)
                        .build()
                    ), VIBRATE_DELAY_MS);
        }
        mState = state;
    }

    private int getAction(int state) {
        final boolean silentMode = state == STATE_TOP;
        final boolean speakerOutput = isSpeakerOutput();
        if (DEBUG_AUDIO_SLIDER) {
            Slog.d(TAG, "getAction, mMuteMedia: " + mMuteMedia
                    + ", mMuteForHeadset: " + mMuteForHeadset
                    + ", silentMode: " + silentMode
                    + ", speakerOutput: " + speakerOutput);
        }
        if (mMuteMedia) {
            if (!speakerOutput && !mMuteForHeadset) {
                return RESTORE_VOLUME;
            }
            return silentMode ? MUTE_VOLUME : RESTORE_VOLUME;
        }
        return silentMode ? RESTORE_VOLUME : NO_CHANGE_VOLUME;
    }

    private boolean isSpeakerOutput() {
        return !mBtAudio && !mWiredAudio;
    }

    private void maybeUpdateMediaVolume() {
        maybeUpdateMediaVolume(mRingerMode);
    }

    private void maybeUpdateMediaVolume(int ringerMode) {
        final int action = getAction(ringerMode);
        if (DEBUG_AUDIO_SLIDER) {
            Slog.d(TAG, "maybeUpdateMediaVolume, action: " + actionToString(action));
        }
        if (action == MUTE_VOLUME) {
            mService.adjustStreamVolume(STREAM_MUSIC, ADJUST_MUTE, 0, CALLING_PACKAGE);
        } else if (action == RESTORE_VOLUME) {
            mService.adjustStreamVolume(STREAM_MUSIC, ADJUST_UNMUTE, 0, CALLING_PACKAGE);
        }
    }

    private void maybeSwitchRingerMode() {
        if (DEBUG_AUDIO_SLIDER) {
            Slog.d(TAG, "maybeSwitchRingerMode, mVibrateOnBluetooth: " + mVibrateOnBluetooth
                    + ", mInTmpRingerMode: " + mInTmpRingerMode
                    + ", mBtAudio: " + mBtAudio
                    + ", mRingerMode: " + mRingerMode);
        }
        if (mVibrateOnBluetooth && !mInTmpRingerMode && mBtAudio
                && mRingerMode == RINGER_MODE_NORMAL) {
            if (DEBUG_AUDIO_SLIDER) {
                Slog.d(TAG, "Set ringer mode to vibrate due to bluetooth connected / settings enabled");
            }
            setStateInternal(STATE_MIDDLE, false);
            mInTmpRingerMode = true;
        } else if (mVibrateOnBluetooth && mInTmpRingerMode && !mBtAudio
                && mRingerMode == RINGER_MODE_VIBRATE) {
            if (DEBUG_AUDIO_SLIDER) {
                Slog.d(TAG, "Restore ringer mode to normal due to bluetooth disconnected");
            }
            setStateInternal(STATE_BOTTOM, false);
        } else if (!mVibrateOnBluetooth && mInTmpRingerMode && mRingerMode == RINGER_MODE_VIBRATE) {
            if (DEBUG_AUDIO_SLIDER) {
                Slog.d(TAG, "Restore ringer mode to normal due to settings disabled");
            }
            setStateInternal(STATE_BOTTOM, false);
        }
    }

    private static String actionToString(int action) {
        switch (action) {
            case NO_CHANGE_VOLUME:
                return "NO_CHANGE";
            case MUTE_VOLUME:
                return "MUTE";
            case RESTORE_VOLUME:
                return "UNMUTE";
            default:
                return "UNKNOWN";
        }
    }

    private static int stateToRingerMode(int state) {
        switch (state) {
            case STATE_TOP:
                return RINGER_MODE_SILENT;
            case STATE_MIDDLE:
                return RINGER_MODE_VIBRATE;
            case STATE_BOTTOM:
                return RINGER_MODE_NORMAL;
            default:
                return -1;
        }
    }

    private final class AlertSliderHandler extends Handler {
        AlertSliderHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case MSG_DO_VIBRATE:
                    final VibrationExtInfo info = (VibrationExtInfo) message.obj;
                    mVibrator.vibrateExt(info);
                    break;
            }
        }
    }
}
