package ru.radiomayak;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LighthouseTracks {
    private final List<LighthouseTrack> tracks;
    private final List<LighthouseTrack> unmodifiableTracks;

    private final Map<TrackId, LighthouseTrack> tracksMap;

    public LighthouseTracks(Collection<LighthouseTrack> collection) {
        this(collection.size());
        for (LighthouseTrack track : collection) {
            if (tracksMap.get(track.getId()) == null) {
                tracks.add(track);
                tracksMap.put(track.getId(), track);
            }
        }
    }

    public LighthouseTracks() {
        tracks = new ArrayList<>();
        unmodifiableTracks = Collections.unmodifiableList(tracks);
        tracksMap = new HashMap<>();
    }

    public LighthouseTracks(int capacity) {
        tracks = new ArrayList<>(capacity);
        unmodifiableTracks = Collections.unmodifiableList(tracks);
        tracksMap = new HashMap<>(capacity);
    }

    public boolean isEmpty() {
        return tracks.isEmpty();
    }

    public List<LighthouseTrack> list() {
        return unmodifiableTracks;
    }

    public void add(LighthouseTrack track) {
        if (tracksMap.containsKey(track.getId())) {
            throw new IllegalArgumentException();
        }
        tracks.add(track);
        tracksMap.put(track.getId(), track);
    }

    public void add(int index, LighthouseTrack track) {
        if (tracksMap.containsKey(track.getId())) {
            throw new IllegalArgumentException();
        }
        tracks.add(index, track);
        tracksMap.put(track.getId(), track);
    }

    public void remove(LighthouseTrack track) {
        tracks.remove(track);
        tracksMap.remove(track.getId());
    }

    public LighthouseTrack get(TrackId id) {
        return tracksMap.get(id);
    }
}
