package com.meow.utaract.utils; // Or your appropriate package

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

public class ScrollAwareFABBehavior extends CoordinatorLayout.Behavior<LinearLayout> {
    private static final long ANIMATION_DURATION = 1000;
    private boolean isFabVisible = true;
    public ScrollAwareFABBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onStartNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull LinearLayout child, @NonNull View directTargetChild, @NonNull View target, int axes, int type) {
        // We are interested in vertical scrolls
        return axes == ViewCompat.SCROLL_AXIS_VERTICAL;
    }

    @Override
    public void onNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull LinearLayout child, @NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type, @NonNull int[] consumed) {
        super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type, consumed);

        if (dyConsumed > 0 && isFabVisible) {
            // User scrolled down and the FAB is currently visible
            hide(child);
            isFabVisible = false;
        } else if (dyConsumed < 0 && !isFabVisible) {
            // User scrolled up and the FAB is currently hidden
            show(child);
            isFabVisible = true;
        }
    }

    private void hide(final LinearLayout view) {
        view.animate()
                .translationX(view.getWidth() + 20)
                .alpha(0.0f) // Fade out
                .setInterpolator(new FastOutSlowInInterpolator()) // Use the Material Design interpolator
                .setDuration(ANIMATION_DURATION)
                .start();
    }

    private void show(final LinearLayout view) {
        view.animate()
                .translationX(0)
                .alpha(1.0f) // Fade in
                .setInterpolator(new FastOutSlowInInterpolator()) // Use the Material Design interpolator
                .setDuration(ANIMATION_DURATION)
                .start();
    }
}