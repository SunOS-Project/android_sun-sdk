/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.server.vibrator;

import static android.os.VibrationAttributes.USAGE_ALARM;
import static android.os.VibrationAttributes.USAGE_NOTIFICATION;
import static android.os.VibrationAttributes.USAGE_RINGTONE;
import static android.os.VibrationEffect.EFFECT_CLICK;

import static org.nameless.os.CustomVibrationAttributes.VIBRATION_ATTRIBUTES_PREVIEW_ALARM_CALL;
import static org.nameless.os.CustomVibrationAttributes.VIBRATION_ATTRIBUTES_PREVIEW_NOTIFICATION;
import static org.nameless.os.DebugConstants.DEBUG_VIBRATION_ADAPTER;
import static org.nameless.os.VibrationPatternManager.RTP_END_DURATION_RINGTONE;
import static org.nameless.os.VibrationPatternManager.RTP_RINGTONE_INTERVAL;
import static org.nameless.os.VibrationPatternManager.RTP_START_DURATION_RINGTONE;

import static vendor.nameless.hardware.vibratorExt.V1_0.Effect.DURATION_ALARM_CALL;
import static vendor.nameless.hardware.vibratorExt.V1_0.Effect.DURATION_NOTIFICATION;
import static vendor.nameless.hardware.vibratorExt.V1_0.Effect.DURATION_STRENGTH_LEVEL1;
import static vendor.nameless.hardware.vibratorExt.V1_0.Effect.DURATION_STRENGTH_LEVEL10;
import static vendor.nameless.hardware.vibratorExt.V1_0.Effect.KEYBOARD_PRESS;

import android.os.CombinedVibration;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.util.Log;
import android.util.Pair;
import android.util.Xml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;

import org.nameless.os.VibratorExtManager;
import org.xmlpull.v1.XmlPullParser;

public class VibrationEffectAdapter {

    private static final String TAG = "VibrationEffectAdapter";

    static final int VERSION = 2;

    static final String SYSTEM_CONFIG_FILE = "/system_ext/etc/vibration_effect_map.xml";
    static final String LOCAL_CONFIG_FILE = "/data/nameless_configs/vibration_effect_map.xml";

    private static final CombinedVibration MOCK_EFFECT_CLICK =
            CombinedVibration.createParallel(VibrationEffect.createPredefined(EFFECT_CLICK));

    private static final String KEY_CALCULATOR_ENHANCE = "calculatorEnhance";
    private static final String KEY_DURATION = "duration";
    private static final String KEY_DURATION_TO_EFFECT = "durationToEffect";
    private static final String KEY_EFFECT_ID = "effectId";
    private static final String KEY_INPUTMETHOD_ENHANCE = "inputmethodEnhance";
    private static final String KEY_MAX_DURATION = "max";
    private static final String KEY_PACKAGE_NAME = "package";
    private static final String KEY_RINGTONE_DURATION = "ringtoneDuration";

    // Convert all vibration with specific attribution to required effect
    private static final HashMap<VibrationAttributes, Integer> attributeToEffect;

    // Convert all vibration with specific usage to required effect
    private static final HashMap<Integer, Integer> usageToEffect;

    // Convert specific duration vibration to prebaked vibration effect with specific id
    private static final HashMap<String, HashMap<Long, Integer>> durationToEffectMap;

    // Inputmethod apps that need to calculate level according to vibration duration
    private static final HashMap<String, Long> inputmethodEnhanceMap;

    // Duration for each ringtone effects
    private static final HashMap<Integer, Long> ringtoneDurationMap;

    // Calculator apps that will convert to keyboard press effect if enabled
    private static final HashSet<String> calculatorEnhanceSet;

    private static final VibratorExtManager sVibratorExtManager = VibratorExtManager.getInstance();
    private static final boolean sVibratorExtSupported = sVibratorExtManager.isSupported();

    private static String sCachedIMEPackageName = "";
    private static long sCachedIMEDuration = -1;
    private static int sCachedIMEStrengthLevel = DURATION_STRENGTH_LEVEL1;

    private static final Object sLock = new Object();

    static {
        attributeToEffect = new HashMap<>();
        attributeToEffect.put(VIBRATION_ATTRIBUTES_PREVIEW_ALARM_CALL, DURATION_ALARM_CALL);
        attributeToEffect.put(VIBRATION_ATTRIBUTES_PREVIEW_NOTIFICATION, DURATION_NOTIFICATION);

        usageToEffect = new HashMap<>();
        usageToEffect.put(USAGE_ALARM, DURATION_ALARM_CALL);
        usageToEffect.put(USAGE_NOTIFICATION, DURATION_NOTIFICATION);
        usageToEffect.put(USAGE_RINGTONE, DURATION_ALARM_CALL);

        durationToEffectMap = new HashMap<>();
        inputmethodEnhanceMap = new HashMap<>();
        ringtoneDurationMap = new HashMap<>();
        calculatorEnhanceSet = new HashSet<>();
    }

