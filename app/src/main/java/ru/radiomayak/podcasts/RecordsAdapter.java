package ru.radiomayak.podcasts;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import ru.radiomayak.R;

class RecordsAdapter extends RecyclerView.Adapter<RecordsAdapter.ViewHolder> {
    static final int ITEM_VIEW_TYPE = 0;
    static final int FOOTER_VIEW_TYPE = 1;

    private final LighthouseApplication application;
    private final Context context;
    private final List<Record> records;
    private FooterMode footerMode;

    private final RecordsPlayer player;

    private final String[] months;

    enum FooterMode {
        HIDDEN,
        LOADING,
        BUTTON
    }

    RecordsAdapter(LighthouseApplication application, Context context, List<Record> records, RecordsPlayer player) {
        this.application = application;
        this.context = context;
        this.records = records;
        this.player = player;
        footerMode = FooterMode.HIDDEN;
        setHasStableIds(true);

        months = application.getString(R.string.months).split(",");
    }

    @Override
    public int getItemCount() {
        return records.size() + (footerMode != FooterMode.HIDDEN ? 1 : 0);
    }

    public boolean isEmpty() {
        return records.isEmpty() && footerMode == FooterMode.HIDDEN;
    }

    @Override
    public int getItemViewType(int position) {
        return position < records.size() ? ITEM_VIEW_TYPE : FOOTER_VIEW_TYPE;
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
        if (viewType == 0) {
            View view = LayoutInflater.from(context).inflate(R.layout.podcast_record, parent, false);
            return new ItemViewHolder(view);
        }
        View view = LayoutInflater.from(context).inflate(R.layout.podcast_loadmore, parent, false);
        return new FooterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (holder instanceof ItemViewHolder) {
            ((ItemViewHolder) holder).bind(records.get(position));
        } else if (holder instanceof FooterViewHolder) {
            ((FooterViewHolder) holder).bind(footerMode);
        }
    }

    @Override
    public long getItemId(int position) {
        return position < records.size() ? records.get(position).getId() : 0;
    }

    private static TextView getNameView(View view) {
        return (TextView) view.findViewById(R.id.name);
    }

    private static TextView getDescriptionView(View view) {
        return (TextView) view.findViewById(R.id.description);
    }

    private static TextView getDateView(View view) {
        return (TextView) view.findViewById(R.id.date);
    }

    private static TextView getDurationView(View view) {
        return (TextView) view.findViewById(R.id.duration);
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
