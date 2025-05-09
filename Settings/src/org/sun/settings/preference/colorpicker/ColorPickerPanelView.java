/*
 * Copyright (C) 2010 Daniel Nilsson
 * Copyright (C) 2013 Slimroms
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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * This class draws a panel which which will be filled with a color which can be set.
 * It can be used to show the currently selected color which you will get from
 * the {@link ColorPickerView}.
 * @author Daniel Nilsson
 *
 */
public class ColorPickerPanelView extends View {

    /**
     * The width in pixels of the border
     * surrounding the color panel.
     */
    private final static float BORDER_WIDTH_PX = 1;

    private final float mDensity;

    private int mBorderColor = 0xff6E6E6E;
    private int mColor = 0xff000000;

    private Paint mBorderPaint = new Paint();
    private Paint mColorPaint = new Paint();

    private RectF mDrawingRect;
    private RectF mColorRect;

    private AlphaPatternDrawable mAlphaPattern;

    public ColorPickerPanelView(Context context) {
        this(context, null);
    }

    public ColorPickerPanelView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ColorPickerPanelView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mDensity = context.getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        mBorderPaint.setColor(mBorderColor);
        canvas.drawRect(mDrawingRect, mBorderPaint);

        if (mAlphaPattern != null) {
            mAlphaPattern.draw(canvas);
        }

        mColorPaint.setColor(mColor);
        canvas.drawRect(mColorRect, mColorPaint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int width = MeasureSpec.getSize(widthMeasureSpec);
        final int height = MeasureSpec.getSize(heightMeasureSpec);

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mDrawingRect = new RectF();
        mDrawingRect.left = getPaddingLeft();
        mDrawingRect.right = w - getPaddingRight();
        mDrawingRect.top = getPaddingTop();
        mDrawingRect.bottom = h - getPaddingBottom();

        setUpColorRect();
    }

    private void setUpColorRect() {
        final float left = mDrawingRect.left + BORDER_WIDTH_PX;
        final float top = mDrawingRect.top + BORDER_WIDTH_PX;
        final float bottom = mDrawingRect.bottom - BORDER_WIDTH_PX;
        final float right = mDrawingRect.right - BORDER_WIDTH_PX;

        mColorRect = new RectF(left,top, right, bottom);
        mAlphaPattern = new AlphaPatternDrawable((int)(5 * mDensity));
        mAlphaPattern.setBounds(
            Math.round(mColorRect.left),
            Math.round(mColorRect.top),
            Math.round(mColorRect.right),
            Math.round(mColorRect.bottom)
        );

    }

    /**
     * Set the color that should be shown by this view.
     * @param color
     */
    public void setColor(int color) {
        mColor = color;
        invalidate();
    }

    /**
     * Get the color currently show by this view.
     * @return
     */
    public int getColor() {
        return mColor;
    }

    /**
     * Set the color of the border surrounding the panel.
     * @param color
     */
    public void setBorderColor(int color) {
        mBorderColor = color;
        invalidate();
    }

    /**
     * Get the color of the border surrounding the panel.
     */
    public int getBorderColor() {
        return mBorderColor;
    }
}
