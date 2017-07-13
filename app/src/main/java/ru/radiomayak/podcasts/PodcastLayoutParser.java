package ru.radiomayak.podcasts;

import android.support.annotation.Nullable;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.radiomayak.NetworkUtils;
import ru.radiomayak.StringUtils;

class PodcastLayoutParser extends AbstractLayoutParser {
    private static final String PODCAST_ID = "podcast-id";
    private static final String PODCAST_ITEM_CLASS = "b-podcast__item";
    private static final String PODCAST_ANCHOR_CLASS = "b-podcast__block-link";
    private static final String PODCAST_LENGTH_CLASS = "b-podcast__number";
    private static final String PODCAST_SPLASH_CLASS = "b-podcast__pic";
    private static final String PODCAST_LOGO_CLASS = "b-podcast__logo";
    private static final String PODCAST_NEXT_PAGE_CLASS = "b-podcast__records-show-more__btn";

    private static final String RECORDS_LIST_CLASS = "b-podcast__records";
    private static final String RECORD_ITEM_CLASS = "b-podcast__records-item";
    private static final String RECORD_ANCHOR_CLASS = "b-podcast__records-listen";
    private static final String RECORD_NAME_CLASS = "b-podcast__records-name";
    private static final String RECORD_DESCRIPTION_CLASS = "b-podcast__records-description__text";
    private static final String RECORD_DATE_CLASS = "b-podcast__records-date";
    private static final String RECORD_DURATION_CLASS = "b-podcast__records-time";

    private static final Pattern RECORD_URL_ID_PATTERN = Pattern.compile(".+listen\\?id=(\\d+).*");

    private static final Pattern JSON_DATE_PATTERN = Pattern.compile("(\\d{2})\\-(\\d{2})\\-(\\d{4})");
    private static final Pattern HTML_DATE_PATTERN = Pattern.compile("\\:[\\u00A0\\s]*(\\d{2})\\.(\\d{2})\\.(\\d{4})");

    private static final Pattern JSON_DURATION_PATTERN = Pattern.compile("((\\d{2}\\:)?\\d{2}\\:\\d{2})");
    private static final Pattern HTML_DURATION_PATTERN = Pattern.compile("\\:[\\u00A0\\s]*((\\d{2}\\:)?\\d{2}\\:\\d{2})");

