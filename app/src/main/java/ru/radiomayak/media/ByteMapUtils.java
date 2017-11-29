package ru.radiomayak.media;

import android.support.annotation.Nullable;
import android.util.Log;

import org.apache.commons.io.FileUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

import ru.radiomayak.io.RandomAccessFileOutputStream;

public final class ByteMapUtils {
    private static final String TAG = ByteMapUtils.class.getSimpleName();

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

    static ByteMap updateFile(File file, int capacity, int from, int to, ByteArrayOutputStream response) {
        File fileCopy = null;
        File parentDir = file.getParentFile();
        if (!parentDir.isDirectory() && !parentDir.mkdirs()) {
            Log.w(TAG, "Failed to create directory \"" + parentDir + "\"");
        }
        try (RandomAccessFile target = new RandomAccessFile(file, "rw")) {
            ByteMap byteMap = ByteMapUtils.readHeader(target);
            if (byteMap == null) {
                byteMap = new ByteMap(capacity, from, to);
                target.seek(0);
                ByteMapUtils.writeHeader(target, byteMap);
                response.writeTo(new RandomAccessFileOutputStream(target));
                target.getChannel().truncate(target.getFilePointer());
                return byteMap;
            }
            int overlap = byteMap.merge(from, to);
            if (overlap < 0) {
                return null;
            }
            if (capacity > 0) {
                byteMap.capacity(capacity);
            }
            int offset = byteMap.toOffset(from);

            fileCopy = new File(file.getAbsolutePath() + ".tmp");
            FileUtils.copyFile(file, fileCopy);

            long sourceBytesOrigin = target.getFilePointer();

            try (RandomAccessFile copy = new RandomAccessFile(fileCopy, "r")) {
                target.seek(0);
                ByteMapUtils.writeHeader(target, byteMap);
                copy.getChannel().transferTo(sourceBytesOrigin, offset, target.getChannel());
                response.writeTo(new RandomAccessFileOutputStream(target));
                copy.seek(sourceBytesOrigin + offset + overlap);
                copy.getChannel().transferTo(copy.getFilePointer(), copy.length() - copy.getFilePointer(), target.getChannel());
                target.getChannel().truncate(target.getFilePointer());
            }
            return byteMap;
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            if (fileCopy != null) {
                FileUtils.deleteQuietly(fileCopy);
            }
        }
        return null;
    }
}
