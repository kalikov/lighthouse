package ru.radiomayak.podcasts;

import android.support.annotation.Nullable;
import android.support.graphics.drawable.AnimatedVectorDrawableCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import ru.radiomayak.LighthouseApplication;
import ru.radiomayak.LighthouseTrack;
import ru.radiomayak.R;

class RecordsAdapter extends RecyclerView.Adapter<RecordsAdapter.ViewHolder> {
    static final int ITEM_VIEW_TYPE = 0;
    static final int HEADER_VIEW_TYPE = 1;
    static final int FOOTER_VIEW_TYPE = 2;

    private final LighthouseApplication application;
    private final Podcast podcast;
    private final List<Record> records;
    private final AnimatedVectorDrawableCompat equalizerDrawable;

    private final RecordsPlayer player;

    private final String[] months;

    private FooterMode footerMode;

    enum FooterMode {
        HIDDEN,
        LOADING,
        BUTTON
    }

    RecordsAdapter(LighthouseApplication application, Podcast podcast, List<Record> records, RecordsPlayer player) {
        this.application = application;
        this.podcast = podcast;
        this.records = records;
        this.player = player;
        footerMode = FooterMode.HIDDEN;
        setHasStableIds(true);

        months = application.getString(R.string.months).split(",");

        equalizerDrawable = AnimatedVectorDrawableCompat.create(application, R.drawable.record_equalizer_animated);
    }

    void updateEqualizerAnimation() {
        if (equalizerDrawable != null && equalizerDrawable.isRunning() && !application.getMediaPlayer().isPlaying()) {
            equalizerDrawable.stop();
        }
    }

    @Override
    public int getItemCount() {
        if (records.isEmpty()) {
            return 0;
        }
        return (podcast.getLength() > 0 ? 1 : 0) + records.size() + (footerMode != FooterMode.HIDDEN ? 1 : 0);
    }

    public boolean isEmpty() {
        return records.isEmpty();
    }

    @Override
    public int getItemViewType(int position) {
        int index = position;
        if (podcast.getLength() > 0) {
            if (position == 0) {
                return HEADER_VIEW_TYPE;
            }
            index--;
        }
        return index < records.size() ? ITEM_VIEW_TYPE : FOOTER_VIEW_TYPE;
    }

    FooterMode getFooterMode() {
        return footerMode;
    }

