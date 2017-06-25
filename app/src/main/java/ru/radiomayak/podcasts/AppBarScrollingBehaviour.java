package ru.radiomayak.podcasts;

import android.content.Context;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;
import android.view.View;

import ru.radiomayak.R;

public class AppBarScrollingBehaviour extends AppBarLayout.ScrollingViewBehavior {
    public AppBarScrollingBehaviour() {}

    public AppBarScrollingBehaviour(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, View child, View dependency) {
        return super.layoutDependsOn(parent, child, dependency) || child instanceof SwipeRefreshLayout && dependency.getId() == R.id.player;
    }
}
