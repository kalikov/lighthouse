package ru.radiomayak.podcasts;

import android.support.annotation.Nullable;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Stack;
import java.util.Vector;
import java.util.regex.Pattern;

import ru.radiomayak.StringUtils;

final class LayoutUtils {
    private static final Pattern BR_PATTERN = Pattern.compile("\\s*(<br>)+\\s*");

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

            StringBuilder cleanedBuilder = null;

            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.TEXT || eventType == XmlPullParser.ENTITY_REF || eventType == XmlPullParser.IGNORABLE_WHITESPACE) {
                    String text = getText(xpp);
                    if (cleanedBuilder == null) {
                        cleanedBuilder = new StringBuilder(text);
                    } else if (text != null) {
                        cleanedBuilder.append(text);
                    }
                } else if (eventType == XmlPullParser.START_TAG) {
                    if (cleanedBuilder != null && "br".equalsIgnoreCase(xpp.getName())) {
                        cleanedBuilder.append("<br>");
                    }
                }
                try {
                    eventType = xpp.nextToken();
                } catch (XmlPullParserException e) {
                    eventType = xpp.getEventType();
                }
            }
            return cleanedBuilder == null ? null : LayoutUtils.br2nl(StringUtils.normalize(cleanedBuilder.toString()));
        } catch (IOException | XmlPullParserException e) {
            return string;
        }
    }

    @Nullable
    static String br2nl(@Nullable String string) {
        if (string == null || string.isEmpty()) {
            return string;
        }
        return BR_PATTERN.matcher(string).replaceAll("\n");
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
        if (xpp.getName() == null) {
            return "&";
        }
        switch (xpp.getName()) {
            case "nbsp":
                return "\u00A0";
            default:
                return null;
        }
    }

    static boolean isHeader(String tag) {
        return "h1".equalsIgnoreCase(tag) || "h2".equalsIgnoreCase(tag) || "h3".equalsIgnoreCase(tag) || "h4".equalsIgnoreCase(tag) || "header".equalsIgnoreCase(tag);
    }

    static boolean isSection(String tag) {
        return "section".equalsIgnoreCase(tag);
    }

    static boolean isDiv(String tag) {
        return "div".equalsIgnoreCase(tag);
    }

    static boolean isSpan(String tag) {
        return "span".equalsIgnoreCase(tag);
    }

    static boolean isUl(String tag) {
        return "ul".equalsIgnoreCase(tag);
    }

    static boolean isLi(String tag) {
        return "li".equalsIgnoreCase(tag);
    }

    static boolean isBlock(String tag) {
        return isDiv(tag) || isSpan(tag) || isUl(tag) || isLi(tag) || isHeader(tag) || isSection(tag);
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

    static class ElementStack {
        private final Vector<StackElement> elements;

        ElementStack(int numElements) {
            elements = new Vector<>(numElements);
        }

        void push(String tag, String classAttribute) {
            elements.addElement(new StackElement(tag, classAttribute));
        }

        void pop(String tag) {
            StackElement element;
            do {
                element = pop();
            } while (!element.tag.equalsIgnoreCase(tag) && !elements.isEmpty());
        }

        StackElement peek() {
            int len = elements.size();
            return elements.elementAt(len - 1);
        }

        StackElement pop() {
            int len = elements.size();
            StackElement element = elements.elementAt(len - 1);
            elements.removeElementAt(len - 1);
            return element;
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
