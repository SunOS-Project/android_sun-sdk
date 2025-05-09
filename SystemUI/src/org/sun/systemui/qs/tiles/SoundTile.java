/*
 * Copyright (C) 2016 The Android Open Source Project
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
package org.sun.systemui.qs.tiles;

import static vendor.sun.hardware.vibratorExt.Effect.ALERT_SLIDER_BOTTOM;
import static vendor.sun.hardware.vibratorExt.Effect.ALERT_SLIDER_MIDDLE;
import static vendor.sun.hardware.vibratorExt.Effect.DOUBLE_CLICK;
import static vendor.sun.hardware.vibratorExt.Effect.HEAVY_CLICK;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationAttributes;
import android.os.VibrationExtInfo;
import android.os.Vibrator;
import android.provider.Settings.Global;
import android.service.quicksettings.Tile;

import androidx.annotation.Nullable;

import com.android.internal.logging.MetricsLogger;

import com.android.systemui.animation.Expandable;
import com.android.systemui.dagger.qualifiers.Background;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.FalsingManager;
import com.android.systemui.plugins.qs.QSTile.BooleanState;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.QsEventLogger;
import com.android.systemui.qs.logging.QSLogger;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.res.R;

import javax.inject.Inject;

import org.sun.audio.AlertSliderManager;

public class SoundTile extends QSTileImpl<BooleanState> {

    public static final String TILE_SPEC = "sound";

    private static final VibrationAttributes VIBRATION_ATTRIBUTE =
            new VibrationAttributes.Builder(
                    VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH))
            .setFlags(VibrationAttributes.FLAG_BYPASS_USER_VIBRATION_INTENSITY_OFF)
            .build();

    private final AudioManager mAudioManager;
    private final Vibrator mVibrator;

    private boolean mListening = false;

    private BroadcastReceiver mReceiver;
    private IntentFilter mFilter;

    @Inject
    public SoundTile(
            QSHost host,
            QsEventLogger uiEventLogger,
            @Background Looper backgroundLooper,
            @Main Handler mainHandler,
            FalsingManager falsingManager,
            MetricsLogger metricsLogger,
            StatusBarStateController statusBarStateController,
            ActivityStarter activityStarter,
            QSLogger qsLogger
    ) {
        super(host, uiEventLogger, backgroundLooper, mainHandler, falsingManager,
                metricsLogger, statusBarStateController, activityStarter, qsLogger);
        mAudioManager = mContext.getSystemService(AudioManager.class);
        mVibrator = mContext.getSystemService(Vibrator.class);
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                refreshState();
            }
        };
    }

    @Override
    public BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    public void handleSetListening(boolean listening) {
        if (mAudioManager == null) {
            return;
        }
        if (mListening == listening) return;
        mListening = listening;
        if (listening) {
            final IntentFilter filter = new IntentFilter();
            filter.addAction(AudioManager.INTERNAL_RINGER_MODE_CHANGED_ACTION);
            mContext.registerReceiver(mReceiver, filter);
        } else {
            mContext.unregisterReceiver(mReceiver);
        }
    }

    @Override
    protected void handleClick(@Nullable Expandable expandable) {
        updateState();
    }

    @Override
    public void handleLongClick(@Nullable Expandable expandable) {
        mAudioManager.adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI);
    }

    @Override
    public Intent getLongClickIntent() {
        return null;
    }

    @Override
    protected boolean vibrateOnClick() {
        return false;
    }

    private void updateState() {
        int oldState = mAudioManager.getRingerModeInternal();
        int newState = oldState;
        switch (oldState) {
            case AudioManager.RINGER_MODE_NORMAL:
                newState = AudioManager.RINGER_MODE_VIBRATE;
                mAudioManager.setRingerModeInternal(newState);
                break;
            case AudioManager.RINGER_MODE_VIBRATE:
                newState = AudioManager.RINGER_MODE_SILENT;
                mAudioManager.setRingerModeInternal(newState);
                break;
            case AudioManager.RINGER_MODE_SILENT:
                newState = AudioManager.RINGER_MODE_NORMAL;
                mAudioManager.setRingerModeInternal(newState);
                break;
            default:
                break;
        }
        if (newState != AudioManager.RINGER_MODE_SILENT) {
            mVibrator.vibrateExt(new VibrationExtInfo.Builder()
                    .setEffectId(newState == AudioManager.RINGER_MODE_NORMAL ? ALERT_SLIDER_BOTTOM : ALERT_SLIDER_MIDDLE)
                    .setFallbackEffectId(newState == AudioManager.RINGER_MODE_NORMAL ? HEAVY_CLICK : DOUBLE_CLICK)
                    .setVibrationAttributes(VIBRATION_ATTRIBUTE)
                    .build());
        }
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_sound_label);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        if (mAudioManager == null) {
            return;
        }
        switch (mAudioManager.getRingerModeInternal()) {
            case AudioManager.RINGER_MODE_NORMAL:
                state.icon = ResourceIcon.get(R.drawable.ic_volume_ringer);
                state.label = mContext.getString(R.string.quick_settings_sound_ring);
                state.state = Tile.STATE_ACTIVE;
                break;
            case AudioManager.RINGER_MODE_VIBRATE:
                state.icon = ResourceIcon.get(R.drawable.ic_volume_ringer_vibrate);
                state.label = mContext.getString(R.string.quick_settings_sound_vibrate);
                state.state = Tile.STATE_ACTIVE;
                break;
            case AudioManager.RINGER_MODE_SILENT:
                state.icon = ResourceIcon.get(R.drawable.ic_volume_ringer_mute);
                state.label = mContext.getString(R.string.quick_settings_sound_mute);
                state.state = Tile.STATE_ACTIVE;
                break;
            default:
                break;
        }
    }

    @Override
    public int getMetricsCategory() {
        return 0;
    }
}
