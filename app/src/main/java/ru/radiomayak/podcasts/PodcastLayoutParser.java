package ru.radiomayak.podcasts;

import android.support.annotation.Nullable;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.radiomayak.NetworkUtils;
import ru.radiomayak.StringUtils;

class PodcastLayoutParser extends AbstractLayoutParser {
    private static final String PODCAST_ID = "podcast-id";
    private static final String PODCAST_ID_QUERY = "#podcast-id";
    private static final String PODCAST_ITEM_CLASS = "b-podcast__item";
    private static final String PODCAST_SPLASH_CLASS = "b-podcast__pic";
    private static final String PODCAST_LOGO_CLASS = "b-podcast__logo";
    private static final String PODCAST_LOGO_QUERY = "img.b-podcast__pic";
    private static final String PODCAST_NEXT_PAGE_CLASS = "b-podcast__records-show-more__btn";
    private static final String PODCAST_NEXT_PAGE_QUERY = "a.b-podcast__records-show-more__btn";

    private static final String RECORDS_LIST_CLASS = "b-podcast__records";
    private static final String RECORD_ITEM_CLASS = "b-podcast__records-item";
    private static final String RECORD_QUERY = ".b-podcast__records-item";
    private static final String RECORD_ANCHOR_CLASS = "b-podcast__records-listen";
    private static final String ANCHOR_QUERY = "a.b-podcast__records-listen";
    private static final String RECORD_NAME_CLASS = "b-podcast__records-name";
    private static final String NAME_QUERY = ".b-podcast__records-name";
    private static final String RECORD_DESCRIPTION_CLASS = "b-podcast__records-description__text";
    private static final String DESCRIPTION_QUERY = ".b-podcast__records-description__text";
    private static final String RECORD_DATE_CLASS = "b-podcast__records-date";
    private static final String DATE_QUERY = ".b-podcast__records-date";
    private static final String RECORD_DURATION_CLASS = "b-podcast__records-time";
    private static final String DURATION_QUERY = ".b-podcast__records-time";

    private static final Pattern RECORD_URL_ID_PATTERN = Pattern.compile(".+listen\\?id=(\\d+).*");

    private static final Pattern JSON_DATE_PATTERN = Pattern.compile("(\\d{2})\\-(\\d{2})\\-(\\d{4})");
    private static final Pattern HTML_DATE_PATTERN = Pattern.compile("\\:[\\u00A0\\s]*(\\d{2})\\.(\\d{2})\\.(\\d{4})");

    private static final Pattern JSON_DURATION_PATTERN = Pattern.compile("((\\d{2}\\:)?\\d{2}\\:\\d{2})");
    private static final Pattern HTML_DURATION_PATTERN = Pattern.compile("\\:[\\u00A0\\s]*((\\d{2}\\:)?\\d{2}\\:\\d{2})");

    private static final Pattern PODCAST_HREF_PATTERN = Pattern.compile("/podcasts/podcast/id/(\\d+)/");

