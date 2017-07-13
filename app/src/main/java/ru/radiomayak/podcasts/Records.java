package ru.radiomayak.podcasts;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.LongSparseArray;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.radiomayak.JsonUtils;
import ru.radiomayak.Jsonable;

public class Records implements Parcelable, Jsonable {
    private static final int VERSION = 1;

    public static final Creator<Records> CREATOR = new Creator<Records>() {
        @Override
        public Records createFromParcel(Parcel in) {
            return new Records(in);
        }

        @Override
        public Records[] newArray(int size) {
            return new Records[size];
        }
    };

    private final List<Record> records;
    private final List<Record> unmodifiableRecords;

    private final LongSparseArray<Record> recordsMap;

    protected Records(Parcel in) {
        int version = in.readInt();
        records = in.createTypedArrayList(Record.CREATOR);
        recordsMap = new LongSparseArray<>(records.size());
        for (Record record : records) {
            recordsMap.put(record.getId(), record);
        }
        unmodifiableRecords = Collections.unmodifiableList(records);
    }

    protected Records(int capacity) {
        records = new ArrayList<>(capacity);
        unmodifiableRecords = Collections.unmodifiableList(records);
        recordsMap = new LongSparseArray<>(capacity);
    }

    public Records() {
        records = new ArrayList<>();
        unmodifiableRecords = Collections.unmodifiableList(records);
        recordsMap = new LongSparseArray<>(records.size());
    }

    public List<Record> list() {
        return unmodifiableRecords;
    }

    public Record get(long id) {
        return recordsMap.get(id);
    }

    public void add(Record record) {
        if (recordsMap.indexOfKey(record.getId()) >= 0) {
            throw new IllegalArgumentException();
        }
        records.add(record);
        recordsMap.put(record.getId(), record);
    }

    public void add(int index, Record record) {
        if (recordsMap.indexOfKey(record.getId()) >= 0) {
            throw new IllegalArgumentException();
        }
        records.add(index, record);
        recordsMap.put(record.getId(), record);
    }

    public void remove(Record record) {
        records.remove(record);
        recordsMap.remove(record.getId());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(VERSION);
        out.writeTypedList(records);
    }

    @Override
    public JSONArray toJson() {
        return JsonUtils.toJsonArray(records);
    }

    public static Records fromJson(JSONArray array) {
        int n = array.length();
        Records records = new Records(n);
        for (int i = 0; i < n; i++) {
            Object item;
            try {
                item = array.get(i);
            } catch (JSONException e) {
                continue;
            }
            if (!(item instanceof JSONObject)) {
                continue;
            }
            Record record = Record.fromJson((JSONObject) item);
            if (record != null) {
                records.add(record);
            }
        }
        return records;
    }
}
