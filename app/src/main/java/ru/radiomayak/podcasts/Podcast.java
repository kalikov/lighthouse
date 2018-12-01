package ru.radiomayak.podcasts;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import ru.radiomayak.StringUtils;

public class Podcast implements Parcelable, Identifiable {
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
    private String name;
    private String description;
    private int length;
    private int seen;
    private Image icon;
    private Image splash;
    private int favorite;
    private boolean archived;

    protected Podcast(Parcel in) {
        id = in.readLong();
        name = in.readString();
        if (in.readInt() > 0) {
            description = in.readString();
        }
        length = in.readInt();
        seen = in.readInt();
        archived = in.readByte() == 1;
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
        out.writeLong(id);
        out.writeString(name);
        writeStringToParcel(out, description);
        out.writeInt(length);
        out.writeInt(seen);
        out.writeByte((byte) (archived ? 1 : 0));
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

    @Override
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

    public int getSeen() {
        return seen;
    }

    public void setSeen(int seen) {
        if (seen < 0) {
            throw new IllegalArgumentException();
        }
        this.seen = seen;
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

    public int getFavorite() {
        return favorite;
    }

    public void setFavorite(int favorite) {
        this.favorite = favorite;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public boolean update(Podcast podcast) {
        boolean updated = false;
        if (!StringUtils.equals(podcast.getName(), name)) {
            updated = true;
            name = podcast.getName();
        }
        if (podcast.getLength() > length) {
            updated = true;
            length = podcast.getLength();
        }
        if (podcast.getSeen() > seen) {
            updated = true;
            seen = podcast.getSeen();
        }
        if (podcast.getDescription() != null) {
            updated = updated || !StringUtils.equals(podcast.getDescription(), description);
            description = StringUtils.nonEmpty(podcast.getDescription());
        }
        if (podcast.getFavorite() != favorite) {
            updated = true;
            favorite = podcast.getFavorite();
        }
        if (podcast.isArchived() != archived) {
            updated = true;
            archived = podcast.archived;
        }
        updated = updateIcon(podcast.getIcon()) || updated;
        updated = updateSplash(podcast.getSplash()) || updated;
        return updated;
    }

    private boolean updateIcon(@Nullable Image source) {
        if (source != null && (icon == null || !icon.getUrl().equalsIgnoreCase(source.getUrl()))) {
            icon = source;
            return true;
        }
        if (source != null && icon != null && source.hasColor() && (icon.getPrimaryColor() != source.getPrimaryColor() || icon.getSecondaryColor() != source.getSecondaryColor())) {
            icon.setColors(source.getPrimaryColor(), source.getSecondaryColor());
            return true;
        }
        return false;
    }

    private boolean updateSplash(@Nullable Image source) {
        if (source != null && (splash == null || !splash.getUrl().equalsIgnoreCase(source.getUrl()))) {
            splash = source;
            return true;
        }
        if (source != null && splash != null && source.hasColor()) {
            splash.setColors(source.getPrimaryColor(), source.getSecondaryColor());
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "Podcast{" +
                "id: " + id +
                ", name: '" + name + '\'' +
                ", description: '" + description + '\'' +
                ", length: " + length +
                ", seen: " + seen +
                ", icon: " + icon +
                ", splash: " + splash +
                ", favorite: " + favorite +
                ", archived: " + archived +
                '}';
    }
}