package ru.radiomayak.media;

import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.ServerSocket;

import ru.radiomayak.StringUtils;

public class MediaProxyServer implements StorageProvider {
    private static final String TAG = MediaProxyServer.class.getSimpleName();

    private ServerSocket socket;
    private Thread thread;

    private MediaProxyServerRunnable runnable;

    private final File storage;

    public MediaProxyServer(File storage) {
        this.storage = storage;
        if (!storage.mkdirs()) {
            Log.e(TAG, "Storage directories not created");
        }
    }

    public File getStorage() {
        return storage;
    }

    public void start() throws IOException {
        socket = new ServerSocket(0, 2, null);

        runnable = new MediaProxyServerRunnable(this, socket);

        thread = new Thread(runnable);
        thread.setName("MediaProxy Main Thread");
        thread.start();
    }

    public void stop() throws InterruptedException {
        if (socket != null) {
            IOUtils.closeQuietly(socket);
            socket = null;
        }
        if (thread != null) {
            thread.interrupt();
            try {
                thread.join();
            } finally {
                thread = null;
            }
        }
    }

    public InetAddress getInetAddress() {
        return socket.getInetAddress();
    }

    public int getPort() {
        return socket.getLocalPort();
    }

    public int getCacheSize(String name) {
        MediaProxyClientSession session = runnable.getSession();
        if (session != null && StringUtils.equals(session.name, name) && session.bytesMap != null) {
            return session.getSize();
        }
        File file = new File(storage, name + ".bin");
        if (!file.exists()) {
            return 0;
        }
        try (RandomAccessFile source = new RandomAccessFile(file, "r")) {
            BytesMap bytesMap = BytesMapUtils.readHeader(source);
            if (bytesMap == null) {
                return 0;
            }
            return bytesMap.capacity() > 0 ? (int)(bytesMap.size() * 100L / bytesMap.capacity()) : 0;
        } catch (IOException e) {
        }
        return 0;
    }
}
