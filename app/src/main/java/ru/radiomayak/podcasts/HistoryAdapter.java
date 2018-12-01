package ru.radiomayak.podcasts;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.graphics.drawable.AnimatedVectorDrawableCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import ru.radiomayak.LighthouseApplication;
import ru.radiomayak.LighthouseTrack;
import ru.radiomayak.R;

class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
    static final int ITEM_VIEW_TYPE = 0;
    static final int HEADER_VIEW_TYPE = 1;
    static final int FOOTER_VIEW_TYPE = 2;

    private final HistoryFragment fragment;
    private final HistoryTracks tracks;
    private final AnimatedVectorDrawableCompat equalizerDrawable;

    private FooterMode footerMode;

    enum FooterMode {
        HIDDEN,
        LOADING,
        MORE,
        ERROR
    }

    HistoryAdapter(HistoryFragment fragment, HistoryTracks tracks) {
        this.fragment = fragment;
        this.tracks = tracks;
        footerMode = FooterMode.HIDDEN;
        setHasStableIds(true);

        equalizerDrawable = AnimatedVectorDrawableCompat.create(this.fragment.requireContext(), R.drawable.record_equalizer_animated);
    }

    void updateEqualizerAnimation(boolean isPlaying) {
        if (equalizerDrawable != null && equalizerDrawable.isRunning() && !isPlaying) {
            equalizerDrawable.stop();
        }
    }

    @Override
    public int getItemCount() {
        return (tracks.isEmpty() ? 0 : 1) + tracks.list().size() + (footerMode != FooterMode.HIDDEN ? 1 : 0);
    }

    public boolean isEmpty() {
        return tracks.isEmpty() && footerMode == FooterMode.HIDDEN;
    }

    @Override
    public int getItemViewType(int position) {
        int index = position;
        if (!tracks.isEmpty()) {
            if (position == 0) {
                return HEADER_VIEW_TYPE;
            }
            index--;
        }
        return index < tracks.list().size() ? ITEM_VIEW_TYPE : FOOTER_VIEW_TYPE;
    }

    FooterMode getFooterMode() {
        return footerMode;
    }

    void setFooterMode(FooterMode footerMode) {
        if (this.footerMode == footerMode) {
            return;
        }
        int position = tracks.list().size();
        if (this.footerMode == FooterMode.HIDDEN) {
            this.footerMode = footerMode;
            notifyItemInserted(position);
        } else if (footerMode == FooterMode.HIDDEN) {
            this.footerMode = footerMode;
            notifyItemRemoved(position);
        } else {
            this.footerMode = footerMode;
            notifyItemChanged(position);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ITEM_VIEW_TYPE) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.history_record, parent, false);
            return new ItemViewHolder(view);
        }
        if (viewType == HEADER_VIEW_TYPE) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.history_header, parent, false);
            return new HeaderViewHolder(view);
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.podcast_footer, parent, false);
        return new FooterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (holder instanceof ItemViewHolder) {
            ((ItemViewHolder) holder).bind(tracks.list().get(position - 1));
        } else if (holder instanceof FooterViewHolder) {
            ((FooterViewHolder) holder).bind(footerMode);
        }
    }

    @Nullable
    HistoryTrack getItem(int position) {
        int index = position;
        if (!tracks.isEmpty()) {
            if (position == 0) {
                return null;
            }
            index--;
        }
        return index >= 0 && index < tracks.list().size() ? tracks.list().get(index) : null;
    }

    @Override
    public long getItemId(int position) {
        int index = position;
        if (!tracks.isEmpty()) {
            if (position == 0) {
                return 0;
            }
            index--;
        }
        if (index >= 0 && index < tracks.list().size()) {
            return tracks.list().get(index).getId().asLong();
        }
        return index == tracks.list().size() && footerMode != FooterMode.HIDDEN ? Long.MAX_VALUE : RecyclerView.NO_ID;
    }

    static abstract class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(View itemView) {
            super(itemView);
        }
    }

    class ItemViewHolder extends ViewHolder {
        ItemViewHolder(View itemView) {
            super(itemView);
        }

        void bind(final HistoryTrack track) {
            LighthouseApplication application = fragment.requireLighthouseActivity().getLighthouseApplication();

            final Record record = track.getRecord();

            TextView nameView = getNameView(itemView);
            nameView.setText(record.getName());
            nameView.setTypeface(application.getFontBold());

            TextView descriptionView = getDescriptionView(itemView);
            if (record.getDescription() != null) {
                descriptionView.setVisibility(View.VISIBLE);
                descriptionView.setText(record.getDescription());
            } else {
                descriptionView.setVisibility(View.GONE);
            }
            descriptionView.setTypeface(application.getFontNormal());

            TextView dateView = getDateView(itemView);
            StringBuilder builder = new StringBuilder();
            if (record.getDate() != null) {
                try {
                    Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(record.getDate());
                    builder.append(PodcastsUtils.formatDate(date.getTime()));
                } catch (ParseException e) {
                    builder.append(record.getDate());
                }
                builder.append(' ').append(application.getResources().getString(R.string.history_podcast_separator)).append(' ');
            }
            builder.append(track.getPodcast().getName());
            dateView.setText(builder.toString());
            dateView.setTypeface(application.getFontLight());

            TextView durationView = getDurationView(itemView);
            if (record.getDuration() != null || record.getLength() > 0) {
                String duration = record.getLength() > 0 ? PodcastsUtils.formatTime(record.getLength()) : record.getDuration();
                durationView.setVisibility(View.VISIBLE);
                durationView.setText(fragment.getResources().getString(R.string.record_duration, duration));
            } else {
                durationView.setVisibility(View.GONE);
            }
            durationView.setTypeface(application.getFontLight());

            TextView playTimeView = getPlayTimeView(itemView);
            if (track.getPlayTime() > 0) {
                String playDate = PodcastsUtils.formatDate(track.getPlayTime());
                playTimeView.setVisibility(View.VISIBLE);
                playTimeView.setText(playDate);
            } else {
                playTimeView.setVisibility(View.GONE);
            }
            playTimeView.setTypeface(application.getFontLight());

            updatePlayPauseState(track);

            View menuView = getMenuView(itemView);
            menuView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    fragment.onCreatePopupMenu(track, view);
                }
            });

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    fragment.playRecord(track);
                }
            });
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    view.showContextMenu();
                    return true;
                }
            });
            itemView.setOnCreateContextMenuListener(fragment);

            ProgressBar progressBar = getProgressBar(itemView);
            if (record.getPosition() == Record.POSITION_UNDEFINED) {
                progressBar.setProgress(0);
            } else if (record.getLength() == 0) {
                progressBar.setProgress(1000);
            } else {
                int progress = (int) ((long) record.getPosition() * 1000 / record.getLength());
                progressBar.setProgress(progress);
            }
        }

        void updatePlayPauseState(HistoryTrack track) {
            if (equalizerDrawable == null) {
                return;
            }
            LighthouseTrack currentTrack = fragment.requireLighthouseActivity().getTrack();
            ImageView iconView = getIconView(itemView);
            if (currentTrack == null || !currentTrack.getId().equals(track.getId())) {
                iconView.setImageResource(R.drawable.record_play);
            } else if (fragment.requireLighthouseActivity().isPlaying()) {
                iconView.setImageDrawable(equalizerDrawable);
                equalizerDrawable.start();
            } else {
                iconView.setImageResource(R.drawable.record_equalizer);
                equalizerDrawable.stop();
            }
        }

        private TextView getNameView(View view) {
            return view.findViewById(R.id.name);
        }

        private TextView getDescriptionView(View view) {
            return view.findViewById(R.id.description);
        }

        private TextView getDateView(View view) {
            return view.findViewById(R.id.date);
        }

        private TextView getDurationView(View view) {
            return view.findViewById(R.id.duration);
        }

        private TextView getPlayTimeView(View view) {
            return view.findViewById(R.id.playtime);
        }

        private ImageView getIconView(View view) {
            return view.findViewById(android.R.id.icon);
        }

        private ImageView getMenuView(View view) {
            return view.findViewById(R.id.menu);
        }

        private ProgressBar getProgressBar(View view) {
            return view.findViewById(android.R.id.progress);
        }
    }

    class HeaderViewHolder extends ViewHolder {
        HeaderViewHolder(View itemView) {
            super(itemView);
        }
    }

    class FooterViewHolder extends ViewHolder {
        FooterViewHolder(View itemView) {
            super(itemView);
        }

        void bind(FooterMode footerMode) {
            LighthouseApplication application = fragment.requireLighthouseActivity().getLighthouseApplication();

            TextView moreView = getMoreView(itemView);
            moreView.setTypeface(application.getFontNormal());
            moreView.setVisibility(footerMode == FooterMode.MORE ? View.VISIBLE : View.INVISIBLE);
            moreView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    fragment.loadMore();
                }
            });

            getErrorTextView(itemView).setTypeface(application.getFontNormal());

            Button retryButton = getRetryButton(itemView);
            retryButton.setTypeface(application.getFontNormal());
            retryButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    fragment.refreshTracks();
                }
            });

            getErrorView(itemView).setVisibility(footerMode == FooterMode.ERROR ? View.VISIBLE : View.GONE);
            getProgressBar(itemView).setVisibility(footerMode == FooterMode.LOADING ? View.VISIBLE : View.INVISIBLE);
        }

        private TextView getMoreView(View view) {
            return view.findViewById(android.R.id.text1);
        }

        private View getErrorView(View view) {
            return view.findViewById(R.id.error);
        }

        private TextView getErrorTextView(View view) {
            return view.findViewById(android.R.id.text2);
        }

        private Button getRetryButton(View view) {
            return view.findViewById(R.id.retry);
        }

        private ProgressBar getProgressBar(View view) {
            return view.findViewById(android.R.id.progress);
        }
    }
}
