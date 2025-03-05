package com.android.server.vibrator;

import static org.nameless.os.DebugConstants.DEBUG_RICHTAP;

import android.content.Context;
import android.hardware.vibrator.V1_0.EffectStrength;
import android.hardware.vibrator.IVibrator;
import android.os.Binder;
import android.os.CombinedVibration;
import android.os.HapticPlayer;
import android.os.IBinder;
import android.os.RichTapVibrationEffect;
import android.os.ServiceManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.telephony.TelephonyManager;
import android.util.Slog;

import vendor.aac.hardware.richtap.vibrator.IRichtapVibrator;
import vendor.aac.hardware.richtap.vibrator.IRichtapCallback;

public class RichTapVibratorService {

    private static final String TAG = "RichTapVibratorService";

    public static final String ACTION_CHANGE_MODE = "richtap_change_mode";

    private static final int SDK_HAL_NEW_FORMAT_DATA_VERSION = 0x02;

    private final IRichtapCallback mCallback;
    private volatile IRichtapVibrator sRichtapVibratorService = null;
    private VibHalDeathRecipient mHalDeathLinker = null;

    static final SenderId mCurrentSenderId = new SenderId(0, 0);

    private boolean mLastRichTapEffect = false;

    public enum HapticParamType {
        HAPTIC_DRC(0x01);

        private int type;

        private HapticParamType(int type) {
            this.type = type;
        }

        public int getValue() {
            return type;
        }
    }

    private IRichtapVibrator getRichtapService() {
        synchronized (RichTapVibratorService.class) {
            if (sRichtapVibratorService == null) {
                String vibratorDescriptor = "android$hardware$vibrator$IVibrator".replace('$', '.') + "/default";
                Slog.d(TAG, "vibratorDescriptor: " + vibratorDescriptor);
                IVibrator vibratorHalService = IVibrator.Stub
                        .asInterface(ServiceManager.getService(vibratorDescriptor));
                if (vibratorHalService == null) {
                    Slog.d(TAG, "can not get hal service");
                    return null;
                }
                Slog.d(TAG, "vibratorHalService: " + vibratorHalService);
                try {
                    Slog.d(TAG, "Capabilities: " + vibratorHalService.getCapabilities());
                } catch (Exception e) {
                    Slog.d(TAG, "getCapabilities failed", e);
                }

                IBinder binder = null;
                try {
                    binder = vibratorHalService.asBinder().getExtension();
                    if (binder != null) {
                        sRichtapVibratorService = IRichtapVibrator.Stub.asInterface(Binder.allowBlocking(binder));
                        mHalDeathLinker = new VibHalDeathRecipient(this);
                        mCurrentSenderId.reset();
                        binder.linkToDeath(mHalDeathLinker, 0);
                    } else {
                        sRichtapVibratorService = null;
                        Slog.e(TAG, "getExtension == null");
                    }
                } catch (Exception e) {
                    binder = null;
                    Slog.e(TAG, "getExtension failed", e);
                }
            }
        }
        return sRichtapVibratorService;
    }

    RichTapVibratorService(IRichtapCallback callback) {
        mCallback = callback;
    }

    /*
     * dispose call state, if not idle, should stop richtap effect loop.
     * return false if call state is not in offhook or ringing
     * return true if not idle, and must stop richtap effect loop.
     */
    public boolean disposeTelephonyCallState(Context context) {
        boolean calling = false;
        TelephonyManager telephonyManager = context.getSystemService(TelephonyManager.class);
        if (TelephonyManager.CALL_STATE_OFFHOOK == telephonyManager.getCallState()
                || TelephonyManager.CALL_STATE_RINGING == telephonyManager.getCallState()) {
            calling = true;
        }
        if (calling) {
            Slog.i(TAG, "current is calling state, stop richtap effect loop");
            richTapVibratorStop(true);
        }
        return calling;
    }

