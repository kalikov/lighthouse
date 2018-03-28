package ru.radiomayak.podcasts;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.LongSparseArray;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Podcasts implements Parcelable {
    public static final Creator<Podcasts> CREATOR = new Creator<Podcasts>() {
        @Override
        public Podcasts createFromParcel(Parcel in) {
            return new Podcasts(in);
        }

        @Override
        public Podcasts[] newArray(int size) {
            return new Podcasts[size];
        }
    };

    private final List<Podcast> podcasts;
    private final List<Podcast> unmodifiablePodcasts;

    private final LongSparseArray<Podcast> podcastsMap;

    protected Podcasts(Parcel in) {
        podcasts = in.createTypedArrayList(Podcast.CREATOR);
        podcastsMap = new LongSparseArray<>(podcasts.size());
        for (Podcast podcast : podcasts) {
            podcastsMap.put(podcast.getId(), podcast);
        }
        unmodifiablePodcasts = Collections.unmodifiableList(podcasts);
    }

    protected Podcasts(int capacity) {
        podcasts = new ArrayList<>(capacity);
        unmodifiablePodcasts = Collections.unmodifiableList(podcasts);
        podcastsMap = new LongSparseArray<>(capacity);
    }

    public Podcasts(Collection<Podcast> collection) {
        int capacity = collection.size();
        podcasts = new ArrayList<>(capacity);
        unmodifiablePodcasts = Collections.unmodifiableList(podcasts);
        podcastsMap = new LongSparseArray<>(capacity);
        for (Podcast podcast : collection) {
            if (podcastsMap.get(podcast.getId()) == null) {
                podcasts.add(podcast);
                podcastsMap.put(podcast.getId(), podcast);
            }
        }
    }

    public Podcasts() {
        podcasts = new ArrayList<>();
        unmodifiablePodcasts = Collections.unmodifiableList(podcasts);
        podcastsMap = new LongSparseArray<>();
    }

    public List<Podcast> list() {
        return unmodifiablePodcasts;
    }

    public Podcast get(long id) {
        return podcastsMap.get(id);
    }

    public void add(Podcast podcast) {
        if (podcastsMap.indexOfKey(podcast.getId()) >= 0) {
            throw new IllegalArgumentException();
        }
        podcasts.add(podcast);
        podcastsMap.put(podcast.getId(), podcast);
    }

    public void add(int index, Podcast podcast) {
        if (podcastsMap.indexOfKey(podcast.getId()) >= 0) {
            throw new IllegalArgumentException();
        }
        podcasts.add(index, podcast);
        podcastsMap.put(podcast.getId(), podcast);
    }

    public void remove(Podcast podcast) {
        podcasts.remove(podcast);
        podcastsMap.remove(podcast.getId());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeTypedList(podcasts);
    }
}
