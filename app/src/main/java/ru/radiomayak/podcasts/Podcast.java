package ru.radiomayak.podcasts;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import ru.radiomayak.JsonUtils;
import ru.radiomayak.Jsonable;
import ru.radiomayak.StringUtils;

public class Podcast implements Parcelable, Jsonable {
    private static final int VERSION = 1;

    private static final byte IMAGE_NONE = 0;
    private static final byte IMAGE_DEFAULT = 1;

    public static final Creator<Podcast> CREATOR = new Creator<Podcast>() {
        @Override
        public Podcast createFromParcel(Parcel in) {
            return new Podcast(in);
        }

        @Override
        public Podcast[] newArray(int size) {
            return new Podcast[size];
        }
    };

    private final long id;
    private final String name;
    private String description;
    private int length;
    private Image icon;
    private Image splash;

    protected Podcast(Parcel in) {
        int version = in.readInt();
        id = in.readLong();
        name = in.readString();
        if (in.readInt() > 0) {
            description = in.readString();
        }
        length = in.readInt();
        icon = readImageFromParcel(in);
        splash = readImageFromParcel(in);
    }

    public Podcast(long id, String name) {
        this.id = id;
        this.name = StringUtils.requireNonEmpty(name);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(VERSION);
        out.writeLong(id);
        out.writeString(name);
        writeStringToParcel(out, description);
        out.writeInt(length);
        writeImageToParcel(out, icon, flags);
        writeImageToParcel(out, splash, flags);
    }

    private static void writeStringToParcel(Parcel out, @Nullable String string) {
        if (string == null || string.isEmpty()) {
            out.writeInt(0);
        } else {
            out.writeInt(string.length());
            out.writeString(string);
        }
    }

    private static void writeImageToParcel(Parcel out, @Nullable Image image, int flags) {
        if (image == null) {
            out.writeByte(IMAGE_NONE);
        } else {
            out.writeByte(IMAGE_DEFAULT);
            image.writeToParcel(out, flags);
        }
    }

    @Nullable
    private static Image readImageFromParcel(Parcel in) {
        if (in.readByte() == IMAGE_DEFAULT) {
            return Image.CREATOR.createFromParcel(in);
        }
        return null;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = StringUtils.nonEmpty(description);
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        if (length < 0) {
            throw new IllegalArgumentException();
        }
        this.length = length;
    }

    @Nullable
    public Image getIcon() {
        return icon;
    }

    public void setIcon(@Nullable Image image) {
        this.icon = image;
    }

    @Nullable
    public Image getSplash() {
        return splash;
    }

    public void setSplash(@Nullable Image splash) {
        this.splash = splash;
    }

    public boolean merge(Podcast podcast) {
        boolean updated = false;
        if (podcast.getLength() > 0) {
            updated = length != podcast.getLength();
            length = podcast.getLength();
        }
        if (podcast.getDescription() != null) {
            updated = updated || !StringUtils.equals(podcast.getDescription(), description);
            description = StringUtils.nonEmpty(podcast.getDescription());
        }
        updated = mergeIcon(podcast.getIcon()) || updated;
        updated = mergeSplash(podcast.getSplash()) || updated;
        return updated;
    }

    private boolean mergeIcon(@Nullable Image source) {
        if (source != null && (icon == null || !icon.getUrl().equalsIgnoreCase(source.getUrl()))) {
            icon = source;
            return true;
        }
        if (source != null && icon != null && source.getPrimaryColor() != 0) {
            icon.setColors(source.getPrimaryColor(), source.getSecondaryColor());
            return true;
        }
        return false;
    }

    private boolean mergeSplash(@Nullable Image source) {
        if (source != null && (splash == null || !splash.getUrl().equalsIgnoreCase(source.getUrl()))) {
            splash = source;
            return true;
        }
        if (source != null && splash != null && source.getPrimaryColor() != 0) {
            splash.setColors(source.getPrimaryColor(), source.getSecondaryColor());
            return true;
        }
        return false;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("name", name);
        if (description != null) {
            json.addProperty("description", description);
        }
        if (length > 0) {
            json.addProperty("length", length);
        }
        if (icon != null) {
            json.add("icon", icon.toJson());
        }
        if (splash != null) {
            json.add("splash", splash.toJson());
        }
        return json;
    }

    @Nullable
    public static Podcast fromJson(JsonObject json) {
        long id = JsonUtils.getOptLong(json, "id", 0);
        if (id <= 0) {
            return null;
        }
        String name = JsonUtils.getOptString(json, "name");
        if (name == null || name.isEmpty()) {
            return null;
        }
        Podcast podcast = new Podcast(id, name);
        podcast.setDescription(StringUtils.nonEmpty(JsonUtils.getOptString(json, "description")));
        int length = JsonUtils.getOptInt(json, "length", 0);
        if (length > 0) {
            podcast.setLength(length);
        }
        podcast.setIcon(getOptImage(json, "icon"));
        podcast.setSplash(getOptImage(json, "splash"));
        return podcast;
    }

    @Nullable
    private static Image getOptImage(JsonObject json, String property) {
        if (json.has(property)) {
            JsonElement element = json.get(property);
            if (element.isJsonObject()) {
                return Image.fromJson(element.getAsJsonObject());
            }
        }
        return null;
    }
}