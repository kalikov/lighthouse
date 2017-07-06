package ru.radiomayak.podcasts;

import android.support.annotation.Nullable;

import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.radiomayak.StringUtils;

final class LayoutUtils {
    private static final Pattern CHARSET_PATTERN = Pattern.compile("(?i)\\bcharset=\\s*(?:\"|')?([^\\s,;\"']*)");

    private LayoutUtils() {
    }

    static Document parse(ByteBuffer buffer, String charsetName, String baseUri) {
        // look for BOM - overrides any other header or input
        charsetName = detectCharsetFromBom(buffer, charsetName);
        if (charsetName == null) {
            charsetName = detectCharsetFromMeta(buffer);
            buffer.rewind();
        }
        StringUtils.requireNonEmpty(charsetName);
        String string = Charset.forName(charsetName).decode(buffer).toString();
        Parser parser = Parser.htmlParser();
        Document doc = parser.parseInput(string, baseUri);
        doc.outputSettings().charset(charsetName);
        return doc;
    }

    @Nullable
    private static String detectCharsetFromMeta(ByteBuffer buffer) {
        // look for <meta http-equiv="Content-Type" content="text/html;charset=gb2312"> or HTML5 <meta charset="gb2312">
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);

            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(new ByteArrayInputStream(buffer.array()), null);

            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    String tag = xpp.getName();
                    if ("body".equalsIgnoreCase(tag)) {
                        break;
                    }
                    if ("meta".equalsIgnoreCase(tag)) {
                        String charset = null;
                        if ("content-type".equalsIgnoreCase(xpp.getAttributeValue(null, "http-equiv"))) {
                            charset = getCharsetFromContentType(xpp.getAttributeValue(null, "content"));
                        }
                        if (charset == null) {
                            charset = xpp.getAttributeValue(null, "charset");
                        }
                        if (charset != null) {
                            return validateCharset(charset);
                        }
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    String tag = xpp.getName();
                    if ("head".equalsIgnoreCase(tag)) {
                        break;
                    }
                }
                eventType = xpp.next();
            }
            return xpp.getInputEncoding();
        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Nullable
    private static String getCharsetFromContentType(String contentType) {
        if (contentType == null) return null;
        Matcher m = CHARSET_PATTERN.matcher(contentType);
        if (m.find()) {
            String charset = m.group(1).trim();
            charset = charset.replace("charset=", "");
            return charset;
        }
        return null;
    }

    @Nullable
    private static String validateCharset(String cs) {
        if (cs == null || cs.length() == 0) return null;
        cs = cs.trim().replaceAll("[\"']", "");
        try {
            if (Charset.isSupported(cs)) return cs;
            cs = cs.toUpperCase(Locale.ENGLISH);
            if (Charset.isSupported(cs)) return cs;
        } catch (IllegalCharsetNameException ignored) {
        }
        return null;
    }

    @Nullable
    private static String detectCharsetFromBom(ByteBuffer buffer, @Nullable String charsetName) {
        buffer.mark();
        byte[] bom = new byte[4];
        if (buffer.remaining() >= bom.length) {
            buffer.get(bom);
            buffer.rewind();
        }
        if (bom[0] == 0x00 && bom[1] == 0x00 && bom[2] == (byte) 0xFE && bom[3] == (byte) 0xFF || // BE
                bom[0] == (byte) 0xFF && bom[1] == (byte) 0xFE && bom[2] == 0x00 && bom[3] == 0x00) { // LE
            charsetName = "UTF-32"; // and I hope it's on your system
        } else if (bom[0] == (byte) 0xFE && bom[1] == (byte) 0xFF || // BE
                bom[0] == (byte) 0xFF && bom[1] == (byte) 0xFE) {
            charsetName = "UTF-16"; // in all Javas
        } else if (bom[0] == (byte) 0xEF && bom[1] == (byte) 0xBB && bom[2] == (byte) 0xBF) {
            charsetName = "UTF-8"; // in all Javas
            buffer.position(3); // 16 and 32 decoders consume the BOM to determine be/le; utf-8 should be consumed here
        }
        return charsetName;
    }
}
