package ru.radiomayak;

import android.support.annotation.NonNull;

import ru.radiomayak.podcasts.Podcast;
import ru.radiomayak.podcasts.Record;

public class LighthouseTrack {
    private final TrackId id;
    private final Podcast podcast;
    private final Record record;

    public LighthouseTrack(@NonNull Podcast podcast, @NonNull Record record) {
        this.podcast = podcast;
        this.record = record;
        id = new TrackId(podcast.getId(), record.getId());
    }

    public TrackId getId() {
        return id;
    }

    public Podcast getPodcast() {
        return podcast;
    }

    public Record getRecord() {
        return record;
    }

    public boolean update(LighthouseTrack track) {
        boolean updated = podcast.update(track.podcast);
        return record.update(track.record) || updated;
    }
}
