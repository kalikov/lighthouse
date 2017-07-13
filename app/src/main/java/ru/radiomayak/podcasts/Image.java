package ru.radiomayak.podcasts;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;

import ru.radiomayak.JsonUtils;
import ru.radiomayak.StringUtils;

public class Image implements Parcelable {
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

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put(PROP_URL, url);
            if (primaryColor != 0) {
                JSONObject colors = new JSONObject();
                colors.put(PROP_PRIMARY_COLOR, primaryColor);
                colors.put(PROP_SECONDARY_COLOR, secondaryColor);
                json.put(PROP_COLORS, colors);
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return json;
    }

    @Nullable
    public static Image fromJson(JSONObject json) {
        String url = JsonUtils.getOptString(json, PROP_URL);
        if (url == null || url.isEmpty()) {
            return null;
        }
        try {
            if (json.has(PROP_COLORS) && json.get(PROP_COLORS) instanceof JSONObject) {
                JSONObject colors = json.getJSONObject(PROP_COLORS);
                int primaryColor = JsonUtils.getOptInt(colors, PROP_PRIMARY_COLOR, 0);
                int secondaryColor = JsonUtils.getOptInt(colors, PROP_SECONDARY_COLOR, 0);
                return new Image(url, primaryColor, secondaryColor);
            }
        } catch (JSONException ignored) {
        }
        return new Image(url);
    }
}
