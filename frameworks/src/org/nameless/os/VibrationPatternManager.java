/*
 * Copyright (C) 2022-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.os;

import static vendor.nameless.hardware.vibratorExt.Effect.DURATION_ALARM_CALL;
import static vendor.nameless.hardware.vibratorExt.Effect.DURATION_NOTIFICATION;
import static vendor.nameless.hardware.vibratorExt.Effect.RAMP_DOWN;
import static vendor.nameless.hardware.vibratorExt.Effect.RINGTONE_WALTZ;

import android.os.VibrationEffect;
import android.util.ArrayMap;
import android.util.ArraySet;

import com.android.internal.R;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Set;

/** @hide */
public class VibrationPatternManager {

    private static final VibratorExtManager sVibratorExtManager = VibratorExtManager.getInstance();

    private static final ArrayList<Integer> NAME_ID_LIST_NOTIFICATION;
    private static final ArrayList<long[]> TIMINGS_LIST_NOTIFICATION;
    private static final LinkedHashMap<Integer, Pattern> PATTERN_MAP_NOTIFICATION;

    private static final ArrayList<Integer> NAME_ID_LIST_RINGTONE;
    private static final ArrayList<long[]> TIMINGS_LIST_RINGTONE;
    private static final LinkedHashMap<Integer, Pattern> PATTERN_MAP_RINGTONE;
    private static final ArraySet<Integer> NATIVE_SUPPORT_RINGTONE_EFFECT_IDX;

    private static final ArrayMap<Integer, int[]> TIMINGS_CUSTOM_AMPLITUDE;

    private static final int RTP_START_INDEX_RINGTONE;

    public static final long RTP_START_DURATION_RINGTONE = 100000L;
    public static final long RTP_END_DURATION_RINGTONE = 110000L;
    public static final long RTP_RINGTONE_INTERVAL = 1000L;

    private static final boolean RAMP_DOWN_SUPPORTED = sVibratorExtManager.isEffectSupported(RAMP_DOWN);

    private static final long RAMP_DOWN_MIN_DURATION = 500L;
    private static final long RAMP_DOWN_DURATION_OFFSET = 1L;

