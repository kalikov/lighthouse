package ru.radiomayak.podcasts;

import android.content.Context;
import android.os.Parcelable;

interface RecordsPaginator extends Parcelable {
    Iterable<Record> current();

    boolean hasNext();

    RecordsPaginator advance(Context context);
}
