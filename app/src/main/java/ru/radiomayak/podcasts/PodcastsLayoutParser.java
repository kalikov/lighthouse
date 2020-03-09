package ru.radiomayak.podcasts;

import android.support.annotation.Nullable;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import ru.radiomayak.NetworkUtils;
import ru.radiomayak.StringUtils;

class PodcastsLayoutParser extends AbstractLayoutParser {
    private static final String PODCAST_LIST_CLASS = "podcast-list";
    private static final String PODCAST_ITEM_CLASS = "podcast-list__item";
    private static final String PODCAST_ANCHOR_CLASS = "podcast-list__link";
    private static final String PODCAST_DESCRIPTION_CLASS = "podcast-list__info";
    private static final String PODCAST_IMAGE_CLASS = "podcast-list__picture";
    private static final String PODCAST_LENGTH_CLASS = "podcast-list__count";

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
                    if (LayoutUtils.isBlock(tag) && hasClass(xpp, PODCAST_LIST_CLASS)) {
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

    private static Podcasts parsePodcasts(XmlPullParser xpp, @Nullable URI uri) throws IOException, XmlPullParserException {
        Podcasts podcasts = new Podcasts();

        LayoutUtils.ElementStack path = new LayoutUtils.ElementStack(3);
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
        LayoutUtils.ElementStack path = new LayoutUtils.ElementStack(6);
        push(path, xpp);

        boolean isUnderDescription = false;
        boolean isUnderPicture = false;
        boolean isUnderCount = false;
        int nameNesting = 0;
        int descriptionNesting = 0;

        boolean failure = false;
        long id = 0;
        String anchorName = null;
        String name = null;
        String description = null;
        String image = null;
        String length = null;

        int eventType = lenientNext(xpp);
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                String tag = xpp.getName();
                push(path, xpp);
                if (!failure) {
                    if (id == 0 && LayoutUtils.isAnchor(tag) && hasClass(xpp, PODCAST_ANCHOR_CLASS)) {
                        id = parsePodcastIdentifier(xpp.getAttributeValue(null, "href"));
                        if (id == 0) {
                            failure = true;
                        } else {
                            anchorName = StringUtils.nonEmpty(xpp.getAttributeValue(null, "title"));
                        }
                    } else if (image == null && LayoutUtils.isImage(tag) && isUnderPicture) {
                        image = xpp.getAttributeValue(null, "src");
                    } else if (!isUnderPicture && hasClass(xpp, PODCAST_IMAGE_CLASS)) {
                        isUnderPicture = true;
                    } else if (!isUnderDescription && isDescriptionBlock(tag, getClass(xpp))) {
                        isUnderDescription = true;
                    } else if (isUnderDescription && isNameTag(tag)) {
                        nameNesting++;
                    } else if (isUnderDescription && isDescriptionTag(tag)) {
                        descriptionNesting++;
                    } else if (length == null && LayoutUtils.isBlock(tag) && hasClass(xpp, PODCAST_LENGTH_CLASS)) {
                        isUnderCount = true;
                    }
                }
            } else if (!failure && isUnderDescription && (eventType == XmlPullParser.TEXT || eventType == XmlPullParser.ENTITY_REF)) {
                if (nameNesting > 0) {
                    name = appendText(name, xpp);
                } else if (isUnderCount) {
                    length = appendText(length, xpp);
                } else if (descriptionNesting > 0) {
                    description = appendText(description, xpp);
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                LayoutUtils.StackElement element;
                do {
                    element = path.pop();
                    if (isUnderCount && LayoutUtils.isBlock(element.getTag()) && LayoutUtils.hasClass(element.getClassAttribute(), PODCAST_LENGTH_CLASS)) {
                        isUnderCount = false;
                    } if (isUnderDescription && isDescriptionBlock(element.getTag(), element.getClassAttribute())) {
                        isUnderDescription = false;
                    } else if (isUnderDescription && isNameTag(element.getTag())) {
                        nameNesting--;
                    } else if (isUnderDescription && isDescriptionTag(element.getTag())) {
                        descriptionNesting--;
                    } else if (isUnderPicture && LayoutUtils.hasClass(element.getClassAttribute(), PODCAST_IMAGE_CLASS)) {
                        isUnderPicture = false;
                    }
                } while (!element.getTag().equalsIgnoreCase(xpp.getName()) && !path.isEmpty());
                if (path.isEmpty()) {
                    break;
                }
            }
            if (isUnderDescription && !failure) {
                eventType = lenientNextToken(xpp);
            } else {
                eventType = lenientNext(xpp);
            }
        }
        if (failure) {
            return null;
        }
        name = StringUtils.nonEmptyNormalized(name);
        if (id == 0 || name == null && anchorName == null) {
            return null;
        }
        Podcast podcast = new Podcast(id, name == null ? anchorName : name);
        podcast.setDescription(StringUtils.nonEmptyNormalized(description));
        podcast.setLength(length == null ? 0 : parsePodcastLength(length));
        if (image != null && !image.isEmpty()) {
            podcast.setIcon(new Image(image, uri));
        }
        return podcast;
    }

    private static boolean isDescriptionBlock(String tag, String classAttribute) {
        return LayoutUtils.isDiv(tag) && LayoutUtils.hasClass(classAttribute, PODCAST_DESCRIPTION_CLASS);
    }

    private static boolean isNameTag(String tag) {
        return "h5".equalsIgnoreCase(tag) || "h3".equalsIgnoreCase(tag);
    }

    private static boolean isDescriptionTag(String tag) {
        return "p".equalsIgnoreCase(tag);
    }
}
