package ru.radiomayak.podcasts;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;

import ru.radiomayak.JsonUtils;
import ru.radiomayak.Jsonable;
import ru.radiomayak.StringUtils;

public class Record implements Parcelable, Jsonable {
    private static final String PROP_ID = "id";
    private static final String PROP_NAME = "name";
    private static final String PROP_URL = "url";
    private static final String PROP_DESC = "description";
    private static final String PROP_DATE = "date";
    private static final String PROP_DURATION = "duration";
    private static final String PROP_PLAYED = "played";

    public static final Creator<Record> CREATOR = new Creator<Record>() {
        @Override
        public Record createFromParcel(Parcel in) {
            return new Record(in);
        }

        @Override
        public Record[] newArray(int size) {
            return new Record[size];
        }
    };

    private final long id;
    private final String name;
    private final String url;
    private String description;
    private String date;
    private String duration;
    private boolean isPlayed;

    protected Record(Parcel in) {
        id = in.readLong();
        name = in.readString();
        url = in.readString();
        description = readStringFromParcel(in);
        date = readStringFromParcel(in);
        duration = readStringFromParcel(in);
        isPlayed = in.readByte() != 0;
    }

    public Record(long id, String name, String url, @Nullable URI uri) {
        this(id, name, uri == null ? url : uri.resolve(url).toString());
    }

    public Record(long id, String name, String url) {
        this.id = id;
        this.name = StringUtils.requireNonEmpty(name);
        this.url = StringUtils.requireNonEmpty(url);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(id);
        out.writeString(name);
        out.writeString(url);
        writeStringToParcel(out, description);
        writeStringToParcel(out, date);
        writeStringToParcel(out, duration);
        out.writeByte(isPlayed ? (byte) 1 : 0);
    }

    @Nullable
    private static String readStringFromParcel(Parcel in) {
        return in.readInt() > 0 ? in.readString() : null;
    }

    private static void writeStringToParcel(Parcel out, String string) {
        if (string == null || string.isEmpty()) {
            out.writeInt(0);
        } else {
            out.writeInt(string.length());
            out.writeString(string);
        }
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = StringUtils.nonEmpty(description);
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = StringUtils.nonEmpty(date);
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = StringUtils.nonEmpty(duration);
    }

    public boolean isPlayed() {
        return isPlayed;
    }

    public void setPlayed(boolean isPlayed) {
        this.isPlayed = isPlayed;
    }

    public boolean merge(Record record) {
        boolean updated = false;
        if (record.getDescription() != null) {
            updated = !StringUtils.equals(description, record.getDescription());
            description = record.getDescription();
        }
        if (record.getDate() != null) {
            updated = updated || !StringUtils.equals(date, record.getDate());
            date = record.getDate();
        }
        if (record.getDuration() != null) {
            updated = updated || !StringUtils.equals(duration, record.getDuration());
            duration = record.getDuration();
        }
        if (record.isPlayed() && !isPlayed) {
            updated = true;
            isPlayed = true;
        }
        return updated;
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put(PROP_ID, id);
            json.put(PROP_NAME, name);
            json.put(PROP_URL, url);
            json.put(PROP_DESC, description);
            json.put(PROP_DATE, date);
            json.put(PROP_DURATION, duration);
            json.put(PROP_PLAYED, isPlayed);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return json;
    }

    @Nullable
    public static Record fromJson(JSONObject json) {
        long id = JsonUtils.getOptLong(json, PROP_ID, 0);
        if (id <= 0) {
            return null;
        }
        String name = JsonUtils.getOptString(json, PROP_NAME);
        if (name == null || name.isEmpty()) {
            return null;
        }
        String url = JsonUtils.getOptString(json, PROP_URL);
        if (url == null || url.isEmpty()) {
            return null;
        }
        Record record = new Record(id, name, url);
        record.setDescription(StringUtils.nonEmpty(JsonUtils.getOptString(json, PROP_DESC)));
        record.setDate(StringUtils.nonEmpty(JsonUtils.getOptString(json, PROP_DATE)));
        record.setDuration(StringUtils.nonEmpty(JsonUtils.getOptString(json, PROP_DURATION)));
        record.setPlayed(JsonUtils.getOptBoolean(json, PROP_PLAYED, false));
        return record;
    }
}
