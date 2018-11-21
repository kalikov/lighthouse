package ru.radiomayak.podcasts;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import java.net.URI;

import ru.radiomayak.StringUtils;

public class Record implements Parcelable, Identifiable {
    public static final int POSITION_UNDEFINED = -1;

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
    private int position = POSITION_UNDEFINED;
    private int length;

    protected Record(Parcel in) {
        id = in.readLong();
        name = in.readString();
        url = in.readString();
        description = readStringFromParcel(in);
        date = readStringFromParcel(in);
        duration = readStringFromParcel(in);
        position = in.readInt();
        length = in.readInt();
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
        out.writeInt(position);
        out.writeInt(length);
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

    @Override
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

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public boolean update(Record record) {
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
        if (record.getPosition() != position) {
            updated = true;
            position = record.getPosition();
        }
        if (record.getLength() != length) {
            updated = true;
            length = record.getLength();
        }
        return updated;
    }
}
