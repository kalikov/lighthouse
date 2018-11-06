package ru.radiomayak.podcasts;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.support.annotation.NonNull;
import android.support.graphics.drawable.AnimatedVectorDrawableCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.widget.RecyclerView;
import android.util.LongSparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;
import java.util.Objects;

import ru.radiomayak.LighthouseActivity;
import ru.radiomayak.LighthouseTrack;
import ru.radiomayak.R;
import ru.radiomayak.graphics.BitmapInfo;

class PodcastsAdapter extends RecyclerView.Adapter<PodcastsAdapter.ViewHolder> {
    private final PodcastsFragment fragment;
    private final List<Podcast> podcasts;
    private final RoundedBitmapDrawable micDrawable;
    private final LongSparseArray<Drawable> icons = new LongSparseArray<>();
    private final AnimatedVectorDrawableCompat equalizerDrawable;

    private final View.OnClickListener adapterFavoriteClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (favoriteClickListener != null) {
                favoriteClickListener.onClick(view);
            }
        }
    };
    private final View.OnClickListener adapterItemClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (itemClickListener != null) {
                itemClickListener.onClick(view);
            }
        }
    };

    private View.OnClickListener favoriteClickListener;
    private View.OnClickListener itemClickListener;

    PodcastsAdapter(PodcastsFragment fragment, List<Podcast> podcasts) {
        this.fragment = fragment;
        this.podcasts = podcasts;

        setHasStableIds(true);

        Context context = fragment.requireContext();
        int size = context.getResources().getDimensionPixelSize(R.dimen.podcast_icon_size);
        Drawable micResourceDrawable = Objects.requireNonNull(ResourcesCompat.getDrawable(context.getResources(), R.drawable.mic, context.getTheme()));
        Bitmap micBitmap = createBitmap(micResourceDrawable, size);
        micDrawable = RoundedBitmapDrawableFactory.create(context.getResources(), micBitmap);
        micDrawable.setCircular(true);

        equalizerDrawable = AnimatedVectorDrawableCompat.create(context, R.drawable.equalizer_animated);
    }

    public void setFavoriteClickListener(View.OnClickListener favoriteClickListener) {
        this.favoriteClickListener = favoriteClickListener;
    }

    public void setItemClickListener(View.OnClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    private static Bitmap createBitmap(Drawable drawable, int size) {
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    void updateEqualizerAnimation() {
        if (equalizerDrawable != null) {
            boolean isPlaying = fragment.requireLighthouseActivity().isPlaying();
            if (equalizerDrawable.isRunning() && !isPlaying) {
                equalizerDrawable.stop();
            } else if (!equalizerDrawable.isRunning() && isPlaying) {
                equalizerDrawable.start();
            }
        }
    }

    @Override
    public int getItemCount() {
        return podcasts.size();
    }

    public boolean isEmpty() {
        return podcasts.isEmpty();
    }

    public Podcast getItem(int position) {
        return podcasts.get(position);
    }

    @Override
    public long getItemId(int position) {
        return podcasts.get(position).getId();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.podcasts_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Podcast podcast = getItem(position);
        holder.bind(podcast);
    }

    private static TextView getNameView(View view) {
        return view.findViewById(R.id.name);
    }

    private static TextView getDescriptionView(View view) {
        return view.findViewById(R.id.description);
    }

    private static TextView getLengthView(View view) {
        return view.findViewById(R.id.length);
    }

    private static ImageView getIconView(View view) {
        return view.findViewById(android.R.id.icon);
    }

    private static ImageView getEqualizerView(View view) {
        return view.findViewById(android.R.id.progress);
    }

    private static ImageView getFavoriteView(View view) {
        return view.findViewById(R.id.favorite);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View itemView) {
            super(itemView);
        }

        public void bind(Podcast podcast) {
            LighthouseActivity activity = fragment.requireLighthouseActivity();
            TextView nameView = getNameView(itemView);
            nameView.setText(podcast.getName());
            nameView.setTypeface(activity.getLighthouseApplication().getFontBold());

            TextView lengthView = getLengthView(itemView);
            int length = podcast.getLength();
            int seen = podcast.getSeen();
            if (length > seen) {
                lengthView.setVisibility(View.VISIBLE);
                lengthView.setText(String.valueOf(length - seen));
                lengthView.setTypeface(activity.getLighthouseApplication().getFontLight());
            } else {
                lengthView.setVisibility(View.GONE);
            }

            ImageView iconView = getIconView(itemView);
            iconView.setContentDescription(podcast.getName());
            setIcon(iconView, podcast.getId());

            if (equalizerDrawable != null) {
                ImageView equalizerView = getEqualizerView(itemView);
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

            TextView descriptionView = getDescriptionView(itemView);
            if (podcast.getDescription() != null) {
                descriptionView.setVisibility(View.VISIBLE);
                descriptionView.setText(podcast.getDescription());
                descriptionView.setTypeface(activity.getLighthouseApplication().getFontNormal());
            } else {
                descriptionView.setVisibility(View.GONE);
            }

            ImageView favoriteView = getFavoriteView(itemView);
            if (podcast.getFavorite() == 0) {
                favoriteView.setImageResource(R.drawable.favorite);
            } else {
                favoriteView.setImageResource(R.drawable.favorite_checked);
            }
            favoriteView.setOnClickListener(adapterFavoriteClickListener);

            itemView.setOnClickListener(adapterItemClickListener);
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
                RoundedBitmapDrawable rounded = RoundedBitmapDrawableFactory.create(view.getContext().getResources(), bitmapInfo.getBitmap());
                rounded.setCircular(true);
                drawable = rounded;
            } else {
                RoundedBitmapDrawable rounded = RoundedBitmapDrawableFactory.create(view.getContext().getResources(), bitmapInfo.getBitmap());
                rounded.setCircular(true);

                Drawable[] layers = new Drawable[]{icon, rounded};
                TransitionDrawable transitionDrawable = new TransitionDrawable(layers);
                transitionDrawable.startTransition(400);
                drawable = transitionDrawable;
            }
            view.setImageDrawable(drawable);

            icons.put(id, drawable);
        }
    }
}
