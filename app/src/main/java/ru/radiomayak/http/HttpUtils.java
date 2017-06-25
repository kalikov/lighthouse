package ru.radiomayak.http;

import android.support.annotation.Nullable;

import ru.radiomayak.http.message.ParserCursor;
import ru.radiomayak.http.message.TokenParser;
import ru.radiomayak.http.util.CharArrayBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.BitSet;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

public final class HttpUtils {
    private HttpUtils() {
    }

    public static HttpRequestParams parseQuery(String query, String charset) throws UnsupportedEncodingException {
        CharArrayBuffer buffer = new CharArrayBuffer(query.length());
        buffer.append(query);

        TokenParser tokenParser = TokenParser.INSTANCE;

        BitSet delimiterSet = new BitSet();
        delimiterSet.set('&');

        ParserCursor cursor = new ParserCursor(0, buffer.length());
        HttpRequestParams params = new HttpRequestParams();

        while (!cursor.atEnd()) {
            delimiterSet.set('=');
            String name = tokenParser.parseToken(buffer, cursor, delimiterSet);
            String value = null;
            if (!cursor.atEnd()) {
                char delimiter = buffer.charAt(cursor.getPos());
                cursor.updatePos(cursor.getPos() + 1);
                if (delimiter == '=') {
                    delimiterSet.clear('=');
                    value = tokenParser.parseValue(buffer, cursor, delimiterSet);
                    if (!cursor.atEnd()) {
                        cursor.updatePos(cursor.getPos() + 1);
                    }
                }
            }

            if (!name.isEmpty()) {
                params.add(URLDecoder.decode(name, charset), value == null ? null : URLDecoder.decode(value, charset));
            }
        }
        return params;
    }

    @Nullable
    public static String getCharset(HttpMessage message) {
        Header contentType = message.getFirstHeader(HttpHeaders.CONTENT_TYPE);
        if (contentType != null) {
            HeaderElement values[] = contentType.getElements();
            if (values != null && values.length > 0) {
                NameValuePair param = values[0].getParameterByName("charset");
                if (param != null) {
                    return param.getValue();
                }
            }
        }
        return null;
    }

    @Nullable
    public static String getFirstHeader(HttpMessage message, String name) {
        Header header = message.getFirstHeader(name);
        if (header != null) {
            return header.getValue();
        }
        return null;
    }

    public static InputStream getContent(HttpEntity entity) throws IOException {
        Header encodingHeader = entity.getContentEncoding();
        String encoding = encodingHeader == null ? null : encodingHeader.getValue();
        if ("gzip".equalsIgnoreCase(encoding)) {
            return new GZIPInputStream(entity.getContent());
        }
        if ("deflate".equalsIgnoreCase(encoding)) {
            return new InflaterInputStream(entity.getContent());
        }
        return entity.getContent();
    }
}