    static {
        TIMINGS_CUSTOM_AMPLITUDE = new ArrayMap<>();
        TIMINGS_CUSTOM_AMPLITUDE.put(R.string.vibrationPattern_ringtone_drums, new int[] {
                0, 80, 0, 100, 0, 220, 0, 255, 0, 80, 0, 100, 0, 220, 0, 255, 0});

        NAME_ID_LIST_NOTIFICATION = new ArrayList<>();
        NAME_ID_LIST_NOTIFICATION.add(R.string.vibrationPattern_notification_dz_dz);
        NAME_ID_LIST_NOTIFICATION.add(R.string.vibrationPattern_notification_mm_dz);
        NAME_ID_LIST_NOTIFICATION.add(R.string.vibrationPattern_notification_da_mm_da);
        NAME_ID_LIST_NOTIFICATION.add(R.string.vibrationPattern_notification_da_da_da_mm);
        NAME_ID_LIST_NOTIFICATION.add(R.string.vibrationPattern_notification_dz);
        if (RAMP_DOWN_SUPPORTED) {
            NAME_ID_LIST_NOTIFICATION.add(R.string.vibrationPattern_notification_bell);
            NAME_ID_LIST_NOTIFICATION.add(R.string.vibrationPattern_notification_kick);
            NAME_ID_LIST_NOTIFICATION.add(R.string.vibrationPattern_notification_strings);
            NAME_ID_LIST_NOTIFICATION.add(R.string.vibrationPattern_notification_water_drop);
        } else {
            NAME_ID_LIST_NOTIFICATION.add(R.string.vibrationPattern_notification_strings);
            NAME_ID_LIST_NOTIFICATION.add(R.string.vibrationPattern_notification_water_drop);
        }

        TIMINGS_LIST_NOTIFICATION = new ArrayList<>();
        TIMINGS_LIST_NOTIFICATION.add(new long[] {0, 100, 150, 100});
        TIMINGS_LIST_NOTIFICATION.add(new long[] {0, 250, 80, 80});
        TIMINGS_LIST_NOTIFICATION.add(new long[] {0, 50, 80, 150, 180, 50});
        TIMINGS_LIST_NOTIFICATION.add(new long[] {0, 50, 60, 50, 60, 50, 180, 150});
        TIMINGS_LIST_NOTIFICATION.add(new long[] {0, 150});
        if (RAMP_DOWN_SUPPORTED) {
            TIMINGS_LIST_NOTIFICATION.add(new long[] {0, 1000 + RAMP_DOWN_DURATION_OFFSET});
            TIMINGS_LIST_NOTIFICATION.add(new long[] {0, 100, 150, 100, 50, 700 + RAMP_DOWN_DURATION_OFFSET});
            TIMINGS_LIST_NOTIFICATION.add(new long[] {0, 50, 50, 50, 50, 100, 130, 100, 130, 100, 130, 50, 30, 700 + RAMP_DOWN_DURATION_OFFSET});
            TIMINGS_LIST_NOTIFICATION.add(new long[] {0, 50, 30, 700 + RAMP_DOWN_DURATION_OFFSET});
        } else {
            TIMINGS_LIST_NOTIFICATION.add(new long[] {0, 50, 50, 50, 50, 100, 130, 100, 130, 100, 130, 50, 30, 200});
            TIMINGS_LIST_NOTIFICATION.add(new long[] {0, 50, 30, 150});
        }

        PATTERN_MAP_NOTIFICATION = new LinkedHashMap<>();
        for (int i = 0; i < NAME_ID_LIST_NOTIFICATION.size(); ++i) {
            final int id = NAME_ID_LIST_NOTIFICATION.get(i);
            if (TIMINGS_CUSTOM_AMPLITUDE.containsKey(id)) {
                PATTERN_MAP_NOTIFICATION.put(id,
                        new Pattern(TIMINGS_LIST_NOTIFICATION.get(i), TIMINGS_CUSTOM_AMPLITUDE.get(id)));
            } else {
                PATTERN_MAP_NOTIFICATION.put(id,
                        new Pattern(TIMINGS_LIST_NOTIFICATION.get(i)));
            }
        }

        NAME_ID_LIST_RINGTONE = new ArrayList<>();
        NAME_ID_LIST_RINGTONE.add(R.string.vibrationPattern_ringtone_dzzz_da);
        NAME_ID_LIST_RINGTONE.add(R.string.vibrationPattern_ringtone_dzzz_dzzz);
        NAME_ID_LIST_RINGTONE.add(R.string.vibrationPattern_ringtone_mm_mm_mm);
        NAME_ID_LIST_RINGTONE.add(R.string.vibrationPattern_ringtone_da_da_dzzz);
        NAME_ID_LIST_RINGTONE.add(R.string.vibrationPattern_ringtone_da_dzzz_da);
        NAME_ID_LIST_RINGTONE.add(R.string.vibrationPattern_ringtone_drums);
        NAME_ID_LIST_RINGTONE.add(R.string.vibrationPattern_ringtone_raise);

        TIMINGS_LIST_RINGTONE = new ArrayList<>();
        TIMINGS_LIST_RINGTONE.add(new long[] {0,
            250, 120, 50, 200,
            250, 120, 50, 200,
            250, 120, 50, 200,
            250, 120, 50, 200,
            250, 120, 50, 1000,
        });
        TIMINGS_LIST_RINGTONE.add(new long[] {0, 1000, 1000});
        TIMINGS_LIST_RINGTONE.add(new long[] {0, 300, 400, 300, 400, 300, 1000});
        TIMINGS_LIST_RINGTONE.add(new long[] {0, 30, 80, 30, 80, 50, 180, 600, 1000});
        TIMINGS_LIST_RINGTONE.add(new long[] {0, 50, 200, 600, 150, 10, 1000});
        TIMINGS_LIST_RINGTONE.add(new long[] {0, 100, 100, 100, 100, 100, 100, 100, 150, 100, 100, 100, 100, 100, 100, 100, 1000});
        if (RAMP_DOWN_SUPPORTED) {
            TIMINGS_LIST_RINGTONE.add(new long[] {
                    0, 150, 100, 50, 80, 50, 80, 50, 200, 150, 100, 50, 80, 50, 80, 50, 250,
                    50, 80, 50, 80, 50, 80, 50, 200, 100, 120, 700 + RAMP_DOWN_DURATION_OFFSET, 1000});
        } else {
            TIMINGS_LIST_RINGTONE.add(new long[] {
                    0, 150, 100, 50, 80, 50, 80, 50, 200, 150, 100, 50, 80, 50, 80, 50, 250,
                    50, 80, 50, 80, 50, 80, 50, 200, 100, 120, 300, 1000});
        }

        PATTERN_MAP_RINGTONE = new LinkedHashMap<>();
        for (int i = 0; i < NAME_ID_LIST_RINGTONE.size(); ++i) {
            final int id = NAME_ID_LIST_RINGTONE.get(i);
            if (TIMINGS_CUSTOM_AMPLITUDE.containsKey(id)) {
                PATTERN_MAP_RINGTONE.put(NAME_ID_LIST_RINGTONE.get(i),
                        new Pattern(TIMINGS_LIST_RINGTONE.get(i), TIMINGS_CUSTOM_AMPLITUDE.get(id)));
            } else {
                PATTERN_MAP_RINGTONE.put(NAME_ID_LIST_RINGTONE.get(i),
                        new Pattern(TIMINGS_LIST_RINGTONE.get(i)));
            }
        }

        NATIVE_SUPPORT_RINGTONE_EFFECT_IDX = new ArraySet<>();
        RTP_START_INDEX_RINGTONE = NAME_ID_LIST_RINGTONE.size();

        int id = RINGTONE_WALTZ;
        if (!addRingtoneIfSupported(R.string.vibrationPattern_ringtone_waltz, id++)) {
            NAME_ID_LIST_RINGTONE.add(R.string.vibrationPattern_ringtone_waltz);
            PATTERN_MAP_RINGTONE.put(R.string.vibrationPattern_ringtone_waltz,
                    new Pattern(new long[] {
                            0, 200, 150, 50, 250, 50,
                            250, 200, 150, 50, 250, 50,
                            250, 200, 150, 50, 250, 50, 1000
                    }));
        }
        if (!addRingtoneIfSupported(R.string.vibrationPattern_ringtone_cut, id++)) {
            NAME_ID_LIST_RINGTONE.add(R.string.vibrationPattern_ringtone_cut);
            PATTERN_MAP_RINGTONE.put(R.string.vibrationPattern_ringtone_cut,
                    new Pattern(new long[] {
                            0, 120, 50, 120,
                            300, 120, 50, 120,
                            300, 120, 50, 120, 1000
                    }));
        }
        if (!addRingtoneIfSupported(R.string.vibrationPattern_ringtone_clock, id++)) {
            NAME_ID_LIST_RINGTONE.add(R.string.vibrationPattern_ringtone_clock);
            PATTERN_MAP_RINGTONE.put(R.string.vibrationPattern_ringtone_clock,
                    new Pattern(new long[] {
                            0, 150, 300, 60,
                            800, 150, 300, 60,
                            800, 150, 300, 60, 1500
                    }));
        }
        if (!addRingtoneIfSupported(R.string.vibrationPattern_ringtone_short, id++)) {
            NAME_ID_LIST_RINGTONE.add(R.string.vibrationPattern_ringtone_short);
            PATTERN_MAP_RINGTONE.put(R.string.vibrationPattern_ringtone_short,
                    new Pattern(new long[] {
                            0, 50, 20, 50, 20, 50, 20, 50, 20, 50,
                            20, 50, 20, 50, 20, 50, 20, 50, 20, 50,
                            20, 50, 20, 50, 20, 50, 20, 50, 20, 50,
                            20, 50, 1000
                    }));
        }
    }

