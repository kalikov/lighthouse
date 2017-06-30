package ru.radiomayak.widget;

import android.content.Context;
import android.support.design.widget.AppBarLayout;
import android.util.AttributeSet;

public class AspectRatioAppBarLayout extends AppBarLayout {
    private static final double ASPECT_RATIO = 0.41176;

    public AspectRatioAppBarLayout(Context context) {
        super(context);
    }

    public AspectRatioAppBarLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);

        int height = Math.min(400, (int)(width * ASPECT_RATIO));

        super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }
}
