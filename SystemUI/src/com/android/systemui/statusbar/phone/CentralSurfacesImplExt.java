/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.statusbar.phone;

import static com.android.settingslib.display.BrightnessUtils.GAMMA_SPACE_MAX;
import static com.android.settingslib.display.BrightnessUtils.convertGammaToLinearFloat;

import static org.nameless.os.CustomVibrationAttributes.VIBRATION_ATTRIBUTES_SLIDER;

import static vendor.nameless.hardware.vibratorExt.V1_0.Effect.HEAVY_CLICK;
import static vendor.nameless.hardware.vibratorExt.V1_0.Effect.SLIDER_EDGE;
import static vendor.nameless.hardware.vibratorExt.V1_0.Effect.SLIDER_STEP;
import static vendor.nameless.hardware.vibratorExt.V1_0.Effect.UNIFIED_SUCCESS;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.VibrationExtInfo;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import com.android.systemui.statusbar.VibratorHelper;
import com.android.systemui.statusbar.window.StatusBarWindowController;
import com.android.systemui.util.concurrency.MessageRouter;

import org.nameless.systemui.shade.CustomGestureListener;

class CentralSurfacesImplExt {

    private static final int MSG_LONG_PRESS_BRIGHTNESS_CHANGE = 2001;

    private static final float BRIGHTNESS_CONTROL_PADDING = 0.18f;
    private static final int BRIGHTNESS_CONTROL_EXTRA_HEIGHT = 20;
    private static final int BRIGHTNESS_CONTROL_LINGER_THRESHOLD = 20;
    private static final long BRIGHTNESS_CONTROL_LONG_PRESS_TIMEOUT = 750L;

