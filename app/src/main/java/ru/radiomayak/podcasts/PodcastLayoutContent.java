package ru.radiomayak.podcasts;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

class PodcastLayoutContent {
    private final Podcast podcast;
    private final Records records;
    private final long nextPage;

    PodcastLayoutContent(@Nullable Podcast podcast, @NonNull Records records, long nextPage) {
        this.podcast = podcast;
        this.records = records;
        this.nextPage = nextPage;
    }

    @Nullable
    Podcast getPodcast() {
        return podcast;
    }

    @NonNull
    Records getRecords() {
        return records;
    }

    long getNextPage() {
        return nextPage;
    }
}
