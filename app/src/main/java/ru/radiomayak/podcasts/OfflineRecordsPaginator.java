package ru.radiomayak.podcasts;

import android.content.Context;

import java.util.List;

class OfflineRecordsPaginator implements RecordsPaginator {
    private final long podcast;
    private final List<Record> records;
    private final int pageSize;

    OfflineRecordsPaginator(long podcast, List<Record> records, int pageSize) {
        this.podcast = podcast;
        this.records = records;
        this.pageSize = pageSize;
    }

    @Override
    public List<Record> current() {
        return records;
    }

    @Override
    public boolean hasNext() {
        return records.size() > pageSize;
    }

    @Override
    public RecordsPaginator advance(Context context) {
        long from = records.get(pageSize - 1).getId();
        List<Record> nextRecords = PodcastsUtils.loadRecords(context, podcast, from, pageSize + 1);
        return new OfflineRecordsPaginator(podcast, nextRecords, pageSize);
    }
}
