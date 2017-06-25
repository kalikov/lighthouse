package ru.radiomayak.podcasts;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.google.gson.JsonObject;

import java.net.URI;

import ru.radiomayak.JsonUtils;
import ru.radiomayak.StringUtils;

public class Image implements Parcelable {
    private static final int VERSION = 1;

    private static final String PROP_URL = "url";
    private static final String PROP_COLORS = "colors";
    private static final String PROP_PRIMARY_COLOR = "primary";
    private static final String PROP_SECONDARY_COLOR = "secondary";

    public static final Creator<Image> CREATOR = new Creator<Image>() {
        @Override
        public Image createFromParcel(Parcel in) {
            return new Image(in);
        }

        @Override
        public Image[] newArray(int size) {
            return new Image[size];
        }
    };

    private final String url;
    private int primaryColor;
    private int secondaryColor;

    protected Image(Parcel in) {
        @SuppressWarnings("unused") int version = in.readInt();
        url = in.readString();
        primaryColor = in.readInt();
        secondaryColor = in.readInt();
    }

    public Image(String url, @Nullable URI uri) {
        this(uri == null ? url : uri.resolve(url).toString());
    }

    public Image(String url) {
        this.url = StringUtils.requireNonEmpty(url);
    }

    public Image(String url, int primaryColor, int secondaryColor) {
        this.url = StringUtils.requireNonEmpty(url);
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(VERSION);
        out.writeString(url);
        out.writeInt(primaryColor);
        out.writeInt(secondaryColor);
    }

    public String getUrl() {
        return url;
    }

    public int getPrimaryColor() {
        return primaryColor;
    }

    public void setPrimaryColor(int primaryColor) {
        this.primaryColor = primaryColor;
    }

    public int getSecondaryColor() {
        return secondaryColor;
    }

    public void setSecondaryColor(int secondaryColor) {
        this.secondaryColor = secondaryColor;
    }

    public void setColors(int primaryColor, int secondaryColor) {
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty(PROP_URL, url);
        if (primaryColor != 0) {
            JsonObject colors = new JsonObject();
            colors.addProperty(PROP_PRIMARY_COLOR, primaryColor);
            colors.addProperty(PROP_SECONDARY_COLOR, secondaryColor);
            json.add(PROP_COLORS, colors);
        }
        return json;
    }

    @Nullable
    public static Image fromJson(JsonObject json) {
        String url = JsonUtils.getOptString(json, PROP_URL);
        if (url == null || url.isEmpty()) {
            return null;
        }
        if (json.has(PROP_COLORS) && json.get(PROP_COLORS).isJsonObject()) {
            JsonObject colors = json.getAsJsonObject(PROP_COLORS);
            int primaryColor = JsonUtils.getOptInt(colors, PROP_PRIMARY_COLOR, 0);
            int secondaryColor = JsonUtils.getOptInt(colors, PROP_SECONDARY_COLOR, 0);
            return new Image(url, primaryColor, secondaryColor);
        }
        return new Image(url);
    }
}
