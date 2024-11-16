/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.server.vibrator;

import static android.provider.Settings.System.HAPTIC_FEEDBACK_ENABLED;

import static org.nameless.os.CustomVibrationAttributes.VIBRATION_ATTRIBUTES_BACK_GESTURE_DRAG;
import static org.nameless.os.CustomVibrationAttributes.VIBRATION_ATTRIBUTES_FACE_UNLOCK;
import static org.nameless.os.CustomVibrationAttributes.VIBRATION_ATTRIBUTES_FINGERPRINT_UNLOCK;
import static org.nameless.os.CustomVibrationAttributes.VIBRATION_ATTRIBUTES_MISC_SCENES;
import static org.nameless.os.CustomVibrationAttributes.VIBRATION_ATTRIBUTES_OFF_SCREEN_GESTURE;
import static org.nameless.os.CustomVibrationAttributes.VIBRATION_ATTRIBUTES_QS_TILE;
import static org.nameless.os.CustomVibrationAttributes.VIBRATION_ATTRIBUTES_SLIDER;
import static org.nameless.os.CustomVibrationAttributes.VIBRATION_ATTRIBUTES_SWITCH;
import static org.nameless.provider.SettingsExt.System.CUSTOM_HAPTIC_ON_BACK_GESTURE;
import static org.nameless.provider.SettingsExt.System.CUSTOM_HAPTIC_ON_FACE;
import static org.nameless.provider.SettingsExt.System.CUSTOM_HAPTIC_ON_FINGERPRINT;
import static org.nameless.provider.SettingsExt.System.CUSTOM_HAPTIC_ON_GESTURE_OFF_SCREEN;
import static org.nameless.provider.SettingsExt.System.CUSTOM_HAPTIC_ON_MISC_SCENES;
import static org.nameless.provider.SettingsExt.System.CUSTOM_HAPTIC_ON_QS_TILE;
import static org.nameless.provider.SettingsExt.System.CUSTOM_HAPTIC_ON_SLIDER;
import static org.nameless.provider.SettingsExt.System.CUSTOM_HAPTIC_ON_SWITCH;
import static org.nameless.provider.SettingsExt.System.FORCE_ENABLE_IME_HAPTIC;
import static org.nameless.provider.SettingsExt.System.IME_KEYBOARD_PRESS_EFFECT;
import static org.nameless.provider.SettingsExt.System.LOW_POWER_DISABLE_VIBRATION;
import static org.nameless.provider.SettingsExt.System.VIBRATOR_EXT_ALARM_CALL_STRENGTH_LEVEL;
import static org.nameless.provider.SettingsExt.System.VIBRATOR_EXT_HAPTIC_STRENGTH_LEVEL;
import static org.nameless.provider.SettingsExt.System.VIBRATOR_EXT_HAPTIC_STYLE;
import static org.nameless.provider.SettingsExt.System.VIBRATOR_EXT_NOTIFICAITON_STRENGTH_LEVEL;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.VibrationAttributes;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.Pair;

import java.util.ArrayList;

import org.nameless.content.IOnlineConfigurable;
import org.nameless.content.OnlineConfigManager;
import org.nameless.os.VibratorExtManager;

import vendor.nameless.hardware.vibratorExt.V1_0.LevelRange;
import vendor.nameless.hardware.vibratorExt.V1_0.Style;
import vendor.nameless.hardware.vibratorExt.V1_0.Type;

/** Controls all the custom settings related to vibration. */
public final class CustomVibrationSettings extends IOnlineConfigurable.Stub {

    private static final int SETTINGS_BACK_GESTURE_DRAG      = 1 << 0;
    private static final int SETTINGS_FACE                   = 1 << 1;
    private static final int SETTINGS_FINGERPRINT            = 1 << 2;
    private static final int SETTINGS_MISC_SCENES            = 1 << 3;
    private static final int SETTINGS_OFF_SCREEN_GESTURE     = 1 << 4;
    private static final int SETTINGS_QS_TILE                = 1 << 5;
    private static final int SETTINGS_SLIDER                 = 1 << 6;
    private static final int SETTINGS_SWITCH                 = 1 << 7;

    private static final ArrayMap<VibrationAttributes, Integer> AttributeToSettings;

    private static final ArrayMap<Integer, Pair<String, String>> STRENGTH_SETTINGS_DATE_MAP;

