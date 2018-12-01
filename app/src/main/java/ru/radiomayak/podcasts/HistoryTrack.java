package ru.radiomayak.podcasts;

import android.support.annotation.NonNull;

import ru.radiomayak.TrackId;

public class HistoryTrack {
    private final TrackId id;
    private final Podcast podcast;
    private final Record record;
    private long playTime;

    public HistoryTrack(@NonNull Podcast podcast, @NonNull Record record, long playTime) {
        this.podcast = podcast;
        this.record = record;
        this.playTime = playTime;
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

    public long getPlayTime() {
        return playTime;
    }

    public boolean update(HistoryTrack track) {
        boolean updated = podcast.update(track.podcast);
        if (record.update(track.record)) {
            updated = true;
        }
        if (track.playTime != playTime) {
            playTime = track.playTime;
            updated = true;
        }
        return updated;
    }
}
