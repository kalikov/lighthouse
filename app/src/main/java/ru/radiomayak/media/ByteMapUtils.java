package ru.radiomayak.media;

import android.support.annotation.Nullable;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

public final class ByteMapUtils {
    private static final byte[] PREFIX = "PBM".getBytes();

    private ByteMapUtils() {
    }

    @Nullable
    public static ByteMap readHeader(RandomAccessFile file) throws IOException {
        byte[] prefix = new byte[PREFIX.length];
        int read = file.read(prefix);
        if (read < prefix.length || !Arrays.equals(prefix, PREFIX)) {
            return null;
        }
        int capacity = file.readInt();
        int segmentCount = file.readInt();
        int[] segments = new int[segmentCount];
        for (int i = 0; i < segmentCount; i++) {
            segments[i] = file.readInt();
        }
        return new ByteMap(capacity, segments);
    }

    static void writeHeader(RandomAccessFile file, ByteMap byteMap) throws IOException {
        file.write(PREFIX);
        file.writeInt(byteMap.capacity());
        int[] segments = byteMap.segments();
        file.writeInt(segments.length);
        for (int offset : segments) {
            file.writeInt(offset);
        }
    }
}
