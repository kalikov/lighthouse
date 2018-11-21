package ru.radiomayak;

public final class TrackId {
    private static final long LONG_HI_BITS = 0xFFFFFFFF00000000L;
    private static final long LONG_LO_BITS = 0xFFFFFFFFL;

    private final long podcast;
    private final long record;

    public TrackId(long podcast, long record) {
        this.podcast = podcast;
        this.record = record;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        TrackId trackId = (TrackId) object;
        return podcast == trackId.podcast && record == trackId.record;
    }

    @Override
    public int hashCode() {
        return (int) (podcast ^ (podcast >>> 32)) ^ (int) (record ^ (record >>> 32));
    }

    public long asLong() {
        long hi = ((podcast & LONG_LO_BITS) << 32) | ((podcast & LONG_HI_BITS) >>> 32);
        return hi ^ record;
    }
}
