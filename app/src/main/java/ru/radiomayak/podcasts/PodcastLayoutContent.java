package ru.radiomayak.podcasts;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

class PodcastLayoutContent {
    private final Podcast podcast;
    private final boolean isArchived;
    private final Records records;
    private final long nextPage;

    PodcastLayoutContent(@Nullable Podcast podcast, boolean isArchived, @NonNull Records records, long nextPage) {
        this.podcast = podcast;
        this.isArchived = isArchived;
        this.records = records;
        this.nextPage = nextPage;
    }

    @Nullable
    Podcast getPodcast() {
        return podcast;
    }

    public boolean isArchived() {
        return isArchived;
    }

    @NonNull
    Records getRecords() {
        return records;
    }

    long getNextPage() {
        return nextPage;
    }
}
