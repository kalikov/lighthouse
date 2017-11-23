package ru.radiomayak.podcasts;

import android.content.Context;
import android.os.Parcelable;

import ru.radiomayak.LighthouseApplication;

interface RecordsPaginator extends Parcelable {
    Iterable<Record> current();

    boolean hasNext();

    RecordsPaginator advance(LighthouseApplication context);
}
