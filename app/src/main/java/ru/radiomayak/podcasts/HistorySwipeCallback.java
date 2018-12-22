package ru.radiomayak.podcasts;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;

import ru.radiomayak.LighthouseApplication;
import ru.radiomayak.R;

public abstract class HistorySwipeCallback extends ItemTouchHelper.SimpleCallback {
    private final Drawable icon;
    private final int tint;
    private final int padding;
    private final int padding2;
    private final float textSize;
    private final Typeface font;
    private final String text;

    public HistorySwipeCallback(LighthouseApplication application) {
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);

        icon = ContextCompat.getDrawable(application, R.drawable.delete);
        tint = ContextCompat.getColor(application, R.color.colorPrimary);
        padding = application.getResources().getDimensionPixelSize(R.dimen.podcast_padding);
        padding2 = application.getResources().getDimensionPixelSize(R.dimen.record_title_padding);
        textSize = application.getResources().getDimension(R.dimen.text_small_size);
        font = application.getFontNormal();
        text = application.getString(R.string.history_remove_record_short);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onChildDraw(@NonNull Canvas canvas, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dx, float dy, int actionState, boolean isCurrentlyActive) {
        View itemView = viewHolder.itemView;
        int itemHeight = itemView.getHeight();

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setFlags(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(tint);
        paint.setTypeface(font);
        paint.setTextSize(textSize);

        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);
//        canvas.drawRect(itemView.getRight() + (int)dx, itemView.getTop(), itemView.getRight(), itemView.getBottom(), paint);
        // Draw the red delete background

        float textTop = itemView.getTop() + itemHeight / 2 - bounds.exactCenterY();

//        background.color = backgroundColor
//        background.setBounds(
//                itemView.right + dX.toInt(),
//                itemView.top,
//                itemView.right,
//                itemView.bottom
//        )
//        background.draw(canvas)
//
//
        // Calculate position of delete icon
        int iconHeight = icon.getIntrinsicHeight();

        int iconTop = itemView.getTop() + (itemHeight - iconHeight) / 2;
        int iconBottom = iconTop + iconHeight;

        int iconLeft;
        int iconRight;
        float textLeft;
        if (dx < 0) {
            iconRight = itemView.getRight() - padding;
            iconLeft = iconRight - icon.getIntrinsicWidth();
            textLeft = iconLeft - bounds.width() - padding2;
        } else {
            iconLeft = itemView.getLeft() + padding;
            iconRight = iconLeft + icon.getIntrinsicWidth();
            textLeft = iconRight + padding2;
        }

        DrawableCompat.setTint(icon, tint);
        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
        icon.draw(canvas);

        canvas.drawText(text, textLeft, textTop, paint);

        super.onChildDraw(canvas, recyclerView, viewHolder, dx, dy, actionState, isCurrentlyActive);
    }
}