    void setFooterMode(FooterMode footerMode) {
        if (this.footerMode == footerMode) {
            return;
        }
        if (this.footerMode == FooterMode.HIDDEN) {
            this.footerMode = footerMode;
            notifyItemInserted(records.size());
        } else if (footerMode == FooterMode.HIDDEN) {
            this.footerMode = footerMode;
            notifyItemRemoved(records.size());
        } else {
            this.footerMode = footerMode;
            notifyItemChanged(records.size());
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == ITEM_VIEW_TYPE) {
            View view = LayoutInflater.from(application).inflate(R.layout.podcast_record, parent, false);
            return new ItemViewHolder(view);
        }
        if (viewType == HEADER_VIEW_TYPE) {
            View view = LayoutInflater.from(application).inflate(R.layout.podcast_header, parent, false);
            return new HeaderViewHolder(view);
        }
        View view = LayoutInflater.from(application).inflate(R.layout.podcast_more, parent, false);
        return new FooterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (holder instanceof ItemViewHolder) {
            int index = podcast.getLength() > 0 ? position - 1 : position;
            ((ItemViewHolder) holder).bind(records.get(index));
        } else if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind(podcast);
        } else if (holder instanceof FooterViewHolder) {
            ((FooterViewHolder) holder).bind(footerMode);
        }
    }

    @Nullable
    Record getItem(int position) {
        int index = position;
        if (podcast.getLength() > 0) {
            if (position == 0) {
                return null;
            }
            index--;
        }
        return index < records.size() ? records.get(index) : null;
    }

    @Override
    public long getItemId(int position) {
        int index = position;
        if (podcast.getLength() > 0) {
            if (position == 0) {
                return 0;
            }
            index--;
        }
        return index < records.size() ? records.get(index).getId() : -1;
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

        void bind(final Record record) {
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
            if (record.getDate() != null) {
                dateView.setVisibility(View.VISIBLE);
                try {
                    Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(record.getDate());
                    Calendar calendar = new GregorianCalendar();
                    calendar.setTime(date);
                    String text = calendar.get(Calendar.DAY_OF_MONTH) + " " + months[calendar.get(Calendar.MONTH)] + " " + calendar.get(Calendar.YEAR);
                    dateView.setText(text);
                } catch (ParseException e) {
                    dateView.setText(record.getDate());
                }
            } else {
                dateView.setVisibility(View.GONE);
            }
            dateView.setTypeface(application.getFontLight());

            TextView durationView = getDurationView(itemView);
            if (record.getDuration() != null) {
                durationView.setVisibility(View.VISIBLE);
                durationView.setText(application.getResources().getString(R.string.record_duration, record.getDuration()));
            } else {
                durationView.setVisibility(View.GONE);
            }
            durationView.setTypeface(application.getFontLight());

            updatePlayPauseState();

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    player.playRecord(record);
                }
            });
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    return true;
                }
            });
        }

        void updatePlayPauseState() {
            if (equalizerDrawable == null) {
                return;
            }
            LighthouseTrack track = application.getTrack();
            ImageView iconView = getIconView(itemView);
            Record record = getItem(getAdapterPosition());
            if (record == null || track == null || track.getPodcast().getId() != podcast.getId() || track.getRecord().getId() != record.getId()) {
                iconView.setImageResource(R.drawable.record_play);
            } else if (application.getMediaPlayer().isPlaying()) {
                iconView.setImageDrawable(equalizerDrawable);
                equalizerDrawable.start();
            } else {
                iconView.setImageResource(R.drawable.record_equalizer);
                equalizerDrawable.stop();
            }
            getDoneIconView(itemView).setVisibility(record == null || !record.isPlayed() ? View.INVISIBLE : View.VISIBLE);
        }

        private TextView getNameView(View view) {
            return (TextView) view.findViewById(R.id.name);
        }

        private TextView getDescriptionView(View view) {
            return (TextView) view.findViewById(R.id.description);
        }

        private TextView getDateView(View view) {
            return (TextView) view.findViewById(R.id.date);
        }

        private TextView getDurationView(View view) {
            return (TextView) view.findViewById(R.id.duration);
        }

        private ImageView getIconView(View view) {
            return (ImageView) view.findViewById(android.R.id.icon);
        }

        private ImageView getDoneIconView(View view) {
            return (ImageView) view.findViewById(android.R.id.icon1);
        }
    }

    class HeaderViewHolder extends ViewHolder {
        HeaderViewHolder(View itemView) {
            super(itemView);
        }

        void bind(Podcast podcast) {
            TextView lengthView = getLengthView(itemView);
            lengthView.setTypeface(application.getFontNormal());
            String format = application.getString(R.string.records_count);
            lengthView.setText(String.format(format, String.valueOf(podcast.getLength())));
        }

        private TextView getLengthView(View view) {
            return (TextView) view.findViewById(R.id.length);
        }
    }

    class FooterViewHolder extends ViewHolder {
        FooterViewHolder(View itemView) {
            super(itemView);
        }

        void bind(FooterMode footerMode) {
            getTextView(itemView).setVisibility(footerMode == FooterMode.BUTTON ? View.VISIBLE : View.INVISIBLE);
            getProgressBar(itemView).setVisibility(footerMode == FooterMode.LOADING ? View.VISIBLE : View.INVISIBLE);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    player.loadMore();
                }
            });
        }

        private TextView getTextView(View view) {
            return (TextView) view.findViewById(android.R.id.text1);
        }

        private ProgressBar getProgressBar(View view) {
            return (ProgressBar) view.findViewById(android.R.id.progress);
        }
    }
}