    private static boolean addRingtoneIfSupported(int resId, int effectId) {
        if (sVibratorExtManager.isEffectSupported(effectId)) {
            NAME_ID_LIST_RINGTONE.add(resId);
            PATTERN_MAP_RINGTONE.put(resId, new Pattern(
                    new long[] {RTP_START_DURATION_RINGTONE + effectId}));
            NATIVE_SUPPORT_RINGTONE_EFFECT_IDX.add(NAME_ID_LIST_RINGTONE.indexOf(resId));
            return true;
        }
        return false;
    }

    public static boolean isRampDownEffect(int effectId, long duration) {
        return RAMP_DOWN_SUPPORTED &&
                (effectId == DURATION_ALARM_CALL || effectId == DURATION_NOTIFICATION) &&
                duration >= RAMP_DOWN_MIN_DURATION &&
                duration % 100 == RAMP_DOWN_DURATION_OFFSET;
    }

    public static Set<Integer> getPatternsNameIdSet(int vibrationType) {
        if (vibrationType == Type.NOTIFICATION) {
            return PATTERN_MAP_NOTIFICATION.keySet();
        }
        return PATTERN_MAP_RINGTONE.keySet();
    }

    public static int getPatternsSize(int vibrationType) {
        if (vibrationType == Type.NOTIFICATION) {
            return PATTERN_MAP_NOTIFICATION.size();
        }
        return PATTERN_MAP_RINGTONE.size();
    }