    public boolean disposeRichtapEffectParams(CombinedVibration combEffect) {
        if (!(combEffect instanceof CombinedVibration.Mono)) {
            return false;
        }
        VibrationEffect effect = ((CombinedVibration.Mono) combEffect).getEffect();
        if (effect instanceof RichTapVibrationEffect.PatternHeParameter) {
            RichTapVibrationEffect.PatternHeParameter param = (RichTapVibrationEffect.PatternHeParameter) effect;
            int interval = param.getInterval();
            int amplitude = param.getAmplitude();
            int freq = param.getFreq();
            if (DEBUG_RICHTAP) {
                Slog.d(TAG, "recive data, interval: " + interval + ", amplitude: " + amplitude + ", freq: " + freq);
            }
            try {
                IRichtapVibrator service = getRichtapService();
                if (service != null) {
                    if (DEBUG_RICHTAP) {
                        Slog.d(TAG, "aac richtap performHeParam");
                    }
                    mLastRichTapEffect = true;
                    service.performHeParam(interval, amplitude, freq, mCallback);
                }
            } catch (Exception e) {
                Slog.e(TAG, "aac richtap performHeParam failed", e);
            }
            return true;
        } else if (effect instanceof RichTapVibrationEffect.HapticParameter) {
            RichTapVibrationEffect.HapticParameter parameter = (RichTapVibrationEffect.HapticParameter) effect;
            int[] param = parameter.getParam();
            int length = parameter.getLength();
            if (DEBUG_RICHTAP) {
                Slog.d(TAG, "receive HapticParameter: " + parameter.toString());
            }
            setHapticParam(param, length);
            return true;
        }
        if (DEBUG_RICHTAP) {
            Slog.d(TAG, "none richtap effect, do nothing");
        }
        return false;
    }

    public void richTapVibratorOn(long millis) {
        try {
            IRichtapVibrator service = getRichtapService();
            if (service != null) {
                if (DEBUG_RICHTAP) {
                    Slog.d(TAG, "aac richtap doVibratorOn");
                }
                mLastRichTapEffect = true;
                service.on((int) millis, mCallback);
            }
        } catch (Exception e) {
            Slog.e(TAG, "aac richtap doVibratorOn failed", e);
        }
    }

    public void richTapVibratorOff() {
        try {
            IRichtapVibrator service = getRichtapService();
            if (service != null) {
                if (DEBUG_RICHTAP) {
                    Slog.d(TAG, "aac richtap doVibratorOff");
                }
                service.off(mCallback);
                mLastRichTapEffect = false;
            }
        } catch (Exception e) {
            Slog.e(TAG, "aac richtap doVibratorOff failed", e);
        }
    }

    private void setHapticParam(int[] data, int length) {
        try {
            IRichtapVibrator service = getRichtapService();
            if (service != null) {
                if (DEBUG_RICHTAP) {
                    Slog.d(TAG, "aac richtap setHapticParam, data length: " + length);
                }
                mLastRichTapEffect = true;
                service.setHapticParam(data, length, mCallback);
            }
        } catch (Exception e) {
            Slog.e(TAG, "aac richtap setHapticParam failed", e);
        }
    }

    public void richTapSetVibrationMode(int mode) {
        if (DEBUG_RICHTAP) {
            Slog.d(TAG, "richtap-mode, richTapSetVibrationMode, mode: " + mode);
        }
        // stop all vibrations first
        richTapVibratorStop(true);
        int[] param = new int[] { HapticParamType.HAPTIC_DRC.getValue(), mode };
        setHapticParam(param, param.length);
    }

    public void richTapVibratorSetAmplitude(int amplitude) {
        try {
            IRichtapVibrator service = getRichtapService();
            if (service != null) {
                if (DEBUG_RICHTAP) {
                    Slog.d(TAG, "aac richtap doVibratorSetAmplitude");
                }
                mLastRichTapEffect = true;
                service.setAmplitude((int) (amplitude), mCallback);
            }
        } catch (Exception e) {
            Slog.e(TAG, "aac richtap doVibratorSetAmplitude failed", e);
        }
    }

