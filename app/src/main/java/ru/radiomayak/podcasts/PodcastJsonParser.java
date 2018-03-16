package ru.radiomayak.podcasts;

import android.support.annotation.Nullable;
import android.util.JsonReader;
import android.util.JsonToken;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.radiomayak.StringUtils;

class PodcastJsonParser {
    private static final String TITLE_PROPERTY = "title";
    private static final String NEXT_PAGE_PROPERTY = "next_page";
    private static final String RECORDS_PROPERTY = "episodes";
    private static final String AUDIO_PROPERTY = "audio";
    private static final String SOURCES_PROPERTY = "sources";
    private static final String URL_PROPERTY = "listen";
    private static final String ID_PROPERTY = "id";
    private static final String NAME_PROPERTY = "title";
    private static final String DESCRIPTION_PROPERTY = "anons";
    private static final String DATE_PROPERTY = "datePub";
    private static final String DURATION_STRING_PROPERTY = "durationHF";
    private static final String DURATION_PROPERTY = "duration";

    private static final Pattern RECORD_URL_ID_PATTERN = Pattern.compile(".+listen\\?id=(\\d+).*");

    private static final Pattern JSON_DATE_PATTERN = Pattern.compile("(\\d{2})\\-(\\d{2})\\-(\\d{4})");
    private static final Pattern HTML_DATE_PATTERN = Pattern.compile("\\:[\\u00A0\\s]*(\\d{2})\\.(\\d{2})\\.(\\d{4})");

    private static final Pattern JSON_DURATION_PATTERN = Pattern.compile("((\\d{2}\\:)?\\d{2}\\:\\d{2})");

    PodcastLayoutContent parse(Reader reader, URI uri) throws IOException {
        JsonReader parser = new JsonReader(reader);

        long id = 0;
        String name = null;

        long nextPage = 0;

        Records records = null;

        parser.beginObject();
        while (parser.hasNext()) {
            String prop = parser.nextName();
            if (ID_PROPERTY.equals(prop)) {
                id = parser.nextLong();
            } else if (TITLE_PROPERTY.equals(prop)) {
                name = StringUtils.nonEmpty(parser.nextString());
            } else if (NEXT_PAGE_PROPERTY.equals(prop)) {
                if (parser.peek() == JsonToken.NUMBER) {
                    nextPage = parser.nextLong();
                } else {
                    parser.skipValue();
                }
            } else if (RECORDS_PROPERTY.equals(prop)) {
                records = parseRecords(parser, uri);
            } else {
                parser.skipValue();
            }
        }
        parser.endObject();

        Podcast podcast = null;
        if (id != 0 && name != null) {
            podcast = new Podcast(id, name);
        }
        return new PodcastLayoutContent(podcast, records == null ? new Records() : records, nextPage);
    }

    private static Records parseRecords(JsonReader parser, @Nullable URI uri) throws IOException {
        Records records = new Records();
        parser.beginArray();
        while (parser.hasNext()) {
            Record record = parseRecord(parser, uri);
            if (record != null) {
                records.add(record);
            }
        }
        parser.endArray();
        return records;
    }

    @Nullable
    private static Record parseRecord(JsonReader parser, @Nullable URI uri) throws IOException {
        long id = 0;
        String url = null;
        String name = null;
        String description = null;
        String date = null;
        String duration = null;

        parser.beginObject();
        while (parser.hasNext()) {
            String prop = parser.nextName();
            if (AUDIO_PROPERTY.equals(prop) && parser.peek() == JsonToken.BEGIN_ARRAY) {
                parser.beginArray();
                if (parser.hasNext()) {
                    parser.beginObject();
                    while (parser.hasNext()) {
                        String audioProp = parser.nextName();
                        if (SOURCES_PROPERTY.equals(audioProp)) {
                            parser.beginObject();
                            while (parser.hasNext()) {
                                String sourcesProp = parser.nextName();
                                if (URL_PROPERTY.equals(sourcesProp)) {
                                    url = StringUtils.nonEmpty(parser.nextString());
                                } else {
                                    parser.skipValue();
                                }
                            }
                            parser.endObject();
                        } else if (ID_PROPERTY.equals(audioProp)) {
                            id = parser.nextLong();
                        } else if (DATE_PROPERTY.equals(audioProp)) {
                            date = extractDate(parser.nextString());
                        } else if (DURATION_PROPERTY.equals(audioProp)) {
                            if (duration == null) {
                                duration = PodcastsUtils.formatSeconds(parser.nextLong());
                            } else {
                                parser.skipValue();
                            }
                        } else if (DURATION_STRING_PROPERTY.equals(audioProp)) {
                            if (duration == null) {
                                duration = extractDuration(parser.nextString());
                            } else {
                                parser.skipValue();
                            }
                        } else {
                            parser.skipValue();
                        }
                    }
                    parser.endObject();
                }
                while (parser.hasNext()) {
                    parser.skipValue();
                }
                parser.endArray();
            } else if (NAME_PROPERTY.equals(prop)) {
                name = StringUtils.nonEmpty(parser.nextString());
            } else if (DESCRIPTION_PROPERTY.equals(prop)) {
                description = StringUtils.nonEmpty(parser.nextString());
                if (description != null) {
                    description = LayoutUtils.clean(description);
                }
            } else {
                parser.skipValue();
            }
        }
        parser.endObject();

        if (url == null || name == null) {
            return null;
        }
        if (id == 0) {
            id = StringUtils.parseLong(extractId(url), 0);
            if (id == 0) {
                return null;
            }
        }
        Record record = new Record(id, name, url, uri);
        record.setDescription(description);
        record.setDate(date);
        record.setDuration(duration);
        return record;
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
        Matcher matcher = JSON_DURATION_PATTERN.matcher(string);
        if (!matcher.find()) {
            return null;
        }
        String value = matcher.group(1);
        if (value.length() > 5 && value.startsWith("00:")) {
            return value.substring(3);
        }
        return value;
    }
}
