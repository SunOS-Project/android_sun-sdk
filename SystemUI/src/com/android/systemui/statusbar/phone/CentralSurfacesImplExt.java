/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.statusbar.phone;

import static com.android.settingslib.display.BrightnessUtils.GAMMA_SPACE_MAX;
import static com.android.settingslib.display.BrightnessUtils.convertGammaToLinearFloat;

import static org.nameless.os.CustomVibrationAttributes.VIBRATION_ATTRIBUTES_SLIDER;
import static org.nameless.os.CustomVibrationAttributes.VIBRATION_ATTRIBUTES_SLIDER_EDGE;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.PowerManager;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
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
    private static final VibrationEffect EFFECT_TICK =
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK);

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
        mVibratorHelper.vibrate(EFFECT_HEAVY_CLICK, HARDWARE_FEEDBACK_VIBRATION_ATTRIBUTES);
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
                if (val == mMinimumBacklight || val == mMaximumBacklight) {
                    mVibratorHelper.vibrate(EFFECT_HEAVY_CLICK, VIBRATION_ATTRIBUTES_SLIDER_EDGE);
                } else {
                    mVibratorHelper.vibrate(EFFECT_TICK, VIBRATION_ATTRIBUTES_SLIDER);
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
