package ru.radiomayak.podcasts;

import android.app.Fragment;
import android.os.Bundle;

public class RecordsDataFragment extends Fragment {
    private Records records;
    private RecordsPaginator paginator;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setRetainInstance(true);
    }

    public Records getRecords() {
        return records;
    }

    public void setRecords(Records records) {
        this.records = records;
    }

    public RecordsPaginator getPaginator() {
        return paginator;
    }

    public void setPaginator(RecordsPaginator paginator) {
        this.paginator = paginator;
    }
}