    public int richTapVibratorPerform(int id, byte scale) {
        int timeout = 0;
        try {
            IRichtapVibrator service = getRichtapService();
            if (service != null) {
                if (DEBUG_RICHTAP) {
                    Slog.d(TAG, "perform richtap vibrator");
                }
                mLastRichTapEffect = true;
                timeout = service.perform(id, scale, mCallback);
                Slog.d(TAG, "aac richtap perform timeout: " + timeout);
                return timeout;

            }
        } catch (Exception e) {
            Slog.e(TAG, "aac richtap perform failed", e);
        }
        return timeout;
    }

    public int getRichTapPrebakStrength(int effectStrength) {
        int strength = 0;
        switch (effectStrength) {
            case EffectStrength.LIGHT:
                strength = 69;
                break;
            case EffectStrength.MEDIUM:
                strength = 89;
                break;
            case EffectStrength.STRONG:
                strength = 99;
                break;
            default:
                if (DEBUG_RICHTAP) {
                    Slog.d(TAG, "wrong Effect Strength!");
                }
                break;
        }
        return strength;
    }

    public void richTapVibratorOnEnvelope(int[] relativeTime, int[] scaleArr, int[] freqArr, boolean steepMode,
            int amplitude) {
        int[] params = new int[12];
        for (int i = 0; i < relativeTime.length; i++) {
            params[i * 3] = relativeTime[i];
            params[i * 3 + 1] = scaleArr[i];
            params[i * 3 + 2] = freqArr[i];
            String temp = String.format("relativeTime, scale, freq = { %d, %d, %d }", params[i * 3], params[i * 3 + 1],
                    params[i * 3 + 2]);
            if (DEBUG_RICHTAP) {
                Slog.d(TAG, temp);
            }
        }
        if (DEBUG_RICHTAP) {
            Slog.d(TAG, "vibrator perform envelope");
        }
        richTapVibratorSetAmplitude(amplitude);
        try {
            IRichtapVibrator service = getRichtapService();
            if (service != null) {
                if (DEBUG_RICHTAP) {
                    Slog.d(TAG, "aac richtap performEnvelope");
                }
                mLastRichTapEffect = true;
                service.performEnvelope(params, steepMode, mCallback);
            }
        } catch (Exception e) {
            Slog.e(TAG, "aac richtap performEnvelope failed", e);
        }
    }

    public void richTapVibratorOnPatternHe(VibrationEffect effect) {
        RichTapVibrationEffect.PatternHe newEffect = (RichTapVibrationEffect.PatternHe) effect;

        int[] pattern = newEffect.getPatternInfo();
        int looper = newEffect.getLooper();
        int interval = newEffect.getInterval();
        int amplitude = newEffect.getAmplitude();
        int freq = newEffect.getFreq();

        long patternId = -1;
        try {
            IRichtapVibrator service = getRichtapService();
            if (service != null) {
                mLastRichTapEffect = true;
                service.performHe(looper, interval, amplitude, freq, pattern, mCallback);
            }
        } catch (Exception e) {
            Slog.e(TAG, "aac richtap doVibratorOnPatternHe failed", e);
        }
    }

    public void richTapVibratorOnRawPattern(int[] pattern, int amplitude, int freq) {
        try {
            IRichtapVibrator service = getRichtapService();
            if (service != null) {
                mLastRichTapEffect = true;
                service.performHe(1, 0, amplitude, freq, pattern, mCallback);
            }
        } catch (Exception e) {
            Slog.e(TAG, "aac richtap doVibratorOnPatternHe failed", e);
        }
    }

    public void richTapVibratorStop(boolean force) {
        if (!mLastRichTapEffect && !force) {
            return;
        }
        try {
            IRichtapVibrator service = getRichtapService();
            if (service != null) {
                if (DEBUG_RICHTAP) {
                    Slog.d(TAG, "aac richtap doVibratorStop");
                }
                service.stop(mCallback);
                mLastRichTapEffect = false;
            }
        } catch (Exception e) {
            Slog.e(TAG, "aac richtap doVibratorStop failed", e);
        }
    }