    private final VibratorExtManager mVibratorExtManager = VibratorExtManager.getInstance();

    private Context mContext;
    private SettingsContentObserver mSettingObserver;

    private int mSettings;
    private boolean mForceEnableIMEHaptic;
    private boolean mUseKeyboardEffect;
    private boolean mLowPowerDisableVibration;

    private static class InstanceHolder {
        private static CustomVibrationSettings INSTANCE = new CustomVibrationSettings();
    }

    public static CustomVibrationSettings getInstance() {
        return InstanceHolder.INSTANCE;
    }

    static {
        AttributeToSettings = new ArrayMap<>();
        AttributeToSettings.put(VIBRATION_ATTRIBUTES_BACK_GESTURE_DRAG, SETTINGS_BACK_GESTURE_DRAG);
        AttributeToSettings.put(VIBRATION_ATTRIBUTES_FACE_UNLOCK, SETTINGS_FACE);
        AttributeToSettings.put(VIBRATION_ATTRIBUTES_FINGERPRINT_UNLOCK, SETTINGS_FINGERPRINT);
        AttributeToSettings.put(VIBRATION_ATTRIBUTES_MISC_SCENES, SETTINGS_MISC_SCENES);
        AttributeToSettings.put(VIBRATION_ATTRIBUTES_OFF_SCREEN_GESTURE, SETTINGS_OFF_SCREEN_GESTURE);
        AttributeToSettings.put(VIBRATION_ATTRIBUTES_QS_TILE, SETTINGS_QS_TILE);
        AttributeToSettings.put(VIBRATION_ATTRIBUTES_SLIDER, SETTINGS_SLIDER);
        AttributeToSettings.put(VIBRATION_ATTRIBUTES_SWITCH, SETTINGS_SWITCH);

        STRENGTH_SETTINGS_DATE_MAP = new ArrayMap<>();
        STRENGTH_SETTINGS_DATE_MAP.put(Type.ALARM_CALL, new Pair(
            "persist.sys.nameless.vibrator.alarm_call.date",
            VIBRATOR_EXT_ALARM_CALL_STRENGTH_LEVEL
        ));
        STRENGTH_SETTINGS_DATE_MAP.put(Type.HAPTIC, new Pair(
            "persist.sys.nameless.vibrator.haptic.date",
            VIBRATOR_EXT_HAPTIC_STRENGTH_LEVEL
        ));
        STRENGTH_SETTINGS_DATE_MAP.put(Type.NOTIFICATION, new Pair(
            "persist.sys.nameless.vibrator.notification.date",
            VIBRATOR_EXT_NOTIFICAITON_STRENGTH_LEVEL
        ));
    }

    @Override
    public int getVersion() {
        return VibrationEffectAdapter.VERSION;
    }

    @Override
    public String getOnlineConfigUri() {
        return SystemProperties.get("persist.sys.nameless.uri.vibration");
    }

    @Override
    public String getSystemConfigPath() {
        return VibrationEffectAdapter.SYSTEM_CONFIG_FILE;
    }

    @Override
    public String getLocalConfigPath() {
        return VibrationEffectAdapter.LOCAL_CONFIG_FILE;
    }

    @Override
    public void onConfigUpdated() {
        // We can use local config here safely because this is called after verification.
        VibrationEffectAdapter.initEffectMap(VibrationEffectAdapter.LOCAL_CONFIG_FILE);
    }

    public void init(Context context, Handler handler) {
        mContext = context;
        mSettingObserver = new SettingsContentObserver(handler);
    }

