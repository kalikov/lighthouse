package ru.radiomayak;

import android.support.annotation.Nullable;
import android.util.JsonReader;

//import com.google.gson.JsonArray;
//import com.google.gson.JsonElement;
//import com.google.gson.JsonObject;
//import com.google.gson.JsonParser;
//import com.google.gson.JsonPrimitive;
//import com.google.gson.internal.Streams;
//import com.google.gson.stream.JsonWriter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

public final class JsonUtils {
//    private static final JsonReader parser = new JsonReader();

    private JsonUtils() {
    }

//    public static Object parse(Reader reader) {
//        reader.
//        JSONTokener parser = new JSONTokener(reader);
//        return JSON..
//    }
//
//    public static void write(Writer writer, Object element) throws IOException {
//        JsonWriter jsonWriter = new JsonWriter(writer);
//        jsonWriter.setLenient(true);
//        Streams.write(element, jsonWriter);
//    }

    public static JSONArray toJsonArray(Iterable<? extends Jsonable> items) {
        JSONArray array = new JSONArray();
        for (Jsonable item : items) {
            array.put(item.toJson());
        }
        return array;
    }

    public static String toString(Iterable<? extends Jsonable> items) {
        return toJsonArray(items).toString();
    }

    @Nullable
    public static String getOptString(JSONObject object, String property) {
        if (!object.has(property)) {
            return null;
        }
        try {
            return object.getString(property);
        } catch (JSONException ignored) {
            return null;
        }
    }

    public static long getOptLong(JSONObject object, String property, long defaultValue) {
        if (!object.has(property)) {
            return defaultValue;
        }
        try {
            return object.getLong(property);
        } catch (JSONException ignored) {
            return defaultValue;
        }
    }

    public static int getOptInt(JSONObject object, String property, int defaultValue) {
        if (!object.has(property)) {
            return defaultValue;
        }
        try {
            return object.getInt(property);
        } catch (JSONException ignored) {
            return defaultValue;
        }
    }

    public static boolean getOptBoolean(JSONObject object, String property, boolean defaultValue) {
        if (!object.has(property)) {
            return defaultValue;
        }
        try {
            return object.getBoolean(property);
        } catch (JSONException ignored) {
            return defaultValue;
        }
    }
}
