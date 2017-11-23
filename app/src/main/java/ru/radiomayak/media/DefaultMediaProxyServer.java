package ru.radiomayak.media;

import android.net.Uri;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.URLEncoder;

import ru.radiomayak.http.protocol.HTTP;

public class DefaultMediaProxyServer implements MediaProxyServer {
    static final String ID_PARAMETER = "id";
    static final String CATEGORY_PARAMETER = "category";
    static final String URL_PARAMETER = "redirect";

    private ServerSocket socket;
    private Thread thread;

    private MediaProxyServerRunnable runnable;

    private final MediaProxyContext context;

    public DefaultMediaProxyServer(MediaProxyContext context) {
        this.context = context;
    }

    public void start() throws IOException {
        socket = new ServerSocket(0, 4, null);

        runnable = new MediaProxyServerRunnable(context, socket);

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

    @Override
    public boolean isStarted() {
        return socket != null && socket.isBound();
    }

    @Override
    public Uri formatUri(long category, long id, String targetUrl) {
        if (socket == null || !socket.isBound()) {
            throw new IllegalStateException();
        }
        StringBuilder builder = new StringBuilder(255);
        builder.append("http://");
        builder.append(socket.getInetAddress().getHostAddress());
        builder.append(':');
        builder.append(socket.getLocalPort()).append('/');
        builder.append('?');
        builder.append(CATEGORY_PARAMETER).append('=').append(category);
        builder.append('&');
        builder.append(ID_PARAMETER).append('=').append(id);
        builder.append('&');
        builder.append(URL_PARAMETER).append('=');
        try {
            builder.append(URLEncoder.encode(targetUrl, HTTP.DEF_CONTENT_CHARSET.name()));
        } catch (UnsupportedEncodingException e) {
            throw new UnsupportedOperationException(e);
        }
        return Uri.parse(builder.toString());
    }

//    public int getCacheSize(String name) {
//        MediaProxyClientSession session = runnable.getSession();
//        if (session != null && StringUtils.equals(session.id, name) && session.byteMap != null) {
//            return session.getSize();
//        }
//        File file = new File(storage, name + ".bin");
//        if (!file.exists()) {
//            return 0;
//        }
//        try (RandomAccessFile source = new RandomAccessFile(file, "r")) {
//            ByteMap byteMap = ByteMapUtils.readHeader(source);
//            if (byteMap == null) {
//                return 0;
//            }
//            return byteMap.capacity() > 0 ? (int) (byteMap.size() * 100L / byteMap.capacity()) : 0;
//        } catch (IOException e) {
//        }
//        return 0;
//    }
}