    public void onSystemReady() {
        registerSettingsObserver(Settings.System.getUriFor(CUSTOM_HAPTIC_ON_BACK_GESTURE));
        registerSettingsObserver(Settings.System.getUriFor(CUSTOM_HAPTIC_ON_FACE));
        registerSettingsObserver(Settings.System.getUriFor(CUSTOM_HAPTIC_ON_FINGERPRINT));
        registerSettingsObserver(Settings.System.getUriFor(CUSTOM_HAPTIC_ON_GESTURE_OFF_SCREEN));
        registerSettingsObserver(Settings.System.getUriFor(CUSTOM_HAPTIC_ON_MISC_SCENES));
        registerSettingsObserver(Settings.System.getUriFor(CUSTOM_HAPTIC_ON_QS_TILE));
        registerSettingsObserver(Settings.System.getUriFor(CUSTOM_HAPTIC_ON_SLIDER));
        registerSettingsObserver(Settings.System.getUriFor(CUSTOM_HAPTIC_ON_SWITCH));
        registerSettingsObserver(Settings.System.getUriFor(HAPTIC_FEEDBACK_ENABLED));
        registerSettingsObserver(Settings.System.getUriFor(FORCE_ENABLE_IME_HAPTIC));
        registerSettingsObserver(Settings.System.getUriFor(IME_KEYBOARD_PRESS_EFFECT));
        registerSettingsObserver(Settings.System.getUriFor(LOW_POWER_DISABLE_VIBRATION));
        registerSettingsObserver(Settings.System.getUriFor(VIBRATOR_EXT_ALARM_CALL_STRENGTH_LEVEL));
        registerSettingsObserver(Settings.System.getUriFor(VIBRATOR_EXT_HAPTIC_STRENGTH_LEVEL));
        registerSettingsObserver(Settings.System.getUriFor(VIBRATOR_EXT_NOTIFICAITON_STRENGTH_LEVEL));
        registerSettingsObserver(Settings.System.getUriFor(VIBRATOR_EXT_HAPTIC_STYLE));

        VibrationEffectAdapter.initEffectMap(VibrationEffectAdapter.compareConfigTimestamp());

        updateSettings();
        updateForceEnableIMEHaptic();
        updateKeyboardEffect();
        updateLowPowerDisableVibration();

        updateHapticStyle();
        for (int type : mVibratorExtManager.getValidVibrationTypes()) {
            if (!maybeResetStrengthLevel(type)) {
                updateStrengthLevel(type);
            }
        }
        mVibratorExtManager.initVibrator();

        mContext.getSystemService(OnlineConfigManager.class).registerOnlineConfigurable(this);
    }

    public void onUserSwitched() {
        updateSettings();
        updateForceEnableIMEHaptic();
        updateKeyboardEffect();
        updateLowPowerDisableVibration();

        updateHapticStyle();
        for (int type : mVibratorExtManager.getValidVibrationTypes()) {
            updateStrengthLevel(type);
        }
    }

