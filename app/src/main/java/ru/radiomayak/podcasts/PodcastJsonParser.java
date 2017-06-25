package ru.radiomayak.podcasts;

import android.support.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.radiomayak.JsonUtils;
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
    private static final String DURATION_PROPERTY = "durationHF";

    private static final Pattern RECORD_URL_ID_PATTERN = Pattern.compile(".+listen\\?id=(\\d+).*");

    private static final Pattern JSON_DATE_PATTERN = Pattern.compile("(\\d{2})\\-(\\d{2})\\-(\\d{4})");
    private static final Pattern HTML_DATE_PATTERN = Pattern.compile("\\:[\\u00A0\\s]*(\\d{2})\\.(\\d{2})\\.(\\d{4})");

    private static final Pattern JSON_DURATION_PATTERN = Pattern.compile("((\\d{2}\\:)?\\d{2}\\:\\d{2})");
    private static final Pattern HTML_DURATION_PATTERN = Pattern.compile("\\:[\\u00A0\\s]*((\\d{2}\\:)?\\d{2}\\:\\d{2})");

    private static final JsonParser parser = new JsonParser();

    PodcastLayoutContent parse(Reader reader, URI uri) throws IOException {
        JsonElement responseElement = parser.parse(reader);
        if (!responseElement.isJsonObject()) {
            throw new UnsupportedFormatException();
        }
        JsonObject json = responseElement.getAsJsonObject();
        Podcast podcast = parsePodcast(json);
        Records records = parseRecords(json.getAsJsonArray(RECORDS_PROPERTY), uri);
        return new PodcastLayoutContent(podcast, records, JsonUtils.getOptLong(json, NEXT_PAGE_PROPERTY, 0));
    }

    private static Podcast parsePodcast(JsonObject json) {
        long id = JsonUtils.getOptLong(json, ID_PROPERTY, 0);
        if (id == 0) {
            return null;
        }
        String name = StringUtils.nonEmpty(JsonUtils.getOptString(json, TITLE_PROPERTY));
        if (name == null) {
            return null;
        }
        return new Podcast(id, name);
    }

    private static Records parseRecords(JsonArray array, @Nullable URI uri) {
        Records records = new Records(array.size());
        for (JsonElement item : array) {
            Record record = parseRecord(item.getAsJsonObject(), uri);
            if (record != null) {
                records.add(record);
            }
        }
        return records;
    }

    @Nullable
    private static Record parseRecord(JsonObject item, @Nullable URI uri) {
        JsonObject audio = getAudio(item);
        if (audio == null || !audio.has(SOURCES_PROPERTY) || !audio.get(SOURCES_PROPERTY).isJsonObject()) {
            return null;
        }
        JsonObject sources = audio.getAsJsonObject(SOURCES_PROPERTY);
        String url = JsonUtils.getOptString(sources, URL_PROPERTY);
        if (url == null || url.isEmpty()) {
            return null;
        }
        long id = extractId(audio, url);
        if (id == 0) {
            return null;
        }

        String name = JsonUtils.getOptString(item, NAME_PROPERTY);
        if (name == null || name.isEmpty()) {
            return null;
        }

        Record record = new Record(id, name, url, uri);
        record.setDescription(JsonUtils.getOptString(item, DESCRIPTION_PROPERTY));
        record.setDate(extractDate(JsonUtils.getOptString(audio, DATE_PROPERTY)));
        record.setDuration(extractDuration(JsonUtils.getOptString(audio, DURATION_PROPERTY)));
        return record;
    }

    @Nullable
    private static JsonObject getAudio(JsonObject object) {
        if (!object.has(AUDIO_PROPERTY) || !object.get(AUDIO_PROPERTY).isJsonArray()) {
            return null;
        }
        JsonArray audio = object.getAsJsonArray(AUDIO_PROPERTY);
        if (audio.size() == 0 || !audio.get(0).isJsonObject()) {
            return null;
        }
        return audio.get(0).getAsJsonObject();
    }

    private static long extractId(JsonObject object, String url) {
        JsonElement id = object.get(ID_PROPERTY);
        if (id.isJsonPrimitive() && id.getAsJsonPrimitive().isNumber()) {
            return id.getAsLong();
        }
        return StringUtils.parseLong(extractId(url), 0);
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
