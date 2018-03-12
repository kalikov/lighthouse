package ru.radiomayak.podcasts;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import ru.radiomayak.JsonUtils;
import ru.radiomayak.Jsonable;

public class RecordFile implements Parcelable, Jsonable {
    private static final String PROP_SIZE = "size";
    private static final String PROP_CAPACITY = "capacity";

    public static final Creator<RecordFile> CREATOR = new Creator<RecordFile>() {
        @Override
        public RecordFile createFromParcel(Parcel in) {
            return new RecordFile(in);
        }

        @Override
        public RecordFile[] newArray(int size) {
            return new RecordFile[size];
        }
    };

    private final int size;
    private final int capacity;

    protected RecordFile(Parcel in) {
        size = in.readInt();
        capacity = in.readInt();
    }

    public RecordFile(int size, int capacity) {
        this.size = size;
        this.capacity = capacity;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(size);
        out.writeInt(capacity);
    }

    public int getSize() {
        return size;
    }

    public int getCapacity() {
        return capacity;
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put(PROP_SIZE, size);
            json.put(PROP_CAPACITY, capacity);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return json;
    }

    @Nullable
    public static RecordFile fromJson(JSONObject json) {
        int size = JsonUtils.getOptInt(json, PROP_SIZE, 0);
        int capacity = JsonUtils.getOptInt(json, PROP_CAPACITY, 0);
        if (size <= 0 || capacity <= 0) {
            return null;
        }
        return new RecordFile(size, capacity);
    }
}