    private static final VibrationAttributes HARDWARE_FEEDBACK_VIBRATION_ATTRIBUTES =
            VibrationAttributes.createForUsage(VibrationAttributes.USAGE_HARDWARE_FEEDBACK);
    private static final VibrationEffect EFFECT_HEAVY_CLICK =
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK);

    private static final long HAPTIC_MIN_INTERVAL =
            SystemProperties.getLong("sys.nameless.haptic.slider_interval", 50L);

    private static class InstanceHolder {
        private static CentralSurfacesImplExt INSTANCE = new CentralSurfacesImplExt();
    }

    static CentralSurfacesImplExt getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private CentralSurfacesImpl mCentralSurfacesImpl;
    private Context mContext;
    private CustomGestureListener mCustomGestureListener;
    private int mDisplayId;
    private MessageRouter mMessageRouter;
    private StatusBarWindowController mStatusBarWindowController;
    private VibratorHelper mVibratorHelper;

    private DisplayManager mDisplayManager;
    private PowerManager mPowerManager;

    private float mMinimumBacklight;
    private float mMaximumBacklight;
    private int mBrightnessControlHeight;

    private float mCurrentBrightness;
    private int mInitialTouchX;
    private int mInitialTouchY;
    private int mLinger;
    private boolean mBrightnessChanged;
    private boolean mJustPeeked;
    private boolean mInBrightnessControl;

    private long mLastHapticTimestamp;

    void init(CentralSurfacesImpl centralSurfacesImpl,
            Context context,
            CustomGestureListener customGestureListener,
            int displayId,
            MessageRouter messageRouter,
            StatusBarWindowController statusBarWindowController,
            VibratorHelper vibratorHelper) {
        mCentralSurfacesImpl = centralSurfacesImpl;
        mContext = context;
        mCustomGestureListener = customGestureListener;
        mDisplayId = displayId;
        mMessageRouter = messageRouter;
        mStatusBarWindowController = statusBarWindowController;
        mVibratorHelper = vibratorHelper;

        mDisplayManager = mContext.getSystemService(DisplayManager.class);
        mPowerManager = mContext.getSystemService(PowerManager.class);

        mMinimumBacklight = mPowerManager.getBrightnessConstraint(
                PowerManager.BRIGHTNESS_CONSTRAINT_TYPE_MINIMUM);
        mMaximumBacklight = mPowerManager.getBrightnessConstraint(
                PowerManager.BRIGHTNESS_CONSTRAINT_TYPE_MAXIMUM);

        mMessageRouter.subscribeTo(MSG_LONG_PRESS_BRIGHTNESS_CHANGE,
                id -> onLongPressBrightnessChange());
    }

    void updateResources() {
        mBrightnessControlHeight = mStatusBarWindowController.getStatusBarHeight()
                + BRIGHTNESS_CONTROL_EXTRA_HEIGHT;
    }

    private void onLongPressBrightnessChange() {
        mVibratorHelper.vibrateExt(new VibrationExtInfo.Builder()
                .setEffectId(UNIFIED_SUCCESS)
                .setFallbackEffectId(HEAVY_CLICK)
                .setVibrationAttributes(HARDWARE_FEEDBACK_VIBRATION_ATTRIBUTES)
                .build()
        );
        mInBrightnessControl = true;
        adjustBrightness(mInitialTouchX);
        mLinger = BRIGHTNESS_CONTROL_LINGER_THRESHOLD + 1;
    }

    private void adjustBrightness(int x) {
        mBrightnessChanged = true;
        final float raw = (float) x / mCentralSurfacesImpl.getDisplayWidth();

        // Add a padding to the brightness control on both sides to
        // make it easier to reach min/max brightness
        final float padded = Math.min(1.0f - BRIGHTNESS_CONTROL_PADDING,
                Math.max(BRIGHTNESS_CONTROL_PADDING, raw));
        final float value = (padded - BRIGHTNESS_CONTROL_PADDING) /
                (1 - (2.0f * BRIGHTNESS_CONTROL_PADDING));
        final float val = convertGammaToLinearFloat(
                Math.round(value * GAMMA_SPACE_MAX),
                mMinimumBacklight, mMaximumBacklight);
        if (mCurrentBrightness != val) {
            if (mCurrentBrightness != -1) {
                final long now = SystemClock.uptimeMillis();
                if (val == mMinimumBacklight || val == mMaximumBacklight) {
                    mLastHapticTimestamp = now;
                    mVibratorHelper.vibrateExt(new VibrationExtInfo.Builder()
                            .setEffectId(SLIDER_EDGE)
                            .setVibrationAttributes(VIBRATION_ATTRIBUTES_SLIDER)
                            .build()
                    );
                } else if (now - mLastHapticTimestamp > HAPTIC_MIN_INTERVAL) {
                    mLastHapticTimestamp = now;
                    mVibratorHelper.vibrateExt(new VibrationExtInfo.Builder()
                            .setEffectId(SLIDER_STEP)
                            .setAmplitude((float) (val - mMinimumBacklight)
                                        / (mMaximumBacklight - mMinimumBacklight))
                            .setVibrationAttributes(VIBRATION_ATTRIBUTES_SLIDER)
                            .build()
                    );
                }
            }
            mCurrentBrightness = val;
            mDisplayManager.setTemporaryBrightness(mDisplayId, val);
        }
    }

    void interceptForBrightnessControl(MotionEvent event) {
        final int action = event.getAction();
        final int x = (int) event.getRawX();
        final int y = (int) event.getRawY();
        if (action == MotionEvent.ACTION_DOWN) {
            mInBrightnessControl = false;
            if (y < mBrightnessControlHeight) {
                mCurrentBrightness = -1;
                mLinger = 0;
                mInitialTouchX = x;
                mInitialTouchY = y;
                mJustPeeked = true;
                mMessageRouter.cancelMessages(MSG_LONG_PRESS_BRIGHTNESS_CHANGE);
                mMessageRouter.sendMessageDelayed(MSG_LONG_PRESS_BRIGHTNESS_CHANGE,
                        BRIGHTNESS_CONTROL_LONG_PRESS_TIMEOUT);
            }
        } else if (action == MotionEvent.ACTION_MOVE) {
            if (y < mBrightnessControlHeight && mJustPeeked) {
                if (mLinger > BRIGHTNESS_CONTROL_LINGER_THRESHOLD) {
                    adjustBrightness(x);
                } else {
                    final int xDiff = Math.abs(x - mInitialTouchX);
                    final int yDiff = Math.abs(y - mInitialTouchY);
                    final int touchSlop = ViewConfiguration.get(mContext).getScaledTouchSlop();
                    if (xDiff > yDiff) {
                        mLinger++;
                    }
                    if (xDiff > touchSlop || yDiff > touchSlop) {
                        mMessageRouter.cancelMessages(MSG_LONG_PRESS_BRIGHTNESS_CHANGE);
                    }
                }
            } else {
                if (y > mBrightnessControlHeight) {
                    mJustPeeked = false;
                }
                mMessageRouter.cancelMessages(MSG_LONG_PRESS_BRIGHTNESS_CHANGE);
            }
        } else if (action == MotionEvent.ACTION_UP) {
            mMessageRouter.cancelMessages(MSG_LONG_PRESS_BRIGHTNESS_CHANGE);
            mInBrightnessControl = false;
        } else if (action == MotionEvent.ACTION_CANCEL) {
            mMessageRouter.cancelMessages(MSG_LONG_PRESS_BRIGHTNESS_CHANGE);
        }
    }

    void checkBrightnessChanged(boolean upOrCancel) {
        if (mBrightnessChanged && upOrCancel) {
            mBrightnessChanged = false;
            mDisplayManager.setBrightness(mDisplayId, mCurrentBrightness);
        }
    }

    boolean isBrightnessControlEnabled() {
        return mCustomGestureListener.isStatusbarBrightnessControlEnabled();
    }

    boolean isInBrightnessControl() {
        return mInBrightnessControl;
    }
}