    private final class SettingsContentObserver extends ContentObserver {
        SettingsContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            switch (uri.getLastPathSegment()) {
                case VIBRATOR_EXT_ALARM_CALL_STRENGTH_LEVEL:
                    updateStrengthLevel(Type.ALARM_CALL);
                    break;
                case VIBRATOR_EXT_HAPTIC_STRENGTH_LEVEL:
                    updateStrengthLevel(Type.HAPTIC);
                    break;
                case VIBRATOR_EXT_NOTIFICAITON_STRENGTH_LEVEL:
                    updateStrengthLevel(Type.NOTIFICATION);
                    break;
                case VIBRATOR_EXT_HAPTIC_STYLE:
                    updateHapticStyle();
                    break;
                case FORCE_ENABLE_IME_HAPTIC:
                    updateForceEnableIMEHaptic();
                    break;
                case IME_KEYBOARD_PRESS_EFFECT:
                    updateKeyboardEffect();
                    break;
                case LOW_POWER_DISABLE_VIBRATION:
                    updateLowPowerDisableVibration();
                    break;
                default:
                    updateSettings();
                    break;
            }
        }
    }

    private void updateSettings() {
        final boolean touchHaptic = loadBooleanSetting(HAPTIC_FEEDBACK_ENABLED);
        final boolean backGesture = loadBooleanSetting(CUSTOM_HAPTIC_ON_BACK_GESTURE);
        final boolean face = loadBooleanSetting(CUSTOM_HAPTIC_ON_FACE);
        final boolean fingerprint = loadBooleanSetting(CUSTOM_HAPTIC_ON_FINGERPRINT);
        final boolean miscScenes = loadBooleanSetting(CUSTOM_HAPTIC_ON_MISC_SCENES);
        final boolean offScreenGesture = loadBooleanSetting(CUSTOM_HAPTIC_ON_GESTURE_OFF_SCREEN);
        final boolean qsTiles = loadBooleanSetting(CUSTOM_HAPTIC_ON_QS_TILE);
        final boolean sliders = loadBooleanSetting(CUSTOM_HAPTIC_ON_SLIDER);
        final boolean switches = loadBooleanSetting(CUSTOM_HAPTIC_ON_SWITCH);

        mSettings = 0;

        if (face) {
            mSettings |= SETTINGS_FACE;
        }
        if (fingerprint) {
            mSettings |= SETTINGS_FINGERPRINT;
        }
        if (touchHaptic) {
            if (backGesture) {
                mSettings |= SETTINGS_BACK_GESTURE_DRAG;
            }
            if (miscScenes) {
                mSettings |= SETTINGS_MISC_SCENES;
            }
            if (offScreenGesture) {
                mSettings |= SETTINGS_OFF_SCREEN_GESTURE;
            }
            if (qsTiles) {
                mSettings |= SETTINGS_QS_TILE;
            }
            if (sliders) {
                mSettings |= SETTINGS_SLIDER;
            }
            if (switches) {
                mSettings |= SETTINGS_SWITCH;
            }
        }
    }

    private void updateForceEnableIMEHaptic() {
        mForceEnableIMEHaptic = loadSystemSetting(FORCE_ENABLE_IME_HAPTIC, 0) == 1;
    }

    private void updateKeyboardEffect() {
        mUseKeyboardEffect = loadSystemSetting(IME_KEYBOARD_PRESS_EFFECT, 0) == 1;
    }

    private void updateLowPowerDisableVibration() {
        mLowPowerDisableVibration = loadBooleanSetting(LOW_POWER_DISABLE_VIBRATION);
    }

    private boolean maybeResetStrengthLevel(int type) {
        final Pair<String, String> p = STRENGTH_SETTINGS_DATE_MAP.getOrDefault(type, null);
        if (p == null) {
            return false;
        }
        final String prop = p.first;
        final long currentDate = SystemProperties.getLong(prop, -1L);
        if (currentDate <= 0L) {
            return false;
        }
        final String lastProp = prop + "_last";
        final long lastDate = SystemProperties.getLong(lastProp, -1L);
        if (currentDate <= lastDate) {
            return false;
        }
        final LevelRange range = mVibratorExtManager.getStrengthLevelRange(type);
        if (range == null) {
            return false;
        }
        SystemProperties.set(lastProp, String.valueOf(currentDate));
        putSystemSetting(p.second, range.defaultLevel);
        return true;
    }

    private void updateStrengthLevel(int type) {
        final LevelRange range = mVibratorExtManager.getStrengthLevelRange(type);
        if (range != null) {
            final int strengthLevel = loadSystemSetting(
                    mVibratorExtManager.vibrationTypeToSettings(type), range.defaultLevel);
            mVibratorExtManager.setStrengthLevel(type, strengthLevel);
        }
    }

    private void updateHapticStyle() {
        final int style = loadSystemSetting(VIBRATOR_EXT_HAPTIC_STYLE, Style.CRISP);
        if (mVibratorExtManager.getValidHapticStyles().contains(style)) {
            mVibratorExtManager.setHapticStyle(style);
        }
    }

    private boolean loadBooleanSetting(String settingKey) {
        return Settings.System.getIntForUser(mContext.getContentResolver(),
                settingKey, 1, UserHandle.USER_CURRENT) != 0;
    }

    private int loadSystemSetting(String settingName, int defaultValue) {
        return Settings.System.getIntForUser(mContext.getContentResolver(),
                settingName, defaultValue, UserHandle.USER_CURRENT);
    }

    private void putSystemSetting(String settingName, int value) {
        Settings.System.putIntForUser(mContext.getContentResolver(),
                settingName, value, UserHandle.USER_CURRENT);
    }

    private void registerSettingsObserver(Uri settingUri) {
        mContext.getContentResolver().registerContentObserver(
                settingUri, false, mSettingObserver,
                UserHandle.USER_ALL);
    }

    public boolean shouldVibrate(VibrationAttributes attribute) {
        if (AttributeToSettings.containsKey(attribute)) {
            if ((mSettings & AttributeToSettings.get(attribute)) == 0) {
                return false;
            }
        }
        return true;
    }

    public boolean shouldForceEnableIMEHaptic() {
        return mForceEnableIMEHaptic;
    }

    boolean isKeyboardEffectEnabled() {
        return mUseKeyboardEffect;
    }

    public boolean isLowPowerDisableVibration() {
        return mLowPowerDisableVibration;
    }
}
