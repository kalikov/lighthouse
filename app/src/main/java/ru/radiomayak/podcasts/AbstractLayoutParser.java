package ru.radiomayak.podcasts;

import android.support.annotation.Nullable;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.radiomayak.StringUtils;

abstract class AbstractLayoutParser {
    private static final Pattern PODCAST_HREF_PATTERN = Pattern.compile("/podcasts/podcast/id/(\\d+)");
    private static final Pattern PODCAST_COUNT_PATTERN = Pattern.compile("(\\d+)\\s+.*$");

    static int lenientNext(XmlPullParser xpp) throws IOException, XmlPullParserException {
        try {
            return xpp.next();
        } catch (XmlPullParserException e) {
            return xpp.getEventType();
        }
    }

    static int lenientNextToken(XmlPullParser xpp) throws IOException, XmlPullParserException {
        try {
            return xpp.nextToken();
        } catch (XmlPullParserException e) {
            return xpp.getEventType();
        }
    }

    static void push(LayoutUtils.ElementStack stack, XmlPullParser xpp) {
        stack.push(xpp.getName(), getClass(xpp));
    }

    static void pop(LayoutUtils.ElementStack stack, XmlPullParser xpp) {
        stack.pop(xpp.getName());
    }

    static boolean hasClass(XmlPullParser xpp, String name) {
        String attr = getClass(xpp);
        return LayoutUtils.hasClass(attr, name);
    }

    static String getClass(XmlPullParser xpp) {
        return xpp.getAttributeValue(null, "class");
    }

    @Nullable
    static String appendText(@Nullable String string, XmlPullParser xpp) {
        return appendText(string, LayoutUtils.getText(xpp));
    }

    @Nullable
    static String appendText(@Nullable String string, @Nullable String text) {
        if (string == null) {
            return text;
        } else if (text != null) {
            return string + text;
        }
        return string;
    }

    static long parsePodcastIdentifier(String href) {
        if (href == null || href.isEmpty()) {
            return 0;
        }
        Matcher matcher = PODCAST_HREF_PATTERN.matcher(href);
        if (!matcher.find()) {
            return 0;
        }
        return StringUtils.parseLong(matcher.group(1), 0);
    }

    static int parsePodcastLength(String input) {
        String text = StringUtils.nonEmptyTrimmed(input);
        Matcher matcher = PODCAST_COUNT_PATTERN.matcher(text);
        int length;
        if (!matcher.find()) {
            length = 0;
        } else {
            length = StringUtils.parseInt(matcher.group(1), 0);
        }
        return length;
    }
}