    PodcastLayoutContent parse(InputStream input, @Nullable String charset, String baseUri) throws IOException {
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setValidating(false);

            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(input, charset);

            boolean isUnderLogo = false;
            long id = 0;
            String name = null;
            String splash = null;
            int length = 0;

            Records records = new Records();
            long nextPage = 0;

            URI uri = NetworkUtils.toOptURI(baseUri);

            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    String tag = xpp.getName();
                    if ("div".equalsIgnoreCase(tag) && hasClass(xpp, RECORDS_LIST_CLASS)) {
                        records = parseRecords(xpp, uri);
                    } else if ("input".equalsIgnoreCase(tag) && PODCAST_ID.equals(xpp.getAttributeValue(null, "id"))) {
                        id = StringUtils.parseLong(xpp.getAttributeValue(null, "value"), 0);
                    } else if ("div".equalsIgnoreCase(tag) && hasClass(xpp, PODCAST_LOGO_CLASS)) {
                        isUnderLogo = true;
                    } else if ("img".equalsIgnoreCase(tag) && hasClass(xpp, PODCAST_SPLASH_CLASS) && isUnderLogo) {
                        splash = xpp.getAttributeValue(null, "src");
                        name = StringUtils.nonEmpty(xpp.getAttributeValue(null, "title"));
                        isUnderLogo = false;
                    } else if ("a".equalsIgnoreCase(tag) && hasClass(xpp, PODCAST_NEXT_PAGE_CLASS)) {
                        nextPage = StringUtils.parseLong(xpp.getAttributeValue(null, "data-page"), 0);
                        if (id == 0) {
                            break;
                        }
                    } else if (LayoutUtils.isDiv(tag) && hasClass(xpp, PODCAST_ITEM_CLASS)) {
                        length = parseLength(xpp, id);
                        if (length > 0) {
                            break;
                        }
                    }
                }
                eventType = lenientNext(xpp);
            }

            Podcast podcast = null;
            if (id != 0 && name != null) {
                podcast = new Podcast(id, name);
                if (splash != null && !splash.isEmpty()) {
                    podcast.setSplash(new Image(splash, uri));
                }
                podcast.setLength(length);
            }

            return new PodcastLayoutContent(podcast, records, nextPage);
        } catch (XmlPullParserException e) {
            throw new IOException(e);
        }
    }

    private static int lenientNext(XmlPullParser xpp) throws IOException, XmlPullParserException {
        try {
            return xpp.nextToken();
        } catch (XmlPullParserException e) {
            return xpp.getEventType();
        }
    }

    private static Records parseRecords(XmlPullParser xpp, @Nullable URI uri) throws IOException, XmlPullParserException {
        Records records = new Records();

        LayoutUtils.Stack path = new LayoutUtils.Stack(3);
        push(path, xpp);

        int eventType = lenientNext(xpp);
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                String tag = xpp.getName();
                if ("div".equalsIgnoreCase(tag) && hasClass(xpp, RECORD_ITEM_CLASS)) {
                    Record record = parseRecord(xpp, uri);
                    if (record != null) {
                        records.add(record);
                    }
                } else {
                    push(path, xpp);
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                pop(path, xpp);
                if (path.isEmpty()) {
                    break;
                }
            }
            eventType = lenientNext(xpp);
        }
        return records;
    }

    @Nullable
    private static Record parseRecord(XmlPullParser xpp, @Nullable URI uri) throws IOException, XmlPullParserException {
        LayoutUtils.Stack path = new LayoutUtils.Stack(5);
        push(path, xpp);

        boolean failure = false;
        long id = 0;
        String anchorName = null;
        String name = null;
        String url = null;
        String description = null;
        String date = null;
        String duration = null;

        int eventType = lenientNext(xpp);
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                String tag = xpp.getName();
                push(path, xpp);
                if (!failure && "a".equalsIgnoreCase(tag) && hasClass(xpp, RECORD_ANCHOR_CLASS)) {
                    url = xpp.getAttributeValue(null, "data-url");
                    if (url == null || url.isEmpty()) {
                        failure = true;
                    } else {
                        id = StringUtils.parseLong(xpp.getAttributeValue(null, "data-id"), 0);
                        if (id == 0) {
                            id = parseIdentifier(url);
                        }
                        if (id == 0) {
                            failure = true;
                        } else {
                            anchorName = StringUtils.nonEmpty(xpp.getAttributeValue(null, "data-title"));
                        }
                    }
                }
            } else if (eventType == XmlPullParser.TEXT || eventType == XmlPullParser.ENTITY_REF) {
                if (!failure) {
                    if (path.is(null, RECORD_NAME_CLASS)) {
                        String text = StringUtils.nonEmptyTrimmed(xpp.getText());
                        if (name == null) {
                            name = text;
                        } else if (text != null) {
                            name += ' ' + text;
                        }
                    } else if (path.isUnder(null, RECORD_DESCRIPTION_CLASS)) {
                        String text = StringUtils.nonEmptyTrimmed(xpp.getText());
                        if (description == null) {
                            description = text;
                        } else if (text != null) {
                            description += ' ' + text;
                        }
                    } else if (path.is(null, RECORD_DATE_CLASS)) {
                        String text = StringUtils.nonEmptyTrimmed(xpp.getText());
                        if (date == null) {
                            date = text;
                        } else if (text != null) {
                            date += ' ' + text;
                        }
                    } else if (path.is(null, RECORD_DURATION_CLASS)) {
                        duration = extractDuration(xpp.getText());
                    }
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                pop(path, xpp);
                if (path.isEmpty()) {
                    break;
                }
            }
            eventType = lenientNext(xpp);
        }

        if (id == 0 || name == null && anchorName == null || url == null) {
            return null;
        }
        Record record = new Record(id, name == null ? anchorName : name, url);
        record.setDescription(description);
        record.setDate(extractDate(date));
        record.setDuration(duration);
        return record;
    }

    private static long parseIdentifier(String url) {
        Matcher matcher = RECORD_URL_ID_PATTERN.matcher(url);
        if (!matcher.find()) {
            return 0;
        }
        return StringUtils.parseLong(matcher.group(1), 0);
    }

    @Nullable
    private static String extractDate(@Nullable String string) {
        if (string == null || string.isEmpty()) {
            return null;
        }
        Matcher htmlMatcher = HTML_DATE_PATTERN.matcher(string);
        if (htmlMatcher.find()) {
            return htmlMatcher.group(3) + '-' + htmlMatcher.group(2) + '-' + htmlMatcher.group(1);
        }
        Matcher jsonMatcher = JSON_DATE_PATTERN.matcher(string);
        if (jsonMatcher.find()) {
            return jsonMatcher.group(3) + '-' + jsonMatcher.group(2) + '-' + jsonMatcher.group(1);
        }
        return null;
    }

    @Nullable
    private static String extractDuration(@Nullable String string) {
        if (string == null || string.isEmpty()) {
            return null;
        }
        Matcher htmlMatcher = HTML_DURATION_PATTERN.matcher(string);
        String value;
        if (htmlMatcher.find()) {
            value = htmlMatcher.group(1);
        } else {
            Matcher jsonMatcher = JSON_DURATION_PATTERN.matcher(string);
            if (!jsonMatcher.find()) {
                return null;
            }
            value = jsonMatcher.group(1);
        }
        return value.length() == 5 ? "00:" + value : value;
    }

    private static int parseLength(XmlPullParser xpp, long podcast) throws IOException, XmlPullParserException {
        LayoutUtils.Stack path = new LayoutUtils.Stack(5);
        push(path, xpp);

        boolean failure = false;
        int length = 0;

        int eventType = lenientNext(xpp);
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                String tag = xpp.getName();
                push(path, xpp);
                if (!failure) {
                    if (LayoutUtils.isAnchor(tag) && hasClass(xpp, "b-podcast__block-link")) {
                        long id = parsePodcastIdentifier(xpp.getAttributeValue(null, "href"));
                        if (id == 0 || id != podcast) {
                            failure = true;
                        }
                    } else if (LayoutUtils.isBlock(tag) && hasClass(xpp, "b-podcast__number")) {
                        length = parseLength(xpp);
                        continue;
                    }
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                pop(path, xpp);
                if (path.isEmpty()) {
                    break;
                }
            }
            eventType = lenientNext(xpp);
        }
        return length;
    }

    private static long parsePodcastIdentifier(String href) {
        if (href == null || href.isEmpty()) {
            return 0;
        }
        Matcher matcher = PODCAST_HREF_PATTERN.matcher(href);
        if (!matcher.find()) {
            return 0;
        }
        return StringUtils.parseLong(matcher.group(1), 0);
    }

    private static int parseLength(XmlPullParser xpp) throws IOException, XmlPullParserException {
        int eventType = lenientNext(xpp);
        if (eventType == XmlPullParser.TEXT) {
            String text = StringUtils.nonEmptyTrimmed(xpp.getText());
            int length = StringUtils.parseInt(text, 0);
            lenientNext(xpp);
            return length;
        }
        return 0;
    }
}
