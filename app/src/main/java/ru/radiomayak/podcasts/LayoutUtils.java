package ru.radiomayak.podcasts;

import android.support.annotation.Nullable;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.radiomayak.StringUtils;

final class LayoutUtils {
    private static final Pattern CHARSET_PATTERN = Pattern.compile("(?i)\\bcharset=\\s*(?:\"|')?([^\\s,;\"']*)");

    private LayoutUtils() {
    }

    @Nullable
    static String clean(@Nullable String string) {
        if (string == null) {
            return null;
        }
        try (InputStream stream = new ByteArrayInputStream(string.getBytes())) {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setValidating(false);

            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(stream, Charset.defaultCharset().name());

            String cleaned = null;

            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.TEXT || eventType == XmlPullParser.ENTITY_REF || eventType == XmlPullParser.IGNORABLE_WHITESPACE) {
                    String text = getText(xpp);
                    if (cleaned == null) {
                        cleaned = text;
                    } else if (text != null) {
                        cleaned += text;
                    }
                }
                try {
                    eventType = xpp.nextToken();
                } catch (XmlPullParserException e) {
                    eventType = xpp.getEventType();
                }
            }
            return StringUtils.normalize(cleaned);
        } catch (IOException | XmlPullParserException e) {
            return string;
        }
    }

    static String getText(XmlPullParser xpp) {
        try {
            if (xpp.getEventType() != XmlPullParser.ENTITY_REF) {
                return xpp.getText();
            }
        } catch (XmlPullParserException e) {
            return xpp.getText();
        }
        String text = xpp.getText();
        if (text != null) {
            return text;
        }
        switch (xpp.getName()) {
            case "nbsp":
                return "\u00A0";
            default:
                return null;
        }
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

    static boolean isDiv(String tag) {
        return "div".equalsIgnoreCase(tag);
    }

    static boolean isSpan(String tag) {
        return "span".equalsIgnoreCase(tag);
    }

    static boolean isBlock(String tag) {
        return isDiv(tag) || isSpan(tag);
    }

    static boolean isAnchor(String tag) {
        return "a".equalsIgnoreCase(tag);
    }

    static boolean isImage(String tag) {
        return "img".equalsIgnoreCase(tag);
    }

    static boolean isInput(String tag) {
        return "input".equalsIgnoreCase(tag);
    }

    static boolean hasClass(String attribute, String className) {
        if (attribute == null || attribute.isEmpty()) {
            return false;
        }
        int nameLength = className.length();
        int length = attribute.length();
        int offset = 0;
        while (offset + nameLength <= length) {
            int index = attribute.indexOf(className, offset);
            if (index < 0) {
                return false;
            }
            boolean isStart = index <= 0 || Character.isWhitespace(attribute.charAt(index - 1));
            boolean isEnd = index + nameLength >= length || Character.isWhitespace(attribute.charAt(index + nameLength));
            if (isStart && isEnd) {
                return true;
            }
            offset = index + 1;
        }
        return false;
    }

    static class Stack {
        private final Deque<StackElement> elements;

        Stack(int numElements) {
            elements = new ArrayDeque<>(numElements);
        }

        void push(String tag, String classAttribute) {
            elements.push(new StackElement(tag, classAttribute));
        }

        void pop(String tag) {
            StackElement element;
            do {
                element = elements.pop();
            } while (!element.tag.equalsIgnoreCase(tag) && !elements.isEmpty());
        }

        StackElement pop() {
            return elements.pop();
        }

        boolean isEmpty() {
            return elements.isEmpty();
        }
    }

    static class StackElement {
        private final String tag;
        private final String classAttribute;

        private StackElement(String tag, String classAttribute) {
            this.tag = tag;
            this.classAttribute = classAttribute;
        }

        String getTag() {
            return tag;
        }

        boolean hasClass(String className) {
            return LayoutUtils.hasClass(classAttribute, className);
        }

        String getClassAttribute() {
            return classAttribute;
        }
    }
}
