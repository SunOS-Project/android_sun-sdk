/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.server.policy.gesture;

import static org.nameless.os.CustomVibrationAttributes.VIBRATION_ATTRIBUTES_OFF_SCREEN_GESTURE;

import static vendor.nameless.hardware.vibratorExt.V1_0.Effect.HEAVY_CLICK;
import static vendor.nameless.hardware.vibratorExt.V1_0.Effect.OFF_SCREEN_GESTURE;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.UserHandle;
import android.os.VibrationExtInfo;
import android.os.Vibrator;
import android.util.Slog;
import android.view.KeyEvent;

import com.android.internal.util.nameless.CustomUtils;

import com.android.server.policy.PhoneWindowManagerExt;

import org.nameless.server.policy.DozeController;

import java.util.HashMap;
import java.util.List;

class TouchGestureActionTrigger {

    private static final String TAG = "TouchGestureActionTrigger";

    static final int ACTION_NONE = 0;

    /** Actions below are generic */
    static final int ACTION_FLASHLIGHT = 1;
    static final int ACTION_BROWSER = 2;
    static final int ACTION_CAMERA = 3;
    static final int ACTION_DIALER = 4;
    static final int ACTION_MESSAGES = 5;
    static final int ACTION_EMAIL = 6;
    /** Change values below after adding new */
    static final int ACTION_GENERIC_START = ACTION_FLASHLIGHT;
    static final int ACTION_GENERIC_END = ACTION_EMAIL;

    /** Actions below are starting user apps */
    static final int ACTION_WECHAT_PAY = 51;
    static final int ACTION_ALIPAY_PAY = 52;
    static final int ACTION_WECHAT_SCAN = 53;
    static final int ACTION_ALIPAY_SCAN = 54;
    static final int ACTION_ALIPAY_TRIP = 55;
    static final int ACTION_WALLET_TRIP = 56;
    static final int ACTION_GPAY = 57;
    static final int ACTION_PAYTM_PAY = 58;
    static final int ACTION_PAYTM_SCAN = 59;
    static final int ACTION_PHONEPE = 60;
    /** Change values below after adding new */
    static final int ACTION_USER_APP_START = ACTION_WECHAT_PAY;
    static final int ACTION_USER_APP_END = ACTION_PHONEPE;

    /** Actions below are used for specific gestures */
    static final int ACTION_SHOW_AMBIENT_DISPLAY = 101;
    static final int ACTION_LAST_SONG = 102;
    static final int ACTION_NEXT_SONG = 103;
    static final int ACTION_PLAY_PAUSE_SONG = 104;
    /** Change values below after adding new */
    static final int ACTION_SPECIFIC_START = ACTION_SHOW_AMBIENT_DISPLAY;
    static final int ACTION_SPECIFIC_END = ACTION_PLAY_PAUSE_SONG;

    private static final HashMap<Integer, ComponentName> ACTION_COMPONENT_NAME_MAP;

    static {
        ACTION_COMPONENT_NAME_MAP = new HashMap<>();
        ACTION_COMPONENT_NAME_MAP.put(ACTION_WECHAT_PAY, new ComponentName(
                "com.tencent.mm",
                "com.tencent.mm.plugin.offline.ui.WalletOfflineCoinPurseUI"));
        ACTION_COMPONENT_NAME_MAP.put(ACTION_ALIPAY_PAY, new ComponentName(
                "com.eg.android.AlipayGphone",
                "com.eg.android.AlipayGphone.FastStartActivity"));
        ACTION_COMPONENT_NAME_MAP.put(ACTION_WECHAT_SCAN, new ComponentName(
                "com.tencent.mm",
                "com.tencent.mm.plugin.scanner.ui.BaseScanUI"));
        ACTION_COMPONENT_NAME_MAP.put(ACTION_ALIPAY_SCAN, new ComponentName(
                "com.eg.android.AlipayGphone",
                "com.alipay.mobile.scan.as.main.MainCaptureActivity"));
        ACTION_COMPONENT_NAME_MAP.put(ACTION_ALIPAY_TRIP, new ComponentName(
                "com.eg.android.AlipayGphone",
                "com.alipay.android.phone.wallet.aptrip.ui.ApTripActivity"));
        ACTION_COMPONENT_NAME_MAP.put(ACTION_WALLET_TRIP, new ComponentName(
                "com.finshell.wallet",
                "com.nearme.wallet.nfc.ui.NfcConsumeActivity"));
        ACTION_COMPONENT_NAME_MAP.put(ACTION_GPAY, new ComponentName(
                "com.google.android.apps.nbu.paisa.user",
                "com.google.nbu.paisa.flutter.gpay.app.MainActivity"));
        ACTION_COMPONENT_NAME_MAP.put(ACTION_PAYTM_PAY, new ComponentName(
                "net.one97.paytm",
                "net.one97.paytm.wallet.newdesign.activity.PaySendActivityV2"));
        ACTION_COMPONENT_NAME_MAP.put(ACTION_PAYTM_SCAN, new ComponentName(
                "net.one97.paytm",
                "net.one97.paytm.acceptPayment.home.AcceptPaymentMainActivity"));
        ACTION_COMPONENT_NAME_MAP.put(ACTION_PHONEPE, new ComponentName(
                "com.phonepe.app",
                "com.phonepe.app.ui.activity.Navigator_MainActivity"));
    }

    private final Context mContext;
    private final PackageManager mPackageManager;
    private final Vibrator mVibrator;

