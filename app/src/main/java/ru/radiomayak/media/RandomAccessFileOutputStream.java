package ru.radiomayak.media;

import android.support.annotation.NonNull;

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

class RandomAccessFileOutputStream extends OutputStream {
    private final RandomAccessFile file;

    RandomAccessFileOutputStream(RandomAccessFile file) {
        this.file = file;
    }

    @Override
    public void write(int b) throws IOException {
        file.write(b);
    }

    @Override
    public void write(@NonNull byte[] buffer, int offset, int length) throws IOException {
        file.write(buffer, offset, length);
    }

    @Override
    public void close() throws IOException {
        file.close();
    }
}
