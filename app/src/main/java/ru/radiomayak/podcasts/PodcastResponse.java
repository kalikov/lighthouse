package ru.radiomayak.podcasts;

class PodcastResponse {
    private final Podcast podcast;
    private final Boolean isArchived;
    private final RecordsPaginator paginator;

    PodcastResponse(Podcast podcast, Boolean isArchived, RecordsPaginator paginator) {
        this.podcast = podcast;
        this.isArchived = isArchived;
        this.paginator = paginator;
    }

    Podcast getPodcast() {
        return podcast;
    }

    public Boolean isArchived() {
        return isArchived;
    }

    RecordsPaginator getPaginator() {
        return paginator;
    }
}
