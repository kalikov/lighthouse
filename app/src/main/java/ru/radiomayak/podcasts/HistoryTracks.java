package ru.radiomayak.podcasts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.radiomayak.TrackId;

public class HistoryTracks {
    private final List<HistoryTrack> tracks;
    private final List<HistoryTrack> unmodifiableTracks;

    private final Map<TrackId, HistoryTrack> tracksMap;

    public HistoryTracks(Collection<HistoryTrack> collection) {
        this(collection.size());
        for (HistoryTrack track : collection) {
            if (tracksMap.get(track.getId()) == null) {
                tracks.add(track);
                tracksMap.put(track.getId(), track);
            }
        }
    }

    public HistoryTracks() {
        tracks = new ArrayList<>();
        unmodifiableTracks = Collections.unmodifiableList(tracks);
        tracksMap = new HashMap<>();
    }

    public HistoryTracks(int capacity) {
        tracks = new ArrayList<>(capacity);
        unmodifiableTracks = Collections.unmodifiableList(tracks);
        tracksMap = new HashMap<>(capacity);
    }

    public boolean isEmpty() {
        return tracks.isEmpty();
    }

    public List<HistoryTrack> list() {
        return unmodifiableTracks;
    }

    public void add(HistoryTrack track) {
        if (tracksMap.containsKey(track.getId())) {
            throw new IllegalArgumentException();
        }
        tracks.add(track);
        tracksMap.put(track.getId(), track);
    }

    public void add(int index, HistoryTrack track) {
        if (tracksMap.containsKey(track.getId())) {
            throw new IllegalArgumentException();
        }
        tracks.add(index, track);
        tracksMap.put(track.getId(), track);
    }

    public void remove(HistoryTrack track) {
        tracks.remove(track);
        tracksMap.remove(track.getId());
    }

    public void removeAll(Collection<HistoryTrack> collection) {
        tracks.removeAll(collection);
        for (HistoryTrack item : collection) {
            tracksMap.remove(item.getId());
        }
    }

    public HistoryTrack get(TrackId id) {
        return tracksMap.get(id);
    }
}
