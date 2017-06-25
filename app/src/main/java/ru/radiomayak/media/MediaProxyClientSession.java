package ru.radiomayak.media;

import ru.radiomayak.http.io.SessionInputBuffer;
import ru.radiomayak.http.io.SessionOutputBuffer;

import ru.radiomayak.http.HttpRange;

class MediaProxyClientSession {
    final SessionInputBuffer inputBuffer;
    final SessionOutputBuffer outputBuffer;
    volatile String name;
    volatile BytesMap bytesMap;
    volatile int from;
    volatile int length;


    MediaProxyClientSession(SessionInputBuffer inputBuffer, SessionOutputBuffer outputBuffer) {
        this.inputBuffer = inputBuffer;
        this.outputBuffer = outputBuffer;
    }

    public int getSize() {
        if (bytesMap.capacity() <= 0) {
            return 0;
        }
        if (length <= 0) {
            return (int)(bytesMap.size() * 100L / bytesMap.capacity());
        }
        return (int)(bytesMap.size(from, from + length - 1) * 100L / bytesMap.capacity());
    }
}
