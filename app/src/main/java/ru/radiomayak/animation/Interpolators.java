package ru.radiomayak.animation;

import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

public final class Interpolators {
    public static final LinearInterpolator LINEAR = new LinearInterpolator();

    public static final AccelerateInterpolator ACCELERATE = new AccelerateInterpolator();
    public static final DecelerateInterpolator DECELERATE = new DecelerateInterpolator();

    private Interpolators() {
    }
}
