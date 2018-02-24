package ru.radiomayak.widget;

import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.widget.TextView;

import java.lang.reflect.Field;

public final class ToolbarCompat {
    private ToolbarCompat() {
    }

    @Nullable
    private static TextView getTitleTextView(Toolbar toolbar) {
        try {
            Field titleTextField = Toolbar.class.getDeclaredField("mTitleTextView");
            boolean isAccessible = titleTextField.isAccessible();
            titleTextField.setAccessible(true);
            try {
                return (TextView) titleTextField.get(toolbar);
            } finally {
                titleTextField.setAccessible(isAccessible);
            }
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static void setTitleTypeface(Toolbar toolbar, Typeface typeface) {
        TextView textView = getTitleTextView(toolbar);
        if (textView != null) {
            textView.setTypeface(typeface);
        }
    }

    public static float getTitleTextSize(Toolbar toolbar) {
        TextView textView = getTitleTextView(toolbar);
        if (textView != null) {
            return textView.getTextSize();
        }
        return 0;
    }

    public static void setTitleTextSize(Toolbar toolbar, float size) {
        TextView textView = getTitleTextView(toolbar);
        if (textView != null) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        }
    }
}
