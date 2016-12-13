package com.gjiazhe.multichoicescirclebutton;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by gjz on 12/12/2016.
 */

public class MultiChoicesCircleButtonBehavior extends CoordinatorLayout.Behavior<MultiChoicesCircleButton>{

    public MultiChoicesCircleButtonBehavior() {
        super();
    }

    public MultiChoicesCircleButtonBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout, MultiChoicesCircleButton child,
                                       View directTargetChild, View target, int nestedScrollAxes) {
        return nestedScrollAxes == ViewCompat.SCROLL_AXIS_VERTICAL;
    }

    @Override
    public void onNestedScroll(CoordinatorLayout coordinatorLayout, MultiChoicesCircleButton child,
                               View target, int dxConsumed, int dyConsumed,
                               int dxUnconsumed, int dyUnconsumed) {
        super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed);

        if (dyConsumed > 0 || dyUnconsumed > 0) {
            child.hide();
        } else if (dyConsumed < 0 || dyUnconsumed < 0) {
            child.show();
        }
    }
}
