/*
 * Copyright (C) 2010 Daniel Nilsson
 * Copyright (C) 2013 Slimroms
 * Copyright (C) 2020 Havoc-OS
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

package org.sun.settings.preference.colorpicker;

import static org.sun.os.CustomVibrationAttributes.VIBRATION_ATTRIBUTES_MISC_SCENES;

import static vendor.sun.hardware.vibratorExt.Effect.CLICK;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.VibrationExtInfo;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.view.KeyEvent;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.android.settings.R;

public class ColorPickerDialog extends AlertDialog implements
        ColorPickerView.OnColorChangedListener,
        View.OnClickListener, View.OnKeyListener {

    private final Context mContext;
    private final Vibrator mVibrator;

    private ColorPickerPanelView mOldColor;
    private ColorPickerPanelView mNewColor;
    private ColorPickerView mColorPicker;
    private EditText mHex;
    private OnColorChangedListener mListener;

    public interface OnColorChangedListener {
        void onColorChanged(int color);
    }

    ColorPickerDialog(Context context, int initialColor) {
        super(context);
        mContext = context;
        mVibrator = context.getSystemService(Vibrator.class);

        init(initialColor);
    }

    private void init(int color) {
        if (getWindow() != null) {
            getWindow().setFormat(PixelFormat.RGBA_8888);
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            setUp(color);
        }
    }

    private void setColorFromHex() {
        final String text = mHex.getText().toString();
        final int newColor = ColorPickerPreference.convertToColorInt(text);
        mColorPicker.setColor(newColor, true);
    }

    private void setUp(int color) {
        final LayoutInflater inflater = mContext.getSystemService(LayoutInflater.class);
        final View layout = inflater.inflate(R.layout.preference_color_picker, null);

        mColorPicker = layout.findViewById(R.id.color_picker_view);
        mOldColor = layout.findViewById(R.id.old_color_panel);
        mNewColor = layout.findViewById(R.id.new_color_panel);
        mHex = layout.findViewById(R.id.hex);

        ((LinearLayout) mOldColor.getParent()).setPadding(
                Math.round(mColorPicker.getDrawingOffset()),
                0,
                Math.round(mColorPicker.getDrawingOffset()),
                0);

        mOldColor.setOnClickListener(this);
        mNewColor.setOnClickListener(this);
        mColorPicker.setOnColorChangedListener(this);
        mOldColor.setColor(color);
        mColorPicker.setColor(color, true);

        mHex.setText(ColorPickerPreference.convertToRGB(color));
        mHex.setOnKeyListener(this);

        setView(layout);
    }

    @Override
    public void onColorChanged(int color) {
        mNewColor.setColor(color);
        mHex.setText(ColorPickerPreference.convertToRGB(color));
    }

    void setAlphaSliderVisible(boolean visible) {
        mColorPicker.setAlphaSliderVisible(visible);
    }

    /**
     * Set a OnColorChangedListener to get notified when the color selected by the user has changed.
     *
     * @param listener
     */
    void setOnColorChangedListener(OnColorChangedListener listener) {
        mListener = listener;
    }

    public int getColor() {
        return mColorPicker.getColor();
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                    (keyCode == KeyEvent.KEYCODE_ENTER)) {
            setColorFromHex();
            return true;
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.new_color_panel) {
            if (mListener != null) {
                mListener.onColorChanged(mNewColor.getColor());
            }
        }
        doHapticFeedback();
        dismiss();
    }

    @NonNull
    @Override
    public Bundle onSaveInstanceState() {
        Bundle state = super.onSaveInstanceState();
        state.putInt("old_color", mOldColor.getColor());
        state.putInt("new_color", mNewColor.getColor());
        dismiss();
        return state;
    }

    @Override
    public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mOldColor.setColor(savedInstanceState.getInt("old_color"));
        mColorPicker.setColor(savedInstanceState.getInt("new_color"), true);
    }

    private void doHapticFeedback() {
        mVibrator.vibrateExt(new VibrationExtInfo.Builder()
                .setEffectId(CLICK)
                .setVibrationAttributes(VIBRATION_ATTRIBUTES_MISC_SCENES)
                .build());
    }
}
