/*
 * Copyright (C) 2019 CypherOS
 * Copyright (C) 2014-2020 Paranoid Android
 * Copyright (C) 2024 Namaless-AOSP
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

package org.nameless.systemui.volume;

import static android.hardware.SensorManager.SENSOR_DELAY_NORMAL;
import static android.view.Surface.ROTATION_0;
import static android.view.Surface.ROTATION_90;
import static android.view.Surface.ROTATION_180;
import static android.view.Surface.ROTATION_270;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.PixelFormat;
import android.graphics.drawable.ColorDrawable;
import android.hardware.display.DisplayManagerGlobal;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.OrientationEventListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.internal.policy.SystemBarUtils;

import com.android.systemui.R;
import com.android.systemui.plugins.VolumeDialogController;
import com.android.systemui.plugins.VolumeDialogController.Callbacks;
import com.android.systemui.plugins.VolumeDialogController.State;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener;

import org.nameless.systemui.volume.TriStateUiController.UserActivityListener;

public class TriStateUiControllerImpl implements ConfigurationListener, TriStateUiController {

    private static String TAG = "TriStateUiControllerImpl";

    private static final int MSG_DIALOG_SHOW = 1;
    private static final int MSG_DIALOG_DISMISS = 2;
    private static final int MSG_RESET_SCHEDULE = 3;
    private static final int MSG_STATE_CHANGE = 4;

    private static final int MODE_NORMAL = AudioManager.RINGER_MODE_NORMAL;
    private static final int MODE_SILENT = AudioManager.RINGER_MODE_SILENT;
    private static final int MODE_VIBRATE = AudioManager.RINGER_MODE_VIBRATE;

    private static final int TRI_STATE_MODE_UNKNOWN = -1;

    private static final int TRI_STATE_UI_POSITION_LEFT = 0;
    private static final int TRI_STATE_UI_POSITION_RIGHT = 1;

    private static final long DIALOG_TIMEOUT = 2000L;

    private final Context mContext;
    private final ConfigurationController mConfigurationController;
    private final VolumeDialogController mVolumeDialogController;
    private final Callbacks mVolumeDialogCallback = new Callbacks() {
        @Override
        public void onShowRequested(int reason, boolean keyguardLocked, int lockTaskModeState) { }

        @Override
        public void onDismissRequested(int reason) { }

        @Override
        public void onScreenOff() { }

        @Override
        public void onStateChanged(State state) { }

        @Override
        public void onLayoutDirectionChanged(int layoutDirection) { }

        @Override
        public void onShowVibrateHint() { }

        @Override
        public void onShowSilentHint() { }

        @Override
        public void onShowSafetyWarning(int flags) { }

        @Override
        public void onAccessibilityModeChanged(Boolean showA11yStream) { }

        @Override
        public void onCaptionComponentStateChanged(
                Boolean isComponentEnabled, Boolean fromTooltip) { }

        @Override
        public void onConfigurationChanged() {
            updateTheme();
            updateTriStateLayout();
        }

        @Override
        public void onCaptionEnabledStateChanged(Boolean isEnabled, Boolean checkBeforeSwitch) { }

        @Override
        public void onShowCsdWarning(int csdWarning, int durationMs) { }
    };

    private final H mHandler = new H(this);

    private final AudioManager mAudioManager;
    private final OrientationEventListener mOrientationListener;

    private int mDensity;
    private Dialog mDialog;
    private int mDialogPosition;
    private ViewGroup mDialogView;
    private UserActivityListener mListener;
    private int mOrientationType = 0;
    private boolean mShowing = false;
    private int mBackgroundColor = 0;
    private int mThemeMode = 0;
    private int mIconColor = 0;
    private int mTextColor = 0;
    private ImageView mTriStateIcon;
    private TextView mTriStateText;
    private int mTriStateMode = TRI_STATE_MODE_UNKNOWN;
    private Window mWindow;
    private LayoutParams mWindowLayoutParams;
    private int mWindowType;

    private final BroadcastReceiver mRingerStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateRingerModeChanged();
        }
    };

    private final class H extends Handler {
        private final TriStateUiControllerImpl mUiController;

        public H(TriStateUiControllerImpl uiController) {
            super(Looper.getMainLooper());
            mUiController = uiController;
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_DIALOG_SHOW:
                    mUiController.handleShow();
                    return;
                case MSG_DIALOG_DISMISS:
                    mUiController.handleDismiss();
                    return;
                case MSG_RESET_SCHEDULE:
                    mUiController.handleResetTimeout();
                    return;
                case MSG_STATE_CHANGE:
                    mUiController.handleStateChanged();
                    return;
            }
        }
    }

    public TriStateUiControllerImpl(Context context,
            ConfigurationController configurationController,
            VolumeDialogController volumeDialogController) {
        mContext = context;
        mConfigurationController = configurationController;
        mVolumeDialogController = volumeDialogController;
        mAudioManager = context.getSystemService(AudioManager.class);
        mOrientationListener = new OrientationEventListener(mContext, SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int orientation) {
                checkOrientationType();
            }
        };

        mContext.registerReceiver(mRingerStateReceiver, new IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION));
    }

    private void checkOrientationType() {
        final Display display = DisplayManagerGlobal.getInstance().getRealDisplay(0);
        if (display == null) {
            return;
        }
        final int rotation = display.getRotation();
        if (rotation != mOrientationType) {
            mOrientationType = rotation;
            updateTriStateLayout();
        }
    }

    public void init(UserActivityListener listener, int windowType) {
        mListener = listener;
        mWindowType = windowType;
        mDensity = mContext.getResources().getConfiguration().densityDpi;
        mConfigurationController.addCallback(this);
        mVolumeDialogController.addCallback(mVolumeDialogCallback, mHandler);
        initDialog();
    }

    public void destroy() {
        mVolumeDialogController.removeCallback(mVolumeDialogCallback);
        mConfigurationController.removeCallback(this);
        mContext.unregisterReceiver(mRingerStateReceiver);
    }

    private void initDialog() {
        mDialog = new Dialog(mContext);
        mShowing = false;
        mWindow = mDialog.getWindow();
        mWindow.requestFeature(Window.FEATURE_NO_TITLE);
        mWindow.setBackgroundDrawable(new ColorDrawable(0));
        mWindow.clearFlags(LayoutParams.FLAG_DIM_BEHIND);
        mWindow.addFlags(LayoutParams.FLAG_NOT_FOCUSABLE
                | LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | LayoutParams.FLAG_NOT_TOUCH_MODAL
                | LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                | LayoutParams.FLAG_HARDWARE_ACCELERATED);
        mDialog.setCanceledOnTouchOutside(false);
        mWindowLayoutParams = mWindow.getAttributes();
        mWindowLayoutParams.type = mWindowType;
        mWindowLayoutParams.format = PixelFormat.TRANSLUCENT;
        mWindowLayoutParams.setTitle(TriStateUiControllerImpl.class.getSimpleName());
        mWindowLayoutParams.gravity = Gravity.TOP | Gravity.RIGHT;
        mWindowLayoutParams.y = mDialogPosition;
        mWindow.setAttributes(mWindowLayoutParams);
        mWindow.setSoftInputMode(LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        mDialog.setContentView(R.layout.tri_state_dialog);
        mDialogView = (ViewGroup) mDialog.findViewById(R.id.tri_state_layout);
        mTriStateIcon = (ImageView) mDialog.findViewById(R.id.tri_state_icon);
        mTriStateText = (TextView) mDialog.findViewById(R.id.tri_state_text);
        updateTheme();
    }

    public void show() {
        mHandler.obtainMessage(MSG_DIALOG_SHOW, 0, 0).sendToTarget();
    }

    private void registerOrientationListener(boolean enable) {
        if (mOrientationListener.canDetectOrientation() && enable) {
            Log.v(TAG, "Can detect orientation");
            mOrientationListener.enable();
            return;
        }
        Log.v(TAG, "Cannot detect orientation");
        mOrientationListener.disable();
    }

    private void updateTriStateLayout() {
        if (mContext == null) {
            return;
        }
        final Resources res = mContext.getResources();
        int bg = 0;
        int iconId = 0;
        int textId = 0;
        int positionX = mWindowLayoutParams.x;
        int positionY;
        int positionY2 = mWindowLayoutParams.y;
        int gravity = mWindowLayoutParams.gravity;

        switch (mTriStateMode) {
            case MODE_SILENT:
                iconId = R.drawable.ic_volume_ringer_mute;
                textId = R.string.volume_ringer_status_silent;
                break;
            case MODE_VIBRATE:
                iconId = R.drawable.ic_volume_ringer_vibrate;
                textId = R.string.volume_ringer_status_vibrate;
                break;
            default:
            case MODE_NORMAL:
                iconId = R.drawable.ic_volume_ringer;
                textId = R.string.volume_ringer_status_normal;
                break;
        }

        final int triStatePos = res.getInteger(com.android.internal.R.integer.config_alertSliderLocation);
        final boolean isTsKeyRight = triStatePos == TRI_STATE_UI_POSITION_RIGHT;
        switch (mOrientationType) {
            case ROTATION_90:
                if (isTsKeyRight) {
                    gravity = Gravity.TOP | Gravity.LEFT;
                } else {
                    gravity = Gravity.BOTTOM | Gravity.LEFT;
                }
                positionY2 = res.getDimensionPixelSize(R.dimen.tri_state_up_dialog_position_deep_land);
                if (isTsKeyRight) {
                    positionY2 += SystemBarUtils.getStatusBarHeight(mContext);
                }
                switch (mTriStateMode) {
                    case MODE_SILENT:
                        positionX = res.getDimensionPixelSize(R.dimen.tri_state_up_dialog_position_l);
                        break;
                    case MODE_VIBRATE:
                        positionX = res.getDimensionPixelSize(R.dimen.tri_state_middle_dialog_position_l);
                        break;
                    default:
                    case MODE_NORMAL:
                        positionX = res.getDimensionPixelSize(R.dimen.tri_state_down_dialog_position_l);
                        break;
                }
                bg = R.drawable.dialog_tri_state_middle_bg;
                break;
            case ROTATION_180:
                if (isTsKeyRight) {
                    gravity = Gravity.BOTTOM | Gravity.LEFT;
                } else {
                    gravity = Gravity.BOTTOM | Gravity.RIGHT;
                }
                positionX = res.getDimensionPixelSize(R.dimen.tri_state_up_dialog_position_deep);
                switch (mTriStateMode) {
                    case MODE_SILENT:
                        positionY = res.getDimensionPixelSize(R.dimen.tri_state_up_dialog_position)
                                + SystemBarUtils.getStatusBarHeight(mContext);
                        break;
                    case MODE_VIBRATE:
                        positionY = res.getDimensionPixelSize(R.dimen.tri_state_middle_dialog_position)
                                + SystemBarUtils.getStatusBarHeight(mContext);
                        break;
                    default:
                    case MODE_NORMAL:
                        positionY = res.getDimensionPixelSize(R.dimen.tri_state_down_dialog_position)
                                + SystemBarUtils.getStatusBarHeight(mContext);
                        break;
                }
                positionY2 = positionY;
                bg = R.drawable.dialog_tri_state_middle_bg;
                break;
            case ROTATION_270:
                if (isTsKeyRight) {
                    gravity = Gravity.BOTTOM | Gravity.RIGHT;
                } else {
                    gravity = Gravity.TOP | Gravity.RIGHT;
                }
                positionY2 = res.getDimensionPixelSize(R.dimen.tri_state_up_dialog_position_deep_land);
                if (!isTsKeyRight) {
                    positionY2 += SystemBarUtils.getStatusBarHeight(mContext);
                }
                switch (mTriStateMode) {
                    case MODE_SILENT:
                        positionX = res.getDimensionPixelSize(R.dimen.tri_state_up_dialog_position_l);
                        break;
                    case MODE_VIBRATE:
                        positionX = res.getDimensionPixelSize(R.dimen.tri_state_middle_dialog_position_l);
                        break;
                    default:
                    case MODE_NORMAL:
                        positionX = res.getDimensionPixelSize(R.dimen.tri_state_down_dialog_position_l);
                        break;
                }
                bg = R.drawable.dialog_tri_state_middle_bg;
                break;
            default:
            case ROTATION_0:
                if (isTsKeyRight) {
                    gravity = Gravity.TOP | Gravity.RIGHT;
                } else {
                    gravity = Gravity.TOP | Gravity.LEFT;
                }
                positionX = res.getDimensionPixelSize(R.dimen.tri_state_up_dialog_position_deep);
                switch (mTriStateMode) {
                    case MODE_SILENT:
                        positionY2 = res.getDimensionPixelSize(R.dimen.tri_state_up_dialog_position)
                                + SystemBarUtils.getStatusBarHeight(mContext);
                        bg = isTsKeyRight ? R.drawable.right_dialog_tri_state_up_bg : R.drawable.left_dialog_tri_state_up_bg;
                        break;
                    case MODE_VIBRATE:
                        positionY2 = res.getDimensionPixelSize(R.dimen.tri_state_middle_dialog_position)
                                + SystemBarUtils.getStatusBarHeight(mContext);
                        bg = R.drawable.dialog_tri_state_middle_bg;
                        break;
                    default:
                    case MODE_NORMAL:
                        positionY2 = res.getDimensionPixelSize(R.dimen.tri_state_down_dialog_position)
                                + SystemBarUtils.getStatusBarHeight(mContext);
                        bg = isTsKeyRight ? R.drawable.right_dialog_tri_state_down_bg : R.drawable.left_dialog_tri_state_down_bg;
                        break;
                }
                break;
        }

        if (mTriStateMode != TRI_STATE_MODE_UNKNOWN) {
            if (mTriStateIcon != null) {
                mTriStateIcon.setImageResource(iconId);
            }
            if (mTriStateText != null) {
                String inputText = res.getString(textId);
                if (inputText != null && mTriStateText.length() == inputText.length()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(inputText);
                    sb.append(" ");
                    inputText = sb.toString();
                }
                mTriStateText.setText(inputText);
            }
            if (mDialogView != null) {
                mDialogView.setBackgroundDrawable(res.getDrawable(bg));
            }
        }
        mDialogPosition = positionY2;
        positionY = res.getDimensionPixelSize(R.dimen.tri_state_dialog_padding);
        mWindowLayoutParams.gravity = gravity;
        mWindowLayoutParams.y = positionY2 - positionY;
        mWindowLayoutParams.x = positionX - positionY;
        mWindow.setAttributes(mWindowLayoutParams);
        handleResetTimeout();
    }

    private void updateRingerModeChanged() {
        mHandler.obtainMessage(MSG_STATE_CHANGE, 0, 0).sendToTarget();
        if (mTriStateMode != TRI_STATE_MODE_UNKNOWN) {
            show();
        }
    }

    private void handleShow() {
        mHandler.removeMessages(MSG_DIALOG_SHOW);
        mHandler.removeMessages(MSG_DIALOG_DISMISS);
        handleResetTimeout();
        if (!mShowing) {
            updateTheme();
            registerOrientationListener(true);
            checkOrientationType();
            mShowing = true;
            mDialog.show();
            if (mListener != null) {
                mListener.onTriStateUserActivity();
            }
        }
    }

    private void handleDismiss() {
        mHandler.removeMessages(MSG_DIALOG_SHOW);
        mHandler.removeMessages(MSG_DIALOG_DISMISS);
        if (mShowing) {
            registerOrientationListener(false);
            mShowing = false;
            mDialog.dismiss();
        }
    }

    private void handleStateChanged() {
        final int ringerMode = mAudioManager.getRingerModeInternal();
        if (ringerMode != mTriStateMode) {
            mTriStateMode = ringerMode;
            updateTriStateLayout();
            if (mListener != null) {
                mListener.onTriStateUserActivity();
            }
        }
    }

    public void handleResetTimeout() {
        mHandler.removeMessages(MSG_DIALOG_DISMISS);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(
                MSG_DIALOG_DISMISS, MSG_RESET_SCHEDULE, 0), DIALOG_TIMEOUT);
        if (mListener != null) {
            mListener.onTriStateUserActivity();
        }
    }

    @Override
    public void onDensityOrFontScaleChanged() {
        handleDismiss();
        initDialog();
        updateTriStateLayout();
    }

    private void updateTheme() {
        mIconColor = getAttrColor(android.R.attr.colorAccent);
        mTextColor = getAttrColor(android.R.attr.textColorPrimary);
        mBackgroundColor = getAttrColor(android.R.attr.colorPrimary);
        mDialogView.setBackgroundTintList(ColorStateList.valueOf(mBackgroundColor));
        mTriStateIcon.setColorFilter(mIconColor);
        mTriStateText.setTextColor(mTextColor);
    }

    public int getAttrColor(int attr) {
        TypedArray ta = mContext.obtainStyledAttributes(new int[]{attr});
        final int colorAccent = ta.getColor(0, 0);
        ta.recycle();
        return colorAccent;
    }
}
