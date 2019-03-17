package ru.radiomayak.podcasts;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;

import java.util.Objects;

import ru.radiomayak.LighthouseApplication;
import ru.radiomayak.R;

public abstract class HistorySwipeCallback extends ItemTouchHelper.SimpleCallback {
    private final Drawable icon;
    private final int padding;
    private final int iconPadding;
    private final String text;
    private final Paint paint;
    private final Rect bounds;

    public HistorySwipeCallback(LighthouseApplication application) {
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);

        icon = Objects.requireNonNull(ContextCompat.getDrawable(application, R.drawable.delete));
        padding = application.getResources().getDimensionPixelSize(R.dimen.podcast_padding);
        iconPadding = application.getResources().getDimensionPixelSize(R.dimen.record_title_padding);
        text = application.getString(R.string.history_remove_record_short);

        int tint = ContextCompat.getColor(application, R.color.colorPrimary);
        DrawableCompat.setTint(icon, tint);

        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setFlags(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(tint);
        paint.setTypeface(application.getFontNormal());
        paint.setTextSize(application.getResources().getDimension(R.dimen.text_small_size));

        bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
        float width = bounds.width() + icon.getIntrinsicWidth() + 2 * padding + iconPadding;
        return width / viewHolder.itemView.getWidth();
    }

    @Override
    public void onChildDraw(@NonNull Canvas canvas, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dx, float dy, int actionState, boolean isCurrentlyActive) {
        View itemView = viewHolder.itemView;
        int itemHeight = itemView.getHeight();

        float textTop = itemView.getTop() + itemHeight / 2 - bounds.exactCenterY();

        int iconHeight = icon.getIntrinsicHeight();

        int iconTop = itemView.getTop() + (itemHeight - iconHeight) / 2;
        int iconBottom = iconTop + iconHeight;

        int iconLeft;
        int iconRight;
        float textLeft;
        if (dx < 0) {
            iconRight = itemView.getRight() - padding;
            iconLeft = iconRight - icon.getIntrinsicWidth();
            textLeft = iconLeft - bounds.width() - iconPadding;
            dx = Math.max(dx, -(bounds.width() + icon.getIntrinsicWidth() + 2 * padding + iconPadding));
        } else {
            iconLeft = itemView.getLeft() + padding;
            iconRight = iconLeft + icon.getIntrinsicWidth();
            textLeft = iconRight + iconPadding;
            dx = Math.min(dx, (bounds.width() + icon.getIntrinsicWidth() + 2 * padding + iconPadding));
        }

        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
        icon.draw(canvas);

        canvas.drawText(text, textLeft, textTop, paint);

        super.onChildDraw(canvas, recyclerView, viewHolder, dx, dy, actionState, isCurrentlyActive);
    }
}
