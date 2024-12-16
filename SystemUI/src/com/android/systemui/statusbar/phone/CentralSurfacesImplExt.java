/*
 * Copyright (C) 2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.statusbar.phone;

import static android.app.StatusBarManager.DISABLE_NOTIFICATION_ICONS;
import static android.app.StatusBarManager.DISABLE_NOTIFICATION_TICKER;

import static com.android.settingslib.display.BrightnessUtils.GAMMA_SPACE_MAX;
import static com.android.settingslib.display.BrightnessUtils.convertGammaToLinearFloat;

import static org.nameless.os.CustomVibrationAttributes.VIBRATION_ATTRIBUTES_SLIDER;
import static org.nameless.os.DebugConstants.DEBUG_TICKER;

import static vendor.nameless.hardware.vibratorExt.V1_0.Effect.HEAVY_CLICK;
import static vendor.nameless.hardware.vibratorExt.V1_0.Effect.SLIDER_EDGE;
import static vendor.nameless.hardware.vibratorExt.V1_0.Effect.SLIDER_STEP;
import static vendor.nameless.hardware.vibratorExt.V1_0.Effect.UNIFIED_SUCCESS;

import android.annotation.NonNull;
import android.app.Notification;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.VibrationExtInfo;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewStub;

import com.android.systemui.demomode.DemoModeController;
import com.android.systemui.res.R;
import com.android.systemui.statusbar.NotificationLockscreenUserManager;
import com.android.systemui.statusbar.VibratorHelper;
import com.android.systemui.statusbar.notification.collection.NotifCollection.CancellationReason;
import com.android.systemui.statusbar.notification.collection.NotifPipeline;
import com.android.systemui.statusbar.notification.collection.notifcollection.NotifCollectionListener;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.android.systemui.statusbar.notification.interruption.NotificationInterruptStateProvider;
import com.android.systemui.statusbar.policy.ClockCenter;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;
import com.android.systemui.statusbar.policy.KeyguardStateController;
import com.android.systemui.statusbar.window.StatusBarWindowController;
import com.android.systemui.util.concurrency.MessageRouter;

import org.nameless.systemui.shade.CustomGestureListener;
import org.nameless.systemui.statusbar.ticker.AdvertSwitcherView;
import org.nameless.systemui.statusbar.ticker.MarqueeTickerEx;
import org.nameless.systemui.statusbar.ticker.MarqueeTickerView;
import org.nameless.systemui.statusbar.ticker.TickerController;
import org.nameless.systemui.statusbar.ticker.TickerEx;

class CentralSurfacesImplExt {

    private static final String TAG = "CentralSurfacesImplExt";

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
    private DemoModeController mDemoModeController;
    private DeviceProvisionedController mDeviceProvisionedController;
    private HeadsUpManagerPhone mHeadsUpManager;
    private int mDisplayId;
    private KeyguardStateController mKeyguardStateController;
    private MessageRouter mMessageRouter;
    private NotifCollectionListener mNotifCollectionListener;
    private NotifPipeline mNotifPipeline;
    private NotificationInterruptStateProvider mNotificationInterruptStateProvider;
    private NotificationLockscreenUserManager mLockscreenUserManager;
    private StatusBarWindowController mStatusBarWindowController;
    private TickerController mTickerController;
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

    private AdvertSwitcherView mSwitcherView;
    private MarqueeTickerEx mTicker;

    void init(CentralSurfacesImpl centralSurfacesImpl,
            Context context,
            CustomGestureListener customGestureListener,
            DemoModeController demoModeController,
            DeviceProvisionedController deviceProvisionedController,
            HeadsUpManagerPhone headsUpManager,
            int displayId,
            KeyguardStateController keyguardStateController,
            MessageRouter messageRouter,
            NotifPipeline notifPipeline,
            NotificationInterruptStateProvider notificationInterruptStateProvider,
            NotificationLockscreenUserManager lockscreenUserManager,
            StatusBarWindowController statusBarWindowController,
            TickerController tickerController,
            VibratorHelper vibratorHelper) {
        mCentralSurfacesImpl = centralSurfacesImpl;
        mContext = context;
        mCustomGestureListener = customGestureListener;
        mDemoModeController = demoModeController;
        mDeviceProvisionedController = deviceProvisionedController;
        mDisplayId = displayId;
        mHeadsUpManager = headsUpManager;
        mLockscreenUserManager = lockscreenUserManager;
        mKeyguardStateController = keyguardStateController;
        mMessageRouter = messageRouter;
        mNotifPipeline = notifPipeline;
        mNotificationInterruptStateProvider = notificationInterruptStateProvider;
        mStatusBarWindowController = statusBarWindowController;
        mTickerController = tickerController;
        mVibratorHelper = vibratorHelper;

        mDisplayManager = mContext.getSystemService(DisplayManager.class);
        mPowerManager = mContext.getSystemService(PowerManager.class);

        mMinimumBacklight = mPowerManager.getBrightnessConstraint(
                PowerManager.BRIGHTNESS_CONSTRAINT_TYPE_MINIMUM);
        mMaximumBacklight = mPowerManager.getBrightnessConstraint(
                PowerManager.BRIGHTNESS_CONSTRAINT_TYPE_MAXIMUM);

        mMessageRouter.subscribeTo(MSG_LONG_PRESS_BRIGHTNESS_CHANGE,
                id -> onLongPressBrightnessChange());

        mTickerController.addCallback((notificationTicker) -> {
            if (!notificationTicker) {
                tickerHalt();
            }
        });
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

    private void initEntryListener() {
        mNotifCollectionListener = new NotifCollectionListener() {
            @Override
            public void onEntryAdded(@NonNull NotificationEntry entry) {
                if (!mTickerController.showNotificationTicker()) {
                    return;
                }
                if (shouldFilterHeadsUpNotification(entry)) {
                    return;
                }
                TickerEx.tickFilter(entry, false, () -> tick(entry, true));
            }

            @Override
            public void onEntryUpdated(@NonNull NotificationEntry entry) {
                if (!mTickerController.showNotificationTicker()) {
                    return;
                }
                if (mDemoModeController.isInDemoMode()) {
                    return;
                }
                if (shouldUpdateNotificationTicker(entry.getSbn())) {
                    updateSwitcherViewVisibility(true);
                } else {
                    if (shouldFilterHeadsUpNotification(entry)) {
                        return;
                    }
                    TickerEx.tickFilter(entry, false, () -> tick(entry, false));
                }
            }

            @Override
            public void onEntryRemoved(@NonNull NotificationEntry entry, @CancellationReason int reason) {
                TickerEx.removeTickFilter(entry);
                mTicker.removeEntry(entry.getSbn());
                mSwitcherView.removeNotification(entry.getSbn().getKey());
            }
        };
        mNotifPipeline.addCollectionListener(mNotifCollectionListener);
    }

    void initTicker(PhoneStatusBarView statusBarView) {
        if (mNotifCollectionListener != null) {
            mNotifPipeline.removeCollectionListener(mNotifCollectionListener);
        }
        mSwitcherView = (AdvertSwitcherView) statusBarView.findViewById(R.id.status_bar_switcher);
        mTicker = inflateTickerView(statusBarView);
        mTicker.setStatusBarContents(statusBarView.findViewById(R.id.status_bar_contents));
        mTicker.setSwitcherView(mSwitcherView);
        mTicker.setCenterClockView((ClockCenter) statusBarView.findViewById(R.id.center_clock));
        initEntryListener();
    }

    private MarqueeTickerEx inflateTickerView(PhoneStatusBarView statusBarView) {
        final ViewStub tickerStub = (ViewStub) statusBarView.findViewById(R.id.ticker_stub);
        if (tickerStub == null) {
            return null;
        }
        final View tickerView = tickerStub.inflate();
        final MarqueeTickerEx marqueeTicker = new MarqueeTickerEx(mContext, statusBarView);
        marqueeTicker.setTickerView(tickerView);
        final MarqueeTickerView tickerText = (MarqueeTickerView) statusBarView.findViewById(R.id.tickerText);
        tickerText.setTicker(marqueeTicker);
        statusBarView.setTickerView(tickerView);
        return marqueeTicker;
    }

    private void tick(NotificationEntry notificationEntry, boolean firstTime) {
        if (mDemoModeController.isInDemoMode()) {
            if (DEBUG_TICKER) {
                Log.d(TAG, "tick, return: in demo mode");
            }
            return;
        }
        if (!mDeviceProvisionedController.isDeviceProvisioned()) {
            if (DEBUG_TICKER) {
                Log.d(TAG, "tick, return: device is not provisioned");
            }
            return;
        }
        final StatusBarNotification n = notificationEntry.getSbn();
        final int notificationUserId = n.getUserId();
        if (!mLockscreenUserManager.isCurrentProfile(notificationUserId)) {
            if (DEBUG_TICKER) {
                Log.d(TAG, "tick, return: not for current user");
            }
            return;
        }
        if (mHeadsUpManager.hasPinnedHeadsUp()) {
            if (DEBUG_TICKER) {
                Log.d(TAG, "tick, return: already has pinned heads up");
            }
            return;
        }
        if (mKeyguardStateController.isShowing() && !mKeyguardStateController.isOccluded()) {
            if (DEBUG_TICKER) {
                Log.d(TAG, "tick, return: keyguard showing and not occluded");
            }
            return;
        }
        if (mCentralSurfacesImpl.getNotificationPanelViewController().isFullyExpanded()) {
            if (DEBUG_TICKER) {
                Log.d(TAG, "tick, return: notification panel is fully expanded");
            }
            return;
        }
        if (mLockscreenUserManager.isAnyProfilePublicMode()) {
            if (DEBUG_TICKER) {
                Log.d(TAG, "tick, return: any of the profiles are in public mode");
            }
            return;
        }
        if (n.getNotification().tickerText == null ||
                n.getNotification().tickerText.toString().isEmpty()) {
            if (DEBUG_TICKER) {
                Log.d(TAG, "tick, return: tickerText is empty");
            }
            return;
        }
        if (mCentralSurfacesImpl.getNotificationShadeWindowView().getWindowToken() == null) {
            if (DEBUG_TICKER) {
                Log.d(TAG, "tick, return: window token is null");
            }
            return;
        }
        if ((mCentralSurfacesImpl.getDisabled1() & (DISABLE_NOTIFICATION_ICONS | DISABLE_NOTIFICATION_TICKER)) != 0) {
            if (DEBUG_TICKER) {
                Log.d(TAG, "tick, return: notification icon/ticker disabled");
            }
            return;
        }

        mTicker.halt();
        if (!mSwitcherView.addNotification(n)) {
            mTicker.addEntry(n);
        }
    }

    void tickerHalt() {
        if (DEBUG_TICKER) {
            Log.d(TAG, "tickerHalt");
        }
        if (mTicker != null) {
            mTicker.halt();
        }
        updateSwitcherViewVisibility(false);
    }

    private boolean shouldFilterHeadsUpNotification(NotificationEntry entry) {
        if (mHeadsUpManager.shouldHeadsUpBecomePinned(entry) &&
                mNotificationInterruptStateProvider.shouldHeadsUp(entry) &&
                !mHeadsUpManager.isSnoozed(entry.getSbn().getPackageName())) {
            return true;
        }
        return false;
    }

    private boolean shouldUpdateNotificationTicker(StatusBarNotification sbn) {
        if (sbn == null || mSwitcherView == null) {
            return false;
        }
        final Notification notification = sbn.getNotification();
        if (notification == null) {
            return false;
        }
        if (!mSwitcherView.addNotification(sbn)) {
            return false;
        }
        return (notification.flags & Notification.FLAG_ONLY_UPDATE_TICKER) != 0;
    }

    private void updateSwitcherViewVisibility(boolean visible) {
        if (mSwitcherView != null) {
            visible &= !(mKeyguardStateController.isShowing() && mKeyguardStateController.isOccluded());
            if (DEBUG_TICKER) {
                Log.d(TAG, "updateSwitcherViewVisibility, visible=" + visible);
            }
            mSwitcherView.updateTickerViewVisibility(visible);
        }
    }
}
