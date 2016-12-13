package com.gjiazhe.multichoicescirclebutton;

import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.view.animation.Interpolator;

/**
 * Created by gjz on 12/12/2016.
 */

public class MultiChoicesCircleButtonBehavior extends CoordinatorLayout.Behavior<MultiChoicesCircleButton>{
    private static final Interpolator INTERPOLATOR = new FastOutSlowInInterpolator();
    private boolean mIsAnimatingOut = false;
}
