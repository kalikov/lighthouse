package ru.radiomayak.podcasts;

import android.support.annotation.Nullable;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.radiomayak.NetworkUtils;
import ru.radiomayak.StringUtils;

class PodcastsLayoutJsoupParser {
    private static final String PODCAST_ITEM_QUERY = ".b-podcast__item--listed";
    private static final String PODCAST_ANCHOR_QUERY = "a.b-podcast__block-link";
    private static final String PODCAST_NAME_QUERY = ".b-podcast__description h5";
    private static final String PODCAST_DESCRIPTION_QUERY = ".b-podcast__description p";
    private static final String PODCAST_IMAGE_QUERY = "img.b-podcast__pic";
    private static final String PODCAST_LENGTH_QUERY = ".b-podcast__number";

    private static final Pattern PODCAST_HREF_PATTERN = Pattern.compile("/podcasts/podcast/id/(\\d+)/");

    Podcasts parse(ByteBuffer buffer, @Nullable String charset, String baseUri) throws IOException {
        Document document = LayoutUtils.parse(buffer, charset, baseUri);
        return parsePodcasts(document, NetworkUtils.toOptURI(baseUri));
    }

    private static Podcasts parsePodcasts(Document document, @Nullable URI uri) {
        Elements items = document.select(PODCAST_ITEM_QUERY);

        Podcasts podcasts = new Podcasts(items.size());
        for (Element item : items) {
            Podcast podcast = parsePodcast(item, uri);
            if (podcast != null) {
                podcasts.add(podcast);
            }
        }
        return podcasts;
    }

    @Nullable
    private static Podcast parsePodcast(Element element, @Nullable URI uri) {
        Elements anchor = element.select(PODCAST_ANCHOR_QUERY);
        String href = anchor.attr("href");
        if (href == null || href.isEmpty()) {
            return null;
        }
        Matcher matcher = PODCAST_HREF_PATTERN.matcher(href);
        if (!matcher.find()) {
            return null;
        }
        long id = StringUtils.parseLong(matcher.group(1), 0);
        if (id == 0) {
            return null;
        }

        String name = element.select(PODCAST_NAME_QUERY).text();
        if (name == null || name.isEmpty()) {
            name = anchor.attr("title");
            if (name == null || name.isEmpty()) {
                return null;
            }
        }
        Podcast podcast = new Podcast(id, name);
        podcast.setDescription(element.select(PODCAST_DESCRIPTION_QUERY).text());
        podcast.setLength(StringUtils.parseInt(element.select(PODCAST_LENGTH_QUERY).text(), 0));

        String image = element.select(PODCAST_IMAGE_QUERY).attr("src");
        if (image != null && !image.isEmpty()) {
            podcast.setIcon(new Image(image, uri));
        }
        return podcast;
    }
}
