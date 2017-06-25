package ru.radiomayak.podcasts;

import android.support.annotation.Nullable;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.radiomayak.NetworkUtils;
import ru.radiomayak.StringUtils;

class PodcastLayoutParser {
    private static final String PODCAST_ID_QUERY = "#podcast-id";
    private static final String PODCAST_LOGO_QUERY = "img.b-podcast__pic";
    private static final String PODCAST_NEXT_PAGE_QUERY = "a.b-podcast__records-show-more__btn";

    private static final String RECORD_QUERY = ".b-podcast__records-item";
    private static final String ANCHOR_QUERY = "a.b-podcast__records-listen";
    private static final String NAME_QUERY = ".b-podcast__records-name";
    private static final String DESCRIPTION_QUERY = ".b-podcast__records-description__text";
    private static final String DATE_QUERY = ".b-podcast__records-date";
    private static final String DURATION_QUERY = ".b-podcast__records-time";

    private static final Pattern RECORD_URL_ID_PATTERN = Pattern.compile(".+listen\\?id=(\\d+).*");

    private static final Pattern JSON_DATE_PATTERN = Pattern.compile("(\\d{2})\\-(\\d{2})\\-(\\d{4})");
    private static final Pattern HTML_DATE_PATTERN = Pattern.compile("\\:[\\u00A0\\s]*(\\d{2})\\.(\\d{2})\\.(\\d{4})");

    private static final Pattern JSON_DURATION_PATTERN = Pattern.compile("((\\d{2}\\:)?\\d{2}\\:\\d{2})");
    private static final Pattern HTML_DURATION_PATTERN = Pattern.compile("\\:[\\u00A0\\s]*((\\d{2}\\:)?\\d{2}\\:\\d{2})");

    PodcastLayoutContent parse(InputStream stream, String charset, String baseUri) throws IOException {
        Document document = Jsoup.parse(stream, charset, baseUri);
        URI uri = NetworkUtils.toOptURI(baseUri);
        Podcast podcast = parsePodcast(document, uri);
        Records records = parseRecords(document, uri);
        long nextPage = StringUtils.parseLong(document.select(PODCAST_NEXT_PAGE_QUERY).attr("data-page"), 0);
        return new PodcastLayoutContent(podcast, records, nextPage);
    }

    @Nullable
    private static Podcast parsePodcast(Document document, @Nullable URI uri) {
        long id = StringUtils.parseLong(document.select(PODCAST_ID_QUERY).val(), 0);
        if (id == 0) {
            return null;
        }
        Elements logo = document.select(PODCAST_LOGO_QUERY);
        String name = StringUtils.nonEmpty(logo.attr("title"));
        if (name == null) {
            return null;
        }
        Podcast podcast = new Podcast(id, name);
        String logoUrl = StringUtils.nonEmpty(logo.attr("src"));
        if (logoUrl != null) {
            podcast.setSplash(new Image(logoUrl, uri));
        }
        return podcast;
    }

    private static Records parseRecords(Document document, @Nullable URI uri) {
        Elements items = document.select(RECORD_QUERY);

        Records records = new Records(items.size());
        for (Element item : items) {
            Record record = parsePodcastRecord(item, uri);
            if (record != null) {
                records.add(record);
            }
        }
        return records;
    }

    @Nullable
    private static Record parsePodcastRecord(Element element, @Nullable URI uri) {
        Elements anchor = element.select(ANCHOR_QUERY);
        String url = anchor.attr("data-url");
        if (url == null || url.isEmpty()) {
            return null;
        }
        long id = extractId(anchor, url);
        if (id == 0) {
            return null;
        }

        String name = element.select(NAME_QUERY).text();
        if (name == null || name.isEmpty()) {
            name = anchor.attr("data-title");
            if (name == null || name.isEmpty()) {
                return null;
            }
        }
        Record record = new Record(id, name, url, uri);
        record.setDescription(element.select(DESCRIPTION_QUERY).text());
        record.setDate(extractDate(element.select(DATE_QUERY).text()));
        record.setDuration(extractDuration(element.select(DURATION_QUERY).text()));
        return record;
    }

    private static long extractId(Elements anchor, String url) {
        String id = anchor.attr("data-id");
        if (id == null || id.isEmpty()) {
            return StringUtils.parseLong(extractId(url), 0);
        }
        return StringUtils.parseLong(id, 0);
    }

    private static String extractId(String url) {
        Matcher matcher = RECORD_URL_ID_PATTERN.matcher(url);
        if (!matcher.matches()) {
            return null;
        }
        return matcher.group(1);
    }

    @Nullable
    private static String extractDate(@Nullable String string) {
        if (string == null || string.isEmpty()) {
            return null;
        }
        Matcher htmlMatcher = HTML_DATE_PATTERN.matcher(string);
        if (htmlMatcher.find()) {
            return htmlMatcher.group(3) + '-' + htmlMatcher.group(2) + '-' + htmlMatcher.group(1);
        }
        Matcher jsonMatcher = JSON_DATE_PATTERN.matcher(string);
        if (jsonMatcher.find()) {
            return jsonMatcher.group(3) + '-' + jsonMatcher.group(2) + '-' + jsonMatcher.group(1);
        }
        return null;
    }

    @Nullable
    private static String extractDuration(@Nullable String string) {
        if (string == null || string.isEmpty()) {
            return null;
        }
        Matcher htmlMatcher = HTML_DURATION_PATTERN.matcher(string);
        String value;
        if (htmlMatcher.find()) {
            value = htmlMatcher.group(1);
        } else {
            Matcher jsonMatcher = JSON_DURATION_PATTERN.matcher(string);
            if (!jsonMatcher.find()) {
                return null;
            }
            value = jsonMatcher.group(1);
        }
        return value.length() == 5 ? "00:" + value : value;
    }
}
