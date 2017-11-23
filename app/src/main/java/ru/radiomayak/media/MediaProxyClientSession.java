package ru.radiomayak.media;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;

import ru.radiomayak.CacheUtils;
import ru.radiomayak.StringUtils;
import ru.radiomayak.http.HttpException;
import ru.radiomayak.http.HttpRequest;
import ru.radiomayak.http.HttpRequestParams;
import ru.radiomayak.http.HttpUtils;
import ru.radiomayak.http.impl.io.DefaultHttpRequestParser;
import ru.radiomayak.http.io.HttpMessageParser;
import ru.radiomayak.http.io.SessionInputBuffer;
import ru.radiomayak.http.io.SessionOutputBuffer;
import ru.radiomayak.http.protocol.HTTP;

class MediaProxyClientSession {
    final SessionInputBuffer inputBuffer;
    final SessionOutputBuffer outputBuffer;

    final HttpRequest request;

    final String category;
    final String id;
    final URL url;
    final File file;

    volatile ByteMap byteMap;
    volatile int from;
    volatile int length;

    MediaProxyClientSession(SessionInputBuffer inputBuffer, SessionOutputBuffer outputBuffer, File dir) throws IOException, HttpException {
        this.inputBuffer = inputBuffer;
        this.outputBuffer = outputBuffer;

        request = parseRequest(inputBuffer);
        URI uri = URI.create(request.getRequestLine().getUri());

        String query = StringUtils.requireNonEmpty(uri.getRawQuery());
        String charset = HttpUtils.getCharset(request);
        HttpRequestParams params = HttpUtils.parseQuery(query, StringUtils.nonEmpty(charset, HTTP.DEF_CONTENT_CHARSET.name()));

        category = StringUtils.requireNonEmpty(params.getFirst(DefaultMediaProxyServer.CATEGORY_PARAMETER));
        id = StringUtils.requireNonEmpty(params.getFirst(DefaultMediaProxyServer.ID_PARAMETER));
        String urlString = StringUtils.requireNonEmpty(params.getFirst(DefaultMediaProxyServer.URL_PARAMETER));

        this.url = new URL(urlString);

        file = CacheUtils.getFile(dir, 0, id);
    }

    private static HttpRequest parseRequest(SessionInputBuffer buffer) throws IOException, HttpException {
        HttpMessageParser<HttpRequest> parser = new DefaultHttpRequestParser(buffer);
        return parser.parse();
    }

//    public int getSize() {
//        if (byteMap.capacity() <= 0) {
//            return 0;
//        }
//        if (length <= 0) {
//            return (int)(byteMap.size() * 100L / byteMap.capacity());
//        }
//        return (int)(byteMap.size(from, from + length - 1) * 100L / byteMap.capacity());
//    }
}
