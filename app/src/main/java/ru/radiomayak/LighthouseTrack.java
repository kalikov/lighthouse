package ru.radiomayak;

import android.support.annotation.NonNull;

import ru.radiomayak.podcasts.Podcast;
import ru.radiomayak.podcasts.Record;

public class LighthouseTrack {
    private final Podcast podcast;
    private final Record record;

    public LighthouseTrack(@NonNull Podcast podcast, @NonNull Record record) {
        this.podcast = podcast;
        this.record = record;
    }

    public Podcast getPodcast() {
        return podcast;
    }

    public Record getRecord() {
        return record;
    }
}
