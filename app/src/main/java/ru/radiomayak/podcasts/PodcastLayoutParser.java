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
    private static final String PODCAST_HEADER_CLASS = "podcast-header";
    private static final String PODCAST_TITLE_CLASS = "podcast-header__title";
    private static final String PODCAST_COMPLETED_CLASS = "podcast-header__completed";
    private static final String PODCAST_UPDATING_CLASS = "podcast-header__updating";
    private static final String PODCAST_LENGTH_CLASS = "podcast-header__count";
    private static final String PODCAST_LOGO_CLASS = "podcast-header__picture";
    private static final String PODCAST_NEXT_PAGE_CLASS = "podcast-episodes--more";

    private static final String RECORDS_LIST_CLASS = "podcast-episodes__list";
    private static final String RECORD_ITEM_CLASS = "podcast-episodes__item";
    private static final String RECORD_LISTEN_ANCHOR_CLASS = "podcast-episodes__listen";
    private static final String RECORD_DOWNLOAD_ANCHOR_CLASS = "podcast-episodes__download";
    private static final String RECORD_NAME_CLASS = "podcast-episodes__title";
    private static final String RECORD_DESCRIPTION_CLASS = "podcast-episodes__anons";
    private static final String RECORD_DATE_CLASS = "podcast-episodes__date";
    private static final String RECORD_DURATION_CLASS = "podcast-episodes__duration";

    private static final Pattern RECORD_URL_ID_PATTERN = Pattern.compile(".+(?:listen|download)\\?id=(\\d+).*");

    private static final Pattern JSON_DATE_PATTERN = Pattern.compile("(\\d{2})\\-(\\d{2})\\-(\\d{4})");
    private static final Pattern HTML_DATE_PATTERN = Pattern.compile("[\\u00A0\\s]*(\\d{2})\\.(\\d{2})\\.(\\d{4})");
    private static final Pattern HTML_STRING_DATE_PATTERN = Pattern.compile("[\\u00A0\\s]*(\\d{2})[\\u00A0\\s]+(\\D+)[\\u00A0\\s]+(\\d{4})");

    private static final Pattern JSON_DURATION_PATTERN = Pattern.compile("((\\d{2}\\:)?\\d{2}\\:\\d{2})");
    private static final Pattern HTML_DURATION_PATTERN = Pattern.compile("[\\u00A0\\s]*((\\d{2}\\:)?\\d{2}\\:\\d{2})");

    PodcastLayoutContent parse(long target, InputStream input, @Nullable String charset, String baseUri) throws IOException {
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setValidating(false);

            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(input, charset);

            LayoutUtils.ElementStack stack = new LayoutUtils.ElementStack(15);

            boolean isUnderLogo = false;
            boolean isUnderHeader = false;
            boolean isUnderTitle = false;
            boolean isUnderCount = false;
            boolean wasComment = false;
            Boolean isArchived = null;

            long id = target;
            String name = null;
            String splash = null;
            String length = null;

            Records records = null;
            long nextPage = 0;

            URI uri = NetworkUtils.toOptURI(baseUri);

            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.COMMENT) {
                    wasComment = true;
                } else if (eventType == XmlPullParser.START_TAG) {
                    wasComment = false;
                    push(stack, xpp);
                    String tag = xpp.getName();
                    if (records == null && LayoutUtils.isBlock(tag) && hasClass(xpp, RECORDS_LIST_CLASS)) {
                        records = parseRecords(xpp, uri);
                    } else if (id == 0 && LayoutUtils.isInput(tag) && PODCAST_ID.equals(xpp.getAttributeValue(null, "id"))) {
                        id = StringUtils.parseLong(xpp.getAttributeValue(null, "value"), id);
                    } else if (!isUnderLogo && isUnderHeader && isLogoTag(tag, getClass(xpp))) {
                        isUnderLogo = true;
                    } else if (isUnderLogo && LayoutUtils.isImage(tag)) {
                        splash = xpp.getAttributeValue(null, "src");
                        name = StringUtils.nonEmpty(xpp.getAttributeValue(null, "title"));
                    } else if (nextPage == 0 && (LayoutUtils.isAnchor(tag) || LayoutUtils.isBlock(tag)) && hasClass(xpp, PODCAST_NEXT_PAGE_CLASS)) {
                        nextPage = StringUtils.parseLong(xpp.getAttributeValue(null, "data-page"), 0);
                    } else if (!isUnderHeader && isHeaderTag(tag, getClass(xpp))) {
                        isUnderHeader = true;
                    } else if (isUnderHeader && !isUnderCount && LayoutUtils.isBlock(tag) && hasClass(xpp, PODCAST_LENGTH_CLASS)) {
                        isUnderCount = true;
                    } else if (isUnderHeader && !isUnderTitle && LayoutUtils.isHeader(tag) && hasClass(xpp, PODCAST_TITLE_CLASS)) {
                        isUnderTitle = true;
                    } else if (isUnderHeader && isArchived == null && (hasClass(xpp, PODCAST_COMPLETED_CLASS) || hasClass(xpp, PODCAST_UPDATING_CLASS))) {
                        isArchived = hasClass(xpp, PODCAST_COMPLETED_CLASS) ? Boolean.TRUE : Boolean.FALSE;
                    }
                } else if (isUnderHeader && (eventType == XmlPullParser.TEXT || eventType == XmlPullParser.ENTITY_REF)) {
                    if (isUnderCount) {
                        length = appendText(length, xpp.getText());
                    } else if (isUnderTitle) {
                        name = appendText(name, xpp.getText());
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    LayoutUtils.StackElement element;
                    do {
                        if (wasComment) {
                            element = stack.peek();
                            if (!element.getTag().equalsIgnoreCase(xpp.getName())) {
                                break;
                            }
                        }
                        element = stack.pop();
                        if (isUnderLogo && isLogoTag(element.getTag(), element.getClassAttribute())) {
                            isUnderLogo = false;
                        } else if (isUnderHeader && isHeaderTag(element.getTag(), element.getClassAttribute())) {
                            isUnderHeader = false;
                        } else if (isUnderCount && LayoutUtils.isBlock(element.getTag()) && LayoutUtils.hasClass(element.getClassAttribute(), PODCAST_LENGTH_CLASS)) {
                            isUnderCount = false;
                        } else if (isUnderTitle && LayoutUtils.isHeader(element.getTag()) && LayoutUtils.hasClass(element.getClassAttribute(), PODCAST_TITLE_CLASS)) {
                            isUnderTitle = false;
                        }
                    } while (!element.getTag().equalsIgnoreCase(xpp.getName()) && !stack.isEmpty());
                    if (stack.isEmpty()) {
                        break;
                    }
                }
                if (isUnderHeader) {
                    eventType = lenientNextToken(xpp);
                } else {
                    eventType = lenientNext(xpp);
                }
            }

            Podcast podcast = null;
            name = StringUtils.nonEmptyTrimmed(name);
            if (id != 0 && name != null) {
                podcast = new Podcast(id, name);
                if (splash != null && !splash.isEmpty()) {
                    podcast.setSplash(new Image(splash, uri));
                }
                podcast.setLength(length == null ? 0 : parsePodcastLength(length));
            }

            return new PodcastLayoutContent(podcast, isArchived == Boolean.TRUE, records == null ? new Records() : records, nextPage);
        } catch (XmlPullParserException e) {
            throw new IOException(e);
        }
    }

    private static Records parseRecords(XmlPullParser xpp, @Nullable URI uri) throws IOException, XmlPullParserException {
        Records records = new Records();

        LayoutUtils.ElementStack path = new LayoutUtils.ElementStack(3);
        push(path, xpp);

        int eventType = lenientNext(xpp);
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                String tag = xpp.getName();
                if (LayoutUtils.isBlock(tag) && hasClass(xpp, RECORD_ITEM_CLASS)) {
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
        LayoutUtils.ElementStack path = new LayoutUtils.ElementStack(10);
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
                    if (id == 0 && LayoutUtils.isAnchor(tag)) {
                        if (hasClass(xpp, RECORD_LISTEN_ANCHOR_CLASS)) {
                            url = xpp.getAttributeValue(null, "data-url");
                            if (url == null || url.isEmpty()) {
                                url = xpp.getAttributeValue(null, "href");
                            }
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
                        } else if (hasClass(xpp, RECORD_DOWNLOAD_ANCHOR_CLASS) && url == null) {
                            String href = xpp.getAttributeValue(null, "href");
                            Matcher matcher = RECORD_URL_ID_PATTERN.matcher(href);
                            if (matcher.find()) {
                                id = StringUtils.parseLong(matcher.group(1), 0);
                                if (id == 0) {
                                    failure = true;
                                } else {
                                    url = href;
                                }
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
                    } else if (nameNesting == 0 && descriptionNesting > 0 && "br".equalsIgnoreCase(tag)) {
                        description = appendText(description, "<br>");
                    }
                }
            } else if (!failure && nesting > 0 && (eventType == XmlPullParser.TEXT || eventType == XmlPullParser.ENTITY_REF)) {
                if (nameNesting > 0) {
                    name = appendText(name, xpp);
                } else if (descriptionNesting > 0) {
                    description = appendText(description, xpp);
                } else if (dateNesting > 0) {
                    date = appendText(date, xpp);
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
            if (!failure && nesting > 0) {
                eventType = lenientNextToken(xpp);
            } else {
                eventType = lenientNext(xpp);
            }
        }
        if (failure) {
            return null;
        }
        name = StringUtils.nonEmptyNormalized(name);
        if (id == 0 || name == null && anchorName == null || url == null) {
            return null;
        }
        Record record = new Record(id, name == null ? anchorName : name, url, uri);
        record.setDescription(LayoutUtils.br2nl(StringUtils.nonEmptyNormalized(description)));
        record.setDate(parseDate(date));
        record.setDuration(duration);
        return record;
    }

    private static boolean isLogoTag(String tag, String classAttribute) {
        return LayoutUtils.isDiv(tag) && LayoutUtils.hasClass(classAttribute, PODCAST_LOGO_CLASS);
    }

    private static boolean isHeaderTag(String tag, String classAttribute) {
        return LayoutUtils.isHeader(tag) && LayoutUtils.hasClass(classAttribute, PODCAST_HEADER_CLASS);
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
        Matcher stringMatcher = HTML_STRING_DATE_PATTERN.matcher(string);
        if (stringMatcher.find()) {
            String monthString = stringMatcher.group(2);
            int month = PodcastsUtils.parseMonth(monthString);
            String monthFormatted = month >= 10 ? String.valueOf(month) : '0' + String.valueOf(month);
            return stringMatcher.group(3) + '-' + monthFormatted + '-' + stringMatcher.group(1);
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
        if (value.length() > 5 && value.startsWith("00:")) {
            return value.substring(3);
        }
        return value;
    }
}