    private static Pair<Integer, Long> getConfigInfo(String path) {
        try {
            FileReader fr = new FileReader(new File(path));
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(fr);
            int event = parser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                   if ("info".equals(parser.getName())) {
                        final String versionStr = parser.getAttributeValue(null, "version");
                        final String timestampStr = parser.getAttributeValue(null, "timestamp");
                        return new Pair<>(Integer.parseInt(versionStr), Long.parseLong(timestampStr));
                    }
                }
                event = parser.next();
            }
            fr.close();
        } catch (Exception e) {
            if (!(e instanceof FileNotFoundException)) {
                Log.e(TAG, "exception on get config info", e);
            }
        }
        return new Pair<>(Integer.MAX_VALUE, -1L);
    }

    static String compareConfigTimestamp() {
        final Pair<Integer, Long> systemConfigInfo = getConfigInfo(SYSTEM_CONFIG_FILE);
        final Pair<Integer, Long> localConfigInfo = getConfigInfo(LOCAL_CONFIG_FILE);

        // Online config requires higher framework version. Fallback to system config.
        if (localConfigInfo.first > VERSION) {
            return SYSTEM_CONFIG_FILE;
        }

        return localConfigInfo.second > systemConfigInfo.second ? LOCAL_CONFIG_FILE : SYSTEM_CONFIG_FILE;
    }

    static void initEffectMap(String path) {
        synchronized (sLock) {
            if (!sVibratorExtSupported) {
                return;
            }

            durationToEffectMap.clear();
            inputmethodEnhanceMap.clear();
            ringtoneDurationMap.clear();
            calculatorEnhanceSet.clear();

            String packageName;
            long maxDuration;
            long duration;
            int effectId;
            try {
                FileReader fr = new FileReader(new File(path));
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(fr);
                int event = parser.getEventType();
                while (event != XmlPullParser.END_DOCUMENT) {
                    if (event == XmlPullParser.START_TAG) {
                        switch (parser.getName()) {
                            case KEY_INPUTMETHOD_ENHANCE:
                                packageName = parser.getAttributeValue(null, KEY_PACKAGE_NAME);
                                maxDuration = Long.parseLong(parser.getAttributeValue(null, KEY_MAX_DURATION));
                                inputmethodEnhanceMap.put(packageName, maxDuration);
                                if (DEBUG_VIBRATION_ADAPTER) {
                                    Log.d(TAG, "Added inputmethod: " + packageName + ", maxDuration: " + maxDuration);
                                }
                                break;
                            case KEY_CALCULATOR_ENHANCE:
                                packageName = parser.getAttributeValue(null, KEY_PACKAGE_NAME);
                                calculatorEnhanceSet.add(packageName);
                                if (DEBUG_VIBRATION_ADAPTER) {
                                    Log.d(TAG, "Added calculator: " + packageName);
                                }
                                break;
                            case KEY_DURATION_TO_EFFECT:
                                packageName = parser.getAttributeValue(null, KEY_PACKAGE_NAME);
                                duration = Long.parseLong(parser.getAttributeValue(null, KEY_DURATION));
                                effectId = Integer.parseInt(parser.getAttributeValue(null, KEY_EFFECT_ID));
                                if (!sVibratorExtManager.isEffectSupported(effectId)) {
                                    break;
                                }
                                if (!durationToEffectMap.containsKey(packageName)) {
                                    durationToEffectMap.put(packageName, new HashMap<>());
                                }
                                durationToEffectMap.get(packageName).put(duration, effectId);
                                if (DEBUG_VIBRATION_ADAPTER) {
                                    Log.d(TAG, "Added durationToEffectId: " + packageName +
                                            ", duration: " + duration + ", effectId: " + effectId);
                                }
                                break;
                            case KEY_RINGTONE_DURATION:
                                effectId = Integer.parseInt(parser.getAttributeValue(null, KEY_EFFECT_ID));
                                duration = Long.parseLong(parser.getAttributeValue(null, KEY_DURATION));
                                ringtoneDurationMap.put(effectId, duration);
                                if (DEBUG_VIBRATION_ADAPTER) {
                                    Log.d(TAG, "Added ringtoneEffect: effectId: " +
                                            effectId + ", duration: " + duration);
                                }
                                break;
                        }
                    }
                    event = parser.next();
                }
                fr.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static CustomEffect getCustomEffect(VibrationAttributes attributes, String opPkg,
            CombinedVibration effect, String reason) {
        synchronized (sLock) {
            CustomEffect ret = new CustomEffect(effect, -1, inputmethodEnhanceMap.containsKey(opPkg));
            if (!sVibratorExtSupported) {
                return ret;
            }

            final long duration = effect.getDuration();

            if (DEBUG_VIBRATION_ADAPTER) {
                StringBuilder sb = new StringBuilder();
                sb.append("packageName: ").append(opPkg)
                        .append(", effect: ").append(effect.toString())
                        .append(", attributes: ").append(attributes.toString())
                        .append(", duration: ").append("" + duration);
                if (reason != null) {
                    sb.append(", reason: ").append(reason);
                }
                Log.d(TAG, sb.toString());
            }

            if (duration >= RTP_START_DURATION_RINGTONE &&
                    duration <= RTP_END_DURATION_RINGTONE &&
                    isRingtone(attributes)) {
                ret.customEffectId = (int) (duration - RTP_START_DURATION_RINGTONE);
                if (ringtoneDurationMap.containsKey(ret.customEffectId)) {
                    ret.combinedEffect = obtainRtpRingtoneVibration(ret.customEffectId, isPreviewRingtone(attributes));
                }
                return ret;
            }

            if (attributeToEffect.containsKey(attributes)) {
                ret.customEffectId = attributeToEffect.get(attributes);
                return ret;
            }

            if (ret.isIME) {
                if (CustomVibrationSettings.getInstance().isKeyboardEffectEnabled()) {
                    ret.combinedEffect = MOCK_EFFECT_CLICK;
                    ret.customEffectId = KEYBOARD_PRESS;
                    return ret;
                } else if (duration > 0) {
                    ret.combinedEffect = obtainDurationVibration(duration);
                    ret.customEffectId = calculateIMEHapticLevel(opPkg, duration);
                    return ret;
                }
            }

            if (calculatorEnhanceSet.contains(opPkg)) {
                if (CustomVibrationSettings.getInstance().isKeyboardEffectEnabled()) {
                    ret.combinedEffect = MOCK_EFFECT_CLICK;
                    ret.customEffectId = KEYBOARD_PRESS;
                    return ret;
                }
            }

            if (duration <= 0) {
                return ret;
            }

            if (usageToEffect.containsKey(attributes.getUsage())) {
                ret.customEffectId = usageToEffect.get(attributes.getUsage());
                return ret;
            }

            if (durationToEffectMap.containsKey(opPkg)) {
                ret.customEffectId = durationToEffectMap.get(opPkg).getOrDefault(duration, -1);
                if (ret.customEffectId != -1) {
                    ret.combinedEffect = MOCK_EFFECT_CLICK;
                    return ret;
                }
            }

            return ret;
        }
    }

    public static boolean isIMEPackage(String opPkg) {
        synchronized (sLock) {
            return inputmethodEnhanceMap.containsKey(opPkg);
        }
    }

    private static boolean isPreviewRingtone(VibrationAttributes attributes) {
        return attributes.equals(VIBRATION_ATTRIBUTES_PREVIEW_ALARM_CALL);
    }

    private static boolean isRingtone(VibrationAttributes attributes) {
        return attributes.equals(VIBRATION_ATTRIBUTES_PREVIEW_ALARM_CALL) ||
                attributes.getUsage() == USAGE_RINGTONE;
    }

    private static int calculateIMEHapticLevel(String opPkg, long duration) {
        if (sCachedIMEPackageName.equals(opPkg) && sCachedIMEDuration == duration) {
            return sCachedIMEStrengthLevel;
        }
        final long maxDuration = inputmethodEnhanceMap.get(opPkg);
        final float interval = (float) maxDuration / 10;
        final int level = (int) (duration / interval);
        final int ret = Math.min(DURATION_STRENGTH_LEVEL1 + level, DURATION_STRENGTH_LEVEL10);
        sCachedIMEPackageName = opPkg;
        sCachedIMEDuration = duration;
        sCachedIMEStrengthLevel = ret;
        return ret;
    }

    private static CombinedVibration obtainDurationVibration(long duration) {
        return CombinedVibration.createParallel(
                VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE));
    }

    private static CombinedVibration obtainRtpRingtoneVibration(int effectId, boolean preview) {
        if (preview) {
            return obtainDurationVibration(ringtoneDurationMap.get(effectId));
        }
        return CombinedVibration.createParallel(
                VibrationEffect.createWaveform(new long[] {0, ringtoneDurationMap.get(effectId), RTP_RINGTONE_INTERVAL},
                new int[] {0, 255, 0}, 0));
    }

    public static class CustomEffect {
        public CombinedVibration combinedEffect;
        public int customEffectId;
        public boolean isIME;

        public CustomEffect(CombinedVibration effect, int id, boolean ime) {
            combinedEffect = effect;
            customEffectId = id;
            isIME = ime;
        }
    }
}