    public static boolean checkIfRichTapEffect(VibrationEffect effect, String reason) {
        if (reason != null && reason.equals(HapticPlayer.VIBRATE_REASON)) {
            return false;
        }
        if (effect instanceof RichTapVibrationEffect.PatternHeParameter
                || effect instanceof RichTapVibrationEffect.PatternHe
                || effect instanceof RichTapVibrationEffect.Envelope
                || effect instanceof RichTapVibrationEffect.HapticParameter
                || effect instanceof RichTapVibrationEffect.ExtPrebaked) {
            return true;
        }
        return false;
    }

    void resetHalServiceProxy() {
        sRichtapVibratorService = null;
    }

    private static final class VibHalDeathRecipient implements IBinder.DeathRecipient {

        RichTapVibratorService mRichTapService;

        VibHalDeathRecipient(RichTapVibratorService richtapService) {
            mRichTapService = richtapService;
        }

        public void binderDied() {
            Slog.w(TAG, "vibrator hal died, should reset hal proxy!!");
            synchronized (VibHalDeathRecipient.class) {
                if (mRichTapService != null) {
                    Slog.i(TAG, "vibrator hal reset hal proxy");
                    mRichTapService.resetHalServiceProxy();
                }
            }
        }
    }

    public boolean checkIfPrevPatternData(SenderId senderId) {
        if ((senderId.getPid() == mCurrentSenderId.getPid()) &&
                (senderId.getSeq() == mCurrentSenderId.getSeq())) {
            return true;
        }
        return false;

    }

    public void setCurrentSenderId(SenderId senderId) {
        mCurrentSenderId.setPid(senderId.getPid());
        mCurrentSenderId.setSeq(senderId.getSeq());
    }

    public void resetCurrentSenderId() {
        mCurrentSenderId.reset();
    }

    public SenderId getSenderId(VibrationEffect effect) {
        if (effect instanceof RichTapVibrationEffect.PatternHe) {
            RichTapVibrationEffect.PatternHe patternHe = (RichTapVibrationEffect.PatternHe) effect;
            int[] patternData = patternHe.getPatternInfo();
            if (patternData != null && patternData.length > 0) {
                int versionOrType = patternData[0];
                if (versionOrType == SDK_HAL_NEW_FORMAT_DATA_VERSION) {
                    int pid = patternData[2];
                    int seq = patternData[3];
                    if (DEBUG_RICHTAP) {
                        Slog.d(TAG, "get sender id pid: " + pid + ", seq: " + seq);
                    }
                    return new SenderId(pid, seq);
                }
            }
        }
        return null;
    }

    public boolean checkIfEffectHe2_0(VibrationEffect effect, String reason) {
        if (reason != null && reason.equals(HapticPlayer.VIBRATE_REASON)) {
            return false;
        }
        if (effect instanceof RichTapVibrationEffect.PatternHe) {
            RichTapVibrationEffect.PatternHe patternHe = (RichTapVibrationEffect.PatternHe) effect;
            int[] patternData = patternHe.getPatternInfo();
            int versionOrType = patternData[0];
            if (versionOrType == SDK_HAL_NEW_FORMAT_DATA_VERSION) {
                return true;
            }
        }
        return false;
    }

    public boolean checkIfFirstHe2_0Package(VibrationEffect effect) {
        if (effect instanceof RichTapVibrationEffect.PatternHe) {
            RichTapVibrationEffect.PatternHe patternHe = (RichTapVibrationEffect.PatternHe) effect;
            int[] patternData = patternHe.getPatternInfo();
            int firstPatternIndexInPackage = patternData[5]; // package first pattern index offset
            if (DEBUG_RICHTAP) {
                Slog.d(TAG, "checkIfFirstHe2_0Package firstPatternIndexInPackage: " + firstPatternIndexInPackage);
            }
            if (firstPatternIndexInPackage == 0) {
                return true;
            }
        }
        return false;
    }

    static class SenderId {
        int mPid;
        int mSeq;

        SenderId(int pid, int seq) {
            mPid = pid;
            mSeq = seq;
        }

        void setPid(int pid) {
            mPid = pid;
        }

        int getPid() {
            return mPid;
        }

        void setSeq(int seq) {
            mSeq = seq;
        }

        int getSeq() {
            return mSeq;
        }

        void reset() {
            mPid = 0;
            mSeq = 0;
        }
    }
}
