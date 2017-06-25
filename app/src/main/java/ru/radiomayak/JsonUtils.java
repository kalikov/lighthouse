package ru.radiomayak;

import android.support.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

public final class JsonUtils {
    private static final JsonParser parser = new JsonParser();

    private JsonUtils() {
    }

    public static JsonElement parse(Reader reader) {
        return parser.parse(reader);
    }

    public static void write(Writer writer, JsonElement element) throws IOException {
        JsonWriter jsonWriter = new JsonWriter(writer);
        jsonWriter.setLenient(true);
        Streams.write(element, jsonWriter);
    }

    public static JsonArray toJsonArray(Iterable<? extends Jsonable> items) {
        JsonArray array = new JsonArray();
        for (Jsonable item : items) {
            array.add(item.toJson());
        }
        return array;
    }

    public static String toString(Iterable<? extends Jsonable> items) {
        return toJsonArray(items).toString();
    }

    @Nullable
    public static String getOptString(JsonObject object, String property) {
        if (!object.has(property)) {
            return null;
        }
        JsonElement element = object.get(property);
        if (element.isJsonPrimitive()) {
            return element.getAsString();
        }
        return null;
    }

    public static long getOptLong(JsonObject object, String property, long defaultValue) {
        if (!object.has(property)) {
            return defaultValue;
        }
        JsonElement element = object.get(property);
        if (element.isJsonPrimitive()) {
            try {
                return element.getAsLong();
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    public static int getOptInt(JsonObject object, String property, int defaultValue) {
        if (!object.has(property)) {
            return defaultValue;
        }
        JsonElement element = object.get(property);
        if (element.isJsonPrimitive()) {
            try {
                return element.getAsInt();
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    public static boolean getOptBoolean(JsonObject object, String property, boolean defaultValue) {
        if (!object.has(property)) {
            return defaultValue;
        }
        JsonElement element = object.get(property);
        if (element.isJsonPrimitive() && ((JsonPrimitive) element).isBoolean()) {
            try {
                return element.getAsBoolean();
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }
}