    public static VibrationEffect getPreviewVibrationFromNumber(int number, int vibrationType) {
        if (vibrationType == Type.NOTIFICATION) {
            return getVibrationFromNumber(number, vibrationType, false);
        }
        if (number >= getPatternsSize(vibrationType)) {
            number = 0;
        }
        if (number >= RTP_START_INDEX_RINGTONE &&
                NATIVE_SUPPORT_RINGTONE_EFFECT_IDX.contains(number)) {
            return getVibrationFromNumber(number, vibrationType, false);
        }
        final Pattern p = PATTERN_MAP_RINGTONE.get(NAME_ID_LIST_RINGTONE.get(number));
        final Pattern previewPattern = createPreviewPattern(p);
        return createWaveform(previewPattern.getTimings(), previewPattern.getAmplitudes(), false);
    }

    public static VibrationEffect getVibrationFromNumber(int number, int vibrationType) {
        return getVibrationFromNumber(number, vibrationType, vibrationType == Type.RINGTONE);
    }

    public static VibrationEffect getVibrationFromNumber(int number, int vibrationType, boolean insistent) {
        if (number >= getPatternsSize(vibrationType)) {
            number = 0;
        }
        if (number >= RTP_START_INDEX_RINGTONE &&
                NATIVE_SUPPORT_RINGTONE_EFFECT_IDX.contains(number)) {
            insistent = false;
        }
        final Pattern p;
        if (vibrationType == Type.NOTIFICATION) {
            p = PATTERN_MAP_NOTIFICATION.get(NAME_ID_LIST_NOTIFICATION.get(number));
        } else {
            p = PATTERN_MAP_RINGTONE.get(NAME_ID_LIST_RINGTONE.get(number));
        }
        return createWaveform(p.getTimings(), p.getAmplitudes(), insistent);
    }

    private static Pattern createPreviewPattern(Pattern p) {
        final int preLen = p.getTimings().length;
        final int len = (preLen - 2) * 2 + 2;
        long[] timings = new long[len];
        int[] amplitudes = new int[len];
        for (int i = 0; i < len; ++i) {
            if (i < preLen) {
                timings[i] = p.getTimings()[i];
                amplitudes[i] = p.getAmplitudes()[i];
            } else {
                timings[i] = p.getTimings()[i - preLen + 1];
                amplitudes[i] = p.getAmplitudes()[i - preLen + 1];
            }
        }
        return new Pattern(timings, amplitudes);
    }

    private static VibrationEffect createWaveform(long[] timings, int[] amplitudes, boolean insistent) {
        return VibrationEffect.createWaveform(timings, amplitudes, insistent ? 0 : -1);
    }

    public static class Type {
        public static final int NOTIFICATION = 0;
        public static final int RINGTONE = 1;
    }

    private static class Pattern {
        private long[] timings;
        private int[] amplitudes;

        public Pattern(long[] timings) {
            this(timings, createAmplitudeList(timings));
        }

        public Pattern(long[] timings, int[] amplitudes) {
            this.timings = new long[timings.length];
            for (int i = 0; i < timings.length; ++i) {
                this.timings[i] = timings[i];
            }
            this.amplitudes = new int[amplitudes.length];
            for (int i = 0; i < amplitudes.length; ++i) {
                this.amplitudes[i] = amplitudes[i];
            }
        }

        public long[] getTimings() {
            return timings;
        }

        public int[] getAmplitudes() {
            return amplitudes;
        }

        private static int[] createAmplitudeList(long[] timingsList) {
            int[] ret = new int[timingsList.length];
            if (timingsList.length == 1) {
                ret[0] = 255;
                return ret;
            }
            for (int i = 0; i < timingsList.length; ++i) {
                ret[i] = (i & 1) != 0 ? 255 : 0;
            }
            return ret;
        }
    }
}
