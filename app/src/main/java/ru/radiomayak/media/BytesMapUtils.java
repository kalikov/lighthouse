package ru.radiomayak.media;

import android.support.annotation.Nullable;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

final class BytesMapUtils {
    private static final byte[] PREFIX = "PBM".getBytes();

    private BytesMapUtils() {
    }

    @Nullable
    static BytesMap readHeader(RandomAccessFile file) throws IOException {
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
        return new BytesMap(capacity, segments);
    }

    static void writeHeader(RandomAccessFile file, BytesMap bytesMap) throws IOException {
        file.write(PREFIX);
        file.writeInt(bytesMap.capacity());
        int[] segments = bytesMap.segments();
        file.writeInt(segments.length);
        for (int offset : segments) {
            file.writeInt(offset);
        }
    }
}
