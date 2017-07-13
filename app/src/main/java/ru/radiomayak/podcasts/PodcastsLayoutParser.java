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

class PodcastsLayoutParser {
    private static final String PODCAST_LIST_CLASS = "b-podcast__list";
    private static final String PODCAST_ITEM_CLASS = "b-podcast__item--listed";
    private static final String PODCAST_ANCHOR_CLASS = "b-podcast__block-link";
    private static final String PODCAST_DESCRIPTION_CLASS = "b-podcast__description";
    private static final String PODCAST_IMAGE_CLASS = "b-podcast__pic";
    private static final String PODCAST_LENGTH_CLASS = "b-podcast__number";

    private static final Pattern PODCAST_HREF_PATTERN = Pattern.compile("/podcasts/podcast/id/(\\d+)/");

    Podcasts parse(InputStream input, @Nullable String charset, String baseUri) throws IOException {
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setValidating(false);

            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(input, charset);

            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    String tag = xpp.getName();
                    if (LayoutUtils.isDiv(tag) && hasClass(xpp, PODCAST_LIST_CLASS)) {
                        return parsePodcasts(xpp, NetworkUtils.toOptURI(baseUri));
                    }
                }
                eventType = lenientNext(xpp);
            }
            return new Podcasts();
        } catch (XmlPullParserException e) {
            throw new IOException(e);
        }
    }

    private static int lenientNext(XmlPullParser xpp) throws IOException, XmlPullParserException {
        try {
            return xpp.next();
        } catch (XmlPullParserException e) {
            return xpp.getEventType();
        }
    }

    private static int lenientNextToken(XmlPullParser xpp) throws IOException, XmlPullParserException {
        try {
            return xpp.nextToken();
        } catch (XmlPullParserException e) {
            return xpp.getEventType();
        }
    }

    private static void push(LayoutUtils.Stack stack, XmlPullParser xpp) {
        stack.push(xpp.getName(), xpp.getAttributeValue(null, "class"));
    }

    private static void pop(LayoutUtils.Stack stack, XmlPullParser xpp) {
        stack.pop(xpp.getName());
    }

    private static Podcasts parsePodcasts(XmlPullParser xpp, @Nullable URI uri) throws IOException, XmlPullParserException {
        Podcasts podcasts = new Podcasts();

        LayoutUtils.Stack path = new LayoutUtils.Stack(3);
        push(path, xpp);

        int eventType = lenientNext(xpp);
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                String tag = xpp.getName();
                if (LayoutUtils.isDiv(tag) && hasClass(xpp, PODCAST_ITEM_CLASS)) {
                    Podcast podcast = parsePodcast(xpp, uri);
                    if (podcast != null) {
                        podcasts.add(podcast);
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
        return podcasts;
    }

    @Nullable
    private static Podcast parsePodcast(XmlPullParser xpp, @Nullable URI uri) throws IOException, XmlPullParserException {
        LayoutUtils.Stack path = new LayoutUtils.Stack(5);
        push(path, xpp);

        boolean isUnderDescription = false;
        int nameNesting = 0;
        int descriptionNesting = 0;

        boolean failure = false;
        long id = 0;
        String anchorName = null;
        String name = null;
        String description = null;
        String image = null;
        int length = 0;

        int eventType = lenientNext(xpp);
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                String tag = xpp.getName();
                push(path, xpp);
                if (!failure) {
                    if (LayoutUtils.isAnchor(tag) && hasClass(xpp, PODCAST_ANCHOR_CLASS)) {
                        id = parseIdentifier(xpp.getAttributeValue(null, "href"));
                        if (id == 0) {
                            failure = true;
                        } else {
                            anchorName = StringUtils.nonEmpty(xpp.getAttributeValue(null, "title"));
                        }
                    } else if (LayoutUtils.isImage(tag) && hasClass(xpp, PODCAST_IMAGE_CLASS)) {
                        image = xpp.getAttributeValue(null, "src");
                    } else if (LayoutUtils.isDiv(tag) && hasClass(xpp, PODCAST_DESCRIPTION_CLASS)) {
                        isUnderDescription = true;
                    } else if (isNameTag(tag) && isUnderDescription) {
                        nameNesting++;
                    } else if (isDescriptionTag(tag) && isUnderDescription) {
                        descriptionNesting++;
                    } else if (LayoutUtils.isBlock(tag) && hasClass(xpp, PODCAST_LENGTH_CLASS)) {
                        length = parseLength(xpp);
                        continue;
                    }
                }
            } else if (!failure && (eventType == XmlPullParser.TEXT || eventType == XmlPullParser.ENTITY_REF)) {
                if (nameNesting > 0) {
                    String text = xpp.getText();
                    if (name == null) {
                        name = text;
                    } else if (text != null) {
                        name += text;
                    }
                } else if (descriptionNesting > 0) {
                    String text = xpp.getText();
                    if (description == null) {
                        description = text;
                    } else if (text != null) {
                        description += text;
                    }
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                LayoutUtils.StackElement element;
                do {
                    element = path.pop();
                    if (isUnderDescription && LayoutUtils.isDiv(element.getTag()) && element.hasClass(PODCAST_DESCRIPTION_CLASS)) {
                        isUnderDescription = false;
                    } else if (isUnderDescription && isNameTag(element.getTag())) {
                        nameNesting--;
                    } else if (isUnderDescription && isDescriptionTag(element.getTag())) {
                        descriptionNesting--;
                    }
                } while (!element.getTag().equalsIgnoreCase(xpp.getName()) && !path.isEmpty());
                if (path.isEmpty()) {
                    break;
                }
            }
            if (isUnderDescription) {
                eventType = lenientNextToken(xpp);
            } else {
                eventType = lenientNext(xpp);
            }
        }

        name = StringUtils.nonEmptyNormalized(name);
        if (id == 0 || name == null && anchorName == null) {
            return null;
        }
        Podcast podcast = new Podcast(id, name == null ? anchorName : name);
        podcast.setDescription(StringUtils.nonEmptyNormalized(description));
        podcast.setLength(length);
        if (image != null && !image.isEmpty()) {
            podcast.setIcon(new Image(image, uri));
        }
        return podcast;
    }

    private static boolean isNameTag(String tag) {
        return "h5".equalsIgnoreCase(tag);
    }

    private static boolean isDescriptionTag(String tag) {
        return "p".equalsIgnoreCase(tag);
    }

    private static long parseIdentifier(String href) {
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

    private static boolean hasClass(XmlPullParser parser, String name) {
        String attr = parser.getAttributeValue(null, "class");
        return LayoutUtils.hasClass(attr, name);
    }
}
