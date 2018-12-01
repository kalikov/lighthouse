package ru.radiomayak.podcasts;

public class HistoryPage {
    private final HistoryTracks tracks;
    private final int cursor;

    public HistoryPage(HistoryTracks tracks, int cursor) {
        this.tracks = tracks;
        this.cursor = cursor;
    }

    public HistoryTracks getTracks() {
        return tracks;
    }

    public int getCursor() {
        return cursor;
    }
}
