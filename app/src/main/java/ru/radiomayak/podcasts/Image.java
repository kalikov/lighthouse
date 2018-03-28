package ru.radiomayak.podcasts;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import java.net.URI;

import ru.radiomayak.StringUtils;

public class Image implements Parcelable {
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

    public boolean hasColor() {
        return primaryColor != 0 || secondaryColor != 0;
    }

    public void setColors(int primaryColor, int secondaryColor) {
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
    }
}