    PodcastLayoutContent parse(long target, InputStream input, @Nullable String charset, String baseUri) throws IOException {
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setValidating(false);

            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(input, charset);

            LayoutUtils.Stack stack = new LayoutUtils.Stack(10);

            boolean isUnderLogo = false;
            boolean isUnderPodcast = false;
            boolean isUnderSourcePodcast = false;

            long id = target;
            String name = null;
            String splash = null;
            int length = 0;

            Records records = new Records();
            long nextPage = 0;

            URI uri = NetworkUtils.toOptURI(baseUri);

            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    push(stack, xpp);
                    String tag = xpp.getName();
                    if (LayoutUtils.isDiv(tag) && hasClass(xpp, RECORDS_LIST_CLASS)) {
                        records = parseRecords(xpp, uri);
                    } else if (LayoutUtils.isInput(tag) && PODCAST_ID.equals(xpp.getAttributeValue(null, "id"))) {
                        id = StringUtils.parseLong(xpp.getAttributeValue(null, "value"), id);
                    } else if (isLogoTag(tag, getClass(xpp))) {
                        isUnderLogo = true;
                    } else if (LayoutUtils.isImage(tag) && hasClass(xpp, PODCAST_SPLASH_CLASS) && isUnderLogo) {
                        splash = xpp.getAttributeValue(null, "src");
                        name = StringUtils.nonEmpty(xpp.getAttributeValue(null, "title"));
                    } else if (LayoutUtils.isAnchor(tag) && hasClass(xpp, PODCAST_NEXT_PAGE_CLASS)) {
                        nextPage = StringUtils.parseLong(xpp.getAttributeValue(null, "data-page"), 0);
                        if (id == 0) {
                            // no need to search for podcast length as target podcast is unknown
                            break;
                        }
                    } else if (isPodcastTag(tag, getClass(xpp))) {
                        isUnderPodcast = true;
                    } else if (isUnderPodcast && LayoutUtils.isAnchor(tag) && hasClass(xpp, PODCAST_ANCHOR_CLASS)) {
                        long item = parsePodcastIdentifier(xpp.getAttributeValue(null, "href"));
                        if (item != 0 && item == id) {
                            isUnderSourcePodcast = true;
                        }
                    } else if (isUnderSourcePodcast && LayoutUtils.isBlock(tag) && hasClass(xpp, PODCAST_LENGTH_CLASS)) {
                        length = parsePodcastLength(xpp);
                        // supposed to be the last task of parsing
                        break;
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    LayoutUtils.StackElement element;
                    do {
                        element = stack.pop();
                        if (isUnderLogo && isLogoTag(element.getTag(), element.getClassAttribute())) {
                            isUnderLogo = false;
                        } else if (isUnderPodcast && isPodcastTag(element.getTag(), element.getClassAttribute())) {
                            isUnderPodcast = false;
                            isUnderSourcePodcast = false;
                        }
                    } while (!element.getTag().equalsIgnoreCase(xpp.getName()) && !stack.isEmpty());
                    if (stack.isEmpty()) {
                        break;
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

    private static Records parseRecords(XmlPullParser xpp, @Nullable URI uri) throws IOException, XmlPullParserException {
        Records records = new Records();

        LayoutUtils.Stack path = new LayoutUtils.Stack(3);
        push(path, xpp);

        int eventType = lenientNext(xpp);
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                String tag = xpp.getName();
                if (LayoutUtils.isDiv(tag) && hasClass(xpp, RECORD_ITEM_CLASS)) {
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

        int nameNesting = 0;
        int descriptionNesting = 0;
        int dateNesting = 0;
        int nesting = 0;

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
                if (!failure) {
                    if (LayoutUtils.isAnchor(tag) && hasClass(xpp, RECORD_ANCHOR_CLASS)) {
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
                    } else if (isNameTag(tag, getClass(xpp))) {
                        nameNesting++;
                        nesting++;
                    } else if (isDescriptionTag(tag, getClass(xpp))) {
                        descriptionNesting++;
                        nesting++;
                    } else if (isDateTag(tag, getClass(xpp))) {
                        dateNesting++;
                        nesting++;
                    } else if (isDurationTag(tag, getClass(xpp))) {
                        duration = parseDuration(xpp);
                        eventType = xpp.getEventType();
                        continue;
                    }
                }
            } else if (!failure && nesting > 0 && (eventType == XmlPullParser.TEXT || eventType == XmlPullParser.ENTITY_REF)) {
                if (nameNesting > 0) {
                    name = appendText(xpp, name);
                } else if (descriptionNesting > 0) {
                    description = appendText(xpp, description);
                } else if (dateNesting > 0) {
                    date = appendText(xpp, date);
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                LayoutUtils.StackElement element;
                do {
                    element = path.pop();
                    if (nesting > 0) {
                        if (isNameTag(element.getTag(), element.getClassAttribute())) {
                            nameNesting--;
                            nesting--;
                        } else if (isDescriptionTag(element.getTag(), element.getClassAttribute())) {
                            descriptionNesting--;
                            nesting--;
                        } else if (isDateTag(element.getTag(), element.getClassAttribute())) {
                            dateNesting--;
                            nesting--;
                        }
                    }
                } while (!element.getTag().equalsIgnoreCase(xpp.getName()) && !path.isEmpty());
                if (path.isEmpty()) {
                    break;
                }
            }
            if (nesting > 0) {
                eventType = lenientNextToken(xpp);
            } else {
                eventType = lenientNext(xpp);
            }
        }

        name = StringUtils.nonEmptyNormalized(name);
        if (id == 0 || name == null && anchorName == null || url == null) {
            return null;
        }
        Record record = new Record(id, name == null ? anchorName : name, url, uri);
        record.setDescription(StringUtils.nonEmptyNormalized(description));
        record.setDate(parseDate(date));
        record.setDuration(duration);
        return record;
    }

    private static boolean isLogoTag(String tag, String classAttribute) {
        return LayoutUtils.isDiv(tag) && LayoutUtils.hasClass(classAttribute, PODCAST_LOGO_CLASS);
    }

    private static boolean isPodcastTag(String tag, String classAttribute) {
        return LayoutUtils.isDiv(tag) && LayoutUtils.hasClass(classAttribute, PODCAST_ITEM_CLASS);
    }

    private static boolean isNameTag(String tag, String classAttribute) {
        return LayoutUtils.isBlock(tag) && LayoutUtils.hasClass(classAttribute, RECORD_NAME_CLASS);
    }

    private static boolean isDescriptionTag(String tag, String classAttribute) {
        return LayoutUtils.isBlock(tag) && LayoutUtils.hasClass(classAttribute, RECORD_DESCRIPTION_CLASS);
    }

    private static boolean isDateTag(String tag, String classAttribute) {
        return LayoutUtils.isBlock(tag) && LayoutUtils.hasClass(classAttribute, RECORD_DATE_CLASS);
    }

    private static boolean isDurationTag(String tag, String classAttribute) {
        return LayoutUtils.isBlock(tag) && LayoutUtils.hasClass(classAttribute, RECORD_DURATION_CLASS);
    }

    private static long parseIdentifier(String url) {
        Matcher matcher = RECORD_URL_ID_PATTERN.matcher(url);
        if (!matcher.find()) {
            return 0;
        }
        return StringUtils.parseLong(matcher.group(1), 0);
    }

    @Nullable
    private static String parseDate(@Nullable String string) {
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
    private static String parseDuration(XmlPullParser xpp) throws IOException, XmlPullParserException {
        int eventType = lenientNext(xpp);
        if (eventType == XmlPullParser.TEXT) {
            String duration = parseDuration(xpp.getText());
            lenientNext(xpp);
            return duration;
        }
        return null;
    }

    @Nullable
    private static String parseDuration(@Nullable String string) {
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
}
