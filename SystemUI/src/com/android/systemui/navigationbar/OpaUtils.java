package com.android.systemui.navigationbar;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;
import android.util.ArraySet;
import android.view.RenderNodeAnimator;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import com.android.internal.app.AssistUtils;
import com.android.systemui.R;

final class OpaUtils {

    static final Interpolator INTERPOLATOR_40_40 = new PathInterpolator(0.4f, 0.0f, 0.6f, 1.0f);
    static final Interpolator INTERPOLATOR_40_OUT = new PathInterpolator(0.4f, 0.0f, 1.0f, 1.0f);

    static float getDeltaDiamondPositionBottomX() {
        return 0.0f;
    }

    static float getDeltaDiamondPositionLeftY() {
        return 0.0f;
    }

    static float getDeltaDiamondPositionRightY() {
        return 0.0f;
    }

    static float getDeltaDiamondPositionTopX() {
        return 0.0f;
    }

    static ObjectAnimator getAlphaObjectAnimator(View view, float value,
            long duration, long delay, Interpolator interpolator) {
        final ObjectAnimator ofFloat = ObjectAnimator.ofFloat(view, View.ALPHA, value);
        ofFloat.setInterpolator(interpolator);
        ofFloat.setDuration(duration);
        ofFloat.setStartDelay(delay);
        return ofFloat;
    }

    static Animator getLongestAnim(ArraySet<Animator> animatorSet) {
        long minDuration = Long.MIN_VALUE;
        Animator animator = null;
        for (int i = animatorSet.size() - 1; i >= 0; i--) {
            final Animator anim = animatorSet.valueAt(i);
            if (anim.getTotalDuration() > minDuration) {
                minDuration = anim.getTotalDuration();
                animator = anim;
            }
        }
        return animator;
    }

    static ObjectAnimator getScaleObjectAnimator(View view, float value,
            long duration, Interpolator interpolator) {
        final ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(view,
                PropertyValuesHolder.ofFloat(View.SCALE_X, value),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, value));
        animator.setDuration(duration);
        animator.setInterpolator(interpolator);
        return animator;
    }

    static ObjectAnimator getTranslationObjectAnimatorX(View view, Interpolator interpolator, float end, float start, long duration) {
        final ObjectAnimator animator = ObjectAnimator.ofFloat(view, View.X, end, end + start);
        animator.setInterpolator(interpolator);
        animator.setDuration(duration);
        return animator;
    }

    static ObjectAnimator getTranslationObjectAnimatorY(View view, Interpolator interpolator,
            float start, float end, long duration) {
        final ObjectAnimator animator = ObjectAnimator.ofFloat(view, View.Y, end, end + start);
        animator.setInterpolator(interpolator);
        animator.setDuration(duration);
        return animator;
    }

    static float getPxVal(Resources resources, int id) {
        return (float) resources.getDimensionPixelOffset(id);
    }

    static float getDeltaDiamondPositionTopY(Resources resources) {
        return -getPxVal(resources, R.dimen.opa_diamond_translation);
    }

    static float getDeltaDiamondPositionLeftX(Resources resources) {
        return -getPxVal(resources, R.dimen.opa_diamond_translation);
    }

    static float getDeltaDiamondPositionRightX(Resources resources) {
        return getPxVal(resources, R.dimen.opa_diamond_translation);
    }

    static float getDeltaDiamondPositionBottomY(Resources resources) {
        return getPxVal(resources, R.dimen.opa_diamond_translation);
    }
}
