package ru.radiomayak;

import com.google.gson.JsonElement;

public interface Jsonable {
    JsonElement toJson();
}
