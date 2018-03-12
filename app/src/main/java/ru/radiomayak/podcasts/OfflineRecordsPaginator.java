package ru.radiomayak.podcasts;

import android.content.Context;
import android.os.Parcel;

import java.util.ArrayList;
import java.util.List;

import ru.radiomayak.LighthouseApplication;

class OfflineRecordsPaginator implements RecordsPaginator {
    public static final Creator<OfflineRecordsPaginator> CREATOR = new Creator<OfflineRecordsPaginator>() {
        @Override
        public OfflineRecordsPaginator createFromParcel(Parcel in) {
            return new OfflineRecordsPaginator(in);
        }

        @Override
        public OfflineRecordsPaginator[] newArray(int size) {
            return new OfflineRecordsPaginator[size];
        }
    };

    private final long podcast;
    private final List<Record> records;
    private final int pageSize;

    protected OfflineRecordsPaginator(Parcel in) {
        this.podcast = in.readLong();
        this.pageSize = in.readInt();
        int size = in.readInt();
        records = new ArrayList<>(size);
        while (size > 0) {
            Record record = Record.CREATOR.createFromParcel(in);
            records.add(record);
        }
    }

    OfflineRecordsPaginator(long podcast, List<Record> records, int pageSize) {
        this.podcast = podcast;
        this.records = records;
        this.pageSize = pageSize;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(podcast);
        out.writeInt(pageSize);
        out.writeInt(records.size());
        for (Record record : records) {
            record.writeToParcel(out, flags);
        }
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
    public RecordsPaginator advance(LighthouseApplication context) {
        long from = records.get(pageSize - 1).getId();
//        List<Record> nextRecords = PodcastsUtils.loadRecords(context, podcast, from, pageSize + 1);
//        return new OfflineRecordsPaginator(podcast, nextRecords, pageSize);
        return null;
    }
}
