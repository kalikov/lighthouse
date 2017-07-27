package ru.radiomayak.podcasts;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.support.graphics.drawable.AnimatedVectorDrawableCompat;
import android.support.v4.content.res.ResourcesCompat;
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

import ru.radiomayak.LighthouseActivity;
import ru.radiomayak.LighthouseTrack;
import ru.radiomayak.R;
import ru.radiomayak.graphics.BitmapInfo;

class PodcastsAdapter extends BaseAdapter {
    private final LighthouseActivity activity;
    private final List<Podcast> podcasts;
    private final RoundedBitmapDrawable micDrawable;
    private final LongSparseArray<Drawable> icons = new LongSparseArray<>();
    private final AnimatedVectorDrawableCompat equalizerDrawable;

    PodcastsAdapter(LighthouseActivity activity, List<Podcast> podcasts) {
        this.activity = activity;
        this.podcasts = podcasts;

        int size = activity.getResources().getDimensionPixelSize(R.dimen.podcast_icon_size);
        Drawable micResourceDrawable = ResourcesCompat.getDrawable(activity.getResources(), R.drawable.mic, activity.getTheme());
        Bitmap micBitmap = createBitmap(micResourceDrawable, size);
        micDrawable = RoundedBitmapDrawableFactory.create(activity.getResources(), micBitmap);
        micDrawable.setCircular(true);

        equalizerDrawable = AnimatedVectorDrawableCompat.create(activity, R.drawable.equalizer_animated);
    }

    private static Bitmap createBitmap(Drawable drawable, int size) {
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    void updateEqualizerAnimation() {
        if (equalizerDrawable != null && equalizerDrawable.isRunning() && !activity.isPlaying()) {
            equalizerDrawable.stop();
        }
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
            convertView = LayoutInflater.from(activity).inflate(R.layout.podcasts_item, parent, false);
        }
        Podcast podcast = getItem(position);

        TextView nameView = getNameView(convertView);
        nameView.setText(podcast.getName());
        nameView.setTypeface(activity.getLighthouseApplication().getFontBold());

        TextView lengthView = getLengthView(convertView);
        int length = podcast.getLength();
        int seen = podcast.getSeen();
        if (length > seen) {
            lengthView.setVisibility(View.VISIBLE);
            lengthView.setText(String.valueOf(length - seen));
            lengthView.setTypeface(activity.getLighthouseApplication().getFontLight());
        } else {
            lengthView.setVisibility(View.GONE);
        }

        ImageView iconView = getIconView(convertView);
        iconView.setContentDescription(podcast.getName());
        setIcon(iconView, podcast.getId());

        if (equalizerDrawable != null) {
            ImageView equalizerView = getEqualizerView(convertView);
            LighthouseTrack track = activity.getTrack();
            if (track == null || track.getPodcast().getId() != podcast.getId()) {
                equalizerView.setVisibility(View.GONE);
            } else if (activity.isPlaying()) {
                equalizerView.setImageDrawable(equalizerDrawable);
                equalizerView.setVisibility(View.VISIBLE);
                equalizerDrawable.start();
            } else {
                equalizerView.setImageResource(R.drawable.equalizer);
                equalizerView.setVisibility(View.VISIBLE);
                equalizerDrawable.stop();
            }
        }

        TextView descriptionView = getDescriptionView(convertView);
        if (podcast.getDescription() != null) {
            descriptionView.setVisibility(View.VISIBLE);
            descriptionView.setText(podcast.getDescription());
            descriptionView.setTypeface(activity.getLighthouseApplication().getFontNormal());
        } else {
            descriptionView.setVisibility(View.GONE);
        }

        return convertView;
    }

    private void setIcon(ImageView view, long id) {
        Drawable icon = icons.get(id);
        if (icon != null && icon != micDrawable) {
            view.setImageDrawable(icon);
            return;
        }
        BitmapInfo bitmapInfo = PodcastImageCache.getInstance().getIcon(id);
        if (icon != null && bitmapInfo == null) {
            view.setImageDrawable(icon);
            return;
        }
        Drawable drawable;
        if (icon == null && bitmapInfo == null) {
            drawable = micDrawable;
        } else if (icon == null) {
            RoundedBitmapDrawable rounded = RoundedBitmapDrawableFactory.create(activity.getResources(), bitmapInfo.getBitmap());
            rounded.setCircular(true);
            drawable = rounded;
        } else {
            RoundedBitmapDrawable rounded = RoundedBitmapDrawableFactory.create(activity.getResources(), bitmapInfo.getBitmap());
            rounded.setCircular(true);

            Drawable[] layers = new Drawable[]{icon, rounded};
            TransitionDrawable transitionDrawable = new TransitionDrawable(layers);
            transitionDrawable.startTransition(400);
            drawable = transitionDrawable;
        }
        view.setImageDrawable(drawable);

        icons.put(id, drawable);
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

    private static ImageView getEqualizerView(View view) {
        return (ImageView) view.findViewById(android.R.id.progress);
    }
}