    TouchGestureActionTrigger(Context context) {
        mContext = context;
        mPackageManager = context.getPackageManager();
        mVibrator = context.getSystemService(Vibrator.class);
    }

    void trigger(int action) {
        if (action >= ACTION_GENERIC_START && action <= ACTION_GENERIC_END) {
            triggerGenericAction(action);
        } else if (action >= ACTION_USER_APP_START && action <= ACTION_USER_APP_END) {
            triggerUserAppAction(action);
        } else if (action >= ACTION_SPECIFIC_START && action <= ACTION_SPECIFIC_END) {
            triggerSpecificAction(action);
        } else {
            Slog.e(TAG, "Unknown action: " + action);
            return;
        }
        if (action != ACTION_SHOW_AMBIENT_DISPLAY) {
            mVibrator.vibrateExt(new VibrationExtInfo.Builder()
                .setEffectId(OFF_SCREEN_GESTURE)
                .setFallbackEffectId(HEAVY_CLICK)
                .setVibrationAttributes(VIBRATION_ATTRIBUTES_OFF_SCREEN_GESTURE)
                .build());
        }
    }

    private void triggerGenericAction(int action) {
        switch (action) {
            case ACTION_FLASHLIGHT:
                CustomUtils.toggleCameraFlash();
                break;
            case ACTION_BROWSER:
                wakeUpAndStartActivity(getBrowserIntent());
                break;
            case ACTION_CAMERA:
                final Intent intent = new Intent(Intent.ACTION_SCREEN_CAMERA_GESTURE);
                mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT,
                        Manifest.permission.STATUS_BAR_SERVICE);
                break;
            case ACTION_DIALER:
                wakeUpAndStartActivity(getDialerIntent());
                break;
            case ACTION_MESSAGES:
                wakeUpAndStartActivity(getMessagesIntent());
                break;
            case ACTION_EMAIL:
                wakeUpAndStartActivity(getEmailIntent());
                break;
        }
    }

    private void triggerUserAppAction(int action) {
        wakeUpAndStartActivity(getAppActivityIntent(action));
    }

    private void triggerSpecificAction(int action) {
        switch (action) {
            case ACTION_SHOW_AMBIENT_DISPLAY:
                DozeController.getInstance().launchDozePulse();
                break;
            case ACTION_LAST_SONG:
                PhoneWindowManagerExt.getInstance().dispatchMediaKeyToMediaSession(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
                break;
            case ACTION_NEXT_SONG:
                PhoneWindowManagerExt.getInstance().dispatchMediaKeyToMediaSession(KeyEvent.KEYCODE_MEDIA_NEXT);
                break;
            case ACTION_PLAY_PAUSE_SONG:
                PhoneWindowManagerExt.getInstance().dispatchMediaKeyToMediaSession(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
                break;
        }
    }

    private void wakeUpAndStartActivity(Intent intent) {
        DozeController.getInstance().wakeUpScreen(false);
        CustomUtils.startActivityDismissingKeyguard(intent);
    }

    private Intent getLaunchableIntent(Intent intent) {
        final List<ResolveInfo> resInfo = mPackageManager.queryIntentActivities(intent, 0);
        if (resInfo.isEmpty()) {
            return null;
        }
        return mPackageManager.getLaunchIntentForPackage(resInfo.get(0).activityInfo.packageName);
    }

    private Intent getBrowserIntent() {
        return getLaunchableIntent(new Intent(Intent.ACTION_VIEW, Uri.parse("http:")));
    }

    private Intent getDialerIntent() {
        return new Intent(Intent.ACTION_DIAL, null);
    }

    private Intent getMessagesIntent() {
        return getLaunchableIntent(new Intent(Intent.ACTION_VIEW, Uri.parse("sms:")));
    }

    private Intent getEmailIntent() {
        return getLaunchableIntent(new Intent(Intent.ACTION_VIEW, Uri.parse("mailto:")));
    }

    private Intent getAppActivityIntent(int action) {
        final ComponentName cn = ACTION_COMPONENT_NAME_MAP.getOrDefault(action, null);
        if (cn == null) {
            return null;
        }
        final Intent intent = new Intent();
        intent.setComponent(cn);
        return intent;
    }

    static String actionToString(int action) {
        switch (action) {
            case ACTION_NONE: return "none";
            case ACTION_FLASHLIGHT: return "flashlight";
            case ACTION_BROWSER: return "browser";
            case ACTION_CAMERA: return "camera";
            case ACTION_DIALER: return "dialer";
            case ACTION_MESSAGES: return "messages";
            case ACTION_EMAIL: return "email";
            case ACTION_WECHAT_PAY: return "WeChat pay";
            case ACTION_ALIPAY_PAY: return "Alipay pay";
            case ACTION_WECHAT_SCAN: return "WeChat scan";
            case ACTION_ALIPAY_SCAN: return "Alipay scan";
            case ACTION_ALIPAY_TRIP: return "Alipay trip";
            case ACTION_WALLET_TRIP: return "Wallet trip";
            case ACTION_GPAY: return "GPay";
            case ACTION_PAYTM_PAY: return "Paytm pay";
            case ACTION_PAYTM_SCAN: return "Paytm scan";
            case ACTION_PHONEPE: return "PhonePe";
            case ACTION_SHOW_AMBIENT_DISPLAY: return "ambient display";
            case ACTION_LAST_SONG: return "last song";
            case ACTION_NEXT_SONG: return "next song";
            case ACTION_PLAY_PAUSE_SONG: return "play/pause song";
            default: return "Unknown";
        }
    }
}
