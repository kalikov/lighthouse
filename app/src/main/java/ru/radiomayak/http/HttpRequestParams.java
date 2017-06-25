package ru.radiomayak.http;

import android.support.annotation.Nullable;

import ru.radiomayak.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HttpRequestParams {
    private final List<NameValuePair> list = new ArrayList<>();

    public void add(String name, String value) {
        list.add(new BasicNameValuePair(name, value));
    }

    @Nullable
    public String getFirst(String name) {
        for (NameValuePair item : list) {
            if (item.getName().equals(name)) {
                return item.getValue();
            }
        }
        return null;
    }

    public List<NameValuePair> getList() {
        return Collections.unmodifiableList(list);
    }
}
