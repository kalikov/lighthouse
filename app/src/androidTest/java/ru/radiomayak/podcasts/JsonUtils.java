package ru.radiomayak.podcasts;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class JsonUtils {
    private JsonUtils() {
    }

    public static JSONArray toJson(Podcasts podcasts) {
        JSONArray array = new JSONArray();
        for (Podcast podcast : podcasts.list()) {
            array.put(toJson(podcast));
        }
        return array;
    }

    public static JSONObject toJson(Podcast podcast) {
        JSONObject json = new JSONObject();
        try {
            json.put("id", podcast.getId());
            json.put("name", podcast.getName());
            if (podcast.getDescription() != null) {
                json.put("description", podcast.getDescription());
            }
            if (podcast.getLength() > 0) {
                json.put("length", podcast.getLength());
            }
            if (podcast.getIcon() != null) {
                json.put("icon", toJson(podcast.getIcon()));
            }
            if (podcast.getSplash() != null) {
                json.put("splash", toJson(podcast.getSplash()));
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return json;
    }

    public static JSONArray toJson(Records records) {
        JSONArray array = new JSONArray();
        for (Record record : records.list()) {
            array.put(toJson(record));
        }
        return array;
    }

    public static JSONObject toJson(Record record) {
        JSONObject json = new JSONObject();
        try {
            json.put("id", record.getId());
            json.put("name", record.getName());
            json.put("url", record.getUrl());
            json.put("description", record.getDescription());
            json.put("date", record.getDate());
            json.put("duration", record.getDuration());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return json;
    }

    public static JSONObject toJson(Image image) {
        JSONObject json = new JSONObject();
        try {
            json.put("url", image.getUrl());
            if (image.hasColor()) {
                JSONObject colors = new JSONObject();
                colors.put("primary", image.getPrimaryColor());
                colors.put("secondary", image.getSecondaryColor());
                json.put("colors", colors);
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return json;
    }
}
