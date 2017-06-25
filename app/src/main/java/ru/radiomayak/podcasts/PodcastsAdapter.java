package ru.radiomayak.podcasts;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.util.LongSparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import ru.radiomayak.LighthouseApplication;
import ru.radiomayak.R;

class PodcastsAdapter extends BaseAdapter {
    private final LighthouseApplication application;
    private final Context context;
    private final List<Podcast> podcasts;
    private final LongSparseArray<Bitmap> images;
    private final Bitmap micBitmap;

    private OnDisplayListener onDisplayListener;

    interface OnDisplayListener {
        void onDisplay(int position);
    }

    PodcastsAdapter(LighthouseApplication application, Context context, List<Podcast> podcasts, LongSparseArray<Bitmap> images) {
        this.application = application;
        this.context = context;
        this.podcasts = podcasts;
        this.images = images;
        micBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.mic);
    }

    void setOnDisplayListener(OnDisplayListener listener) {
        this.onDisplayListener = listener;
    }

    @Override
    public int getCount() {
        return podcasts.size();
    }

    @Override
    public boolean isEmpty() {
        return podcasts.isEmpty();
    }

    @Override
    public Podcast getItem(int position) {
        return podcasts.get(position);
    }

    @Override
    public long getItemId(int position) {
        return podcasts.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.podcasts_item, parent, false);
        }
        Podcast podcast = getItem(position);

        TextView nameView = getNameView(convertView);
        nameView.setText(podcast.getName());
        nameView.setTypeface(application.getFontBold());

        TextView lengthView = getLengthView(convertView);
        int length = podcast.getLength();
        if (length > 0) {
            lengthView.setVisibility(View.VISIBLE);
            lengthView.setText(String.valueOf(length));
            lengthView.setTypeface(application.getFontLight());
        } else {
            lengthView.setVisibility(View.GONE);
        }

        ImageView iconView = getIconView(convertView);
        iconView.setContentDescription(podcast.getName());

        Bitmap iconBitmap = images.get(podcast.getId());
        setCircularImageBitmap(iconView, iconBitmap == null ? micBitmap : iconBitmap);

        TextView descriptionView = getDescriptionView(convertView);
        if (podcast.getDescription() != null) {
            descriptionView.setVisibility(View.VISIBLE);
            descriptionView.setText(podcast.getDescription());
            descriptionView.setTypeface(application.getFontNormal());
        } else {
            descriptionView.setVisibility(View.GONE);
        }

        if (onDisplayListener != null) {
            onDisplayListener.onDisplay(position);
        }

        return convertView;
    }

    private void setCircularImageBitmap(ImageView view, Bitmap bitmap) {
        RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(context.getResources(), bitmap);
        drawable.setCircular(true);
        view.setImageDrawable(drawable);
    }

    private static TextView getNameView(View view) {
        return (TextView) view.findViewById(R.id.name);
    }

    private static TextView getDescriptionView(View view) {
        return (TextView) view.findViewById(R.id.description);
    }

    private static TextView getLengthView(View view) {
        return (TextView) view.findViewById(R.id.length);
    }

    private static ImageView getIconView(View view) {
        return (ImageView) view.findViewById(android.R.id.icon);
    }
}
