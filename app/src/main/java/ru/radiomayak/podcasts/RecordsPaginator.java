package ru.radiomayak.podcasts;

import android.content.Context;

interface RecordsPaginator {
    Iterable<Record> current();

    boolean hasNext();

    RecordsPaginator advance(Context context);
}
