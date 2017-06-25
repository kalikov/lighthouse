package ru.radiomayak.podcasts;

class PodcastResponse {
    private final Podcast podcast;
    private final RecordsPaginator paginator;

    PodcastResponse(Podcast podcast, RecordsPaginator paginator) {
        this.podcast = podcast;
        this.paginator = paginator;;
    }

    Podcast getPodcast() {
        return podcast;
    }

    RecordsPaginator getPaginator() {
        return paginator;
    }
}
