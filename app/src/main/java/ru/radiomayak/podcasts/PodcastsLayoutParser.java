package ru.radiomayak.podcasts;

import android.support.annotation.Nullable;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.radiomayak.NetworkUtils;
import ru.radiomayak.StringUtils;

class PodcastsLayoutParser {
    private static final String PODCAST_LIST_QUERY = ".b-podcast__list";
    private static final String PODCAST_ITEM_QUERY = ".b-podcast__item--listed";
    private static final String PODCAST_ANCHOR_QUERY = "a.b-podcast__block-link";
    private static final String PODCAST_NAME_QUERY = ".b-podcast__description h5";
    private static final String PODCAST_DESCRIPTION_QUERY = ".b-podcast__description p";
    private static final String PODCAST_IMAGE_QUERY = "img.b-podcast__pic";
    private static final String PODCAST_LENGTH_QUERY = ".b-podcast__number";

    private static final Pattern PODCAST_HREF_PATTERN = Pattern.compile("/podcasts/podcast/id/(\\d+)/");

    Podcasts parse(InputStream input, @Nullable String charset, String baseUri) throws IOException {
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setValidating(false);

            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(input, null);

            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    String tag = xpp.getName();
                    if ("div".equalsIgnoreCase(tag) && hasClass(xpp, "b-podcast__list")) {
                        return parsePodcasts(xpp, NetworkUtils.toOptURI(baseUri));
                    }
                }
                eventType = lenientNext(xpp);
            }
            return null;
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

    private static Podcasts parsePodcasts(XmlPullParser xpp, @Nullable URI uri) throws IOException, XmlPullParserException {
        Podcasts podcasts = new Podcasts();

        Deque<String> path = new ArrayDeque<>(5);

        path.push(xpp.getName());

        int eventType = lenientNext(xpp);
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                String tag = xpp.getName();
                if ("div".equalsIgnoreCase(tag) && hasClass(xpp, "b-podcast__item--listed")) {
                    Podcast podcast = parsePodcast(xpp, uri);
                    if (podcast != null) {
                        podcasts.add(podcast);
                    }
                } else {
                    path.push(tag);
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                String tag = xpp.getName();
                String pop;
                do {
                    pop = path.pop();
                } while (!pop.equalsIgnoreCase(tag) && !path.isEmpty());
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
        Deque<String> path = new ArrayDeque<>(5);

        long id = 0;
        String anchorName = null;
        String name = null;
        String description = null;
        String image = null;
        int length = 0;

        path.push(xpp.getName());

        int eventType = lenientNext(xpp);
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                String tag = xpp.getName();
                path.push(tag);
                if ("a".equalsIgnoreCase(tag) && hasClass(xpp, "b-podcast__block-link")) {
                    String href = xpp.getAttributeValue(null, "href");
                    if (href == null || href.isEmpty()) {
                        return null;
                    }
                    Matcher matcher = PODCAST_HREF_PATTERN.matcher(href);
                    if (!matcher.find()) {
                        return null;
                    }
                    id = StringUtils.parseLong(matcher.group(1), 0);
                    if (id == 0) {
                        return null;
                    }
                    anchorName = xpp.getAttributeValue(null, "title");
                } else if ("img".equalsIgnoreCase(tag) && hasClass(xpp, "b-podcast__pic")) {
                    image = xpp.getAttributeValue(null, "src");
                }
            } else if (eventType == XmlPullParser.TEXT) {
                String tag = path.peek();
                if ("h5".equalsIgnoreCase(tag)) {
                    name = xpp.getText();
                    if (name == null || name.isEmpty()) {
                        return null;
                    }
                } else if ("p".equalsIgnoreCase(tag)) {
                    description = StringUtils.nonEmpty(xpp.getText());
                } else if ("span".equalsIgnoreCase(tag)) {
                    length = StringUtils.parseInt(xpp.getText(), 0);
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                String tag = xpp.getName();
                String pop;
                do {
                    pop = path.pop();
                } while (!pop.equalsIgnoreCase(tag) && !path.isEmpty());
                if (path.isEmpty()) {
                    break;
                }
            }
            eventType = lenientNext(xpp);
        }

        if (id == 0 || name == null && anchorName == null) {
            return null;
        }
        Podcast podcast = new Podcast(id, name == null ? anchorName : name);
        podcast.setDescription(description);
        podcast.setLength(length);
        if (image != null && !image.isEmpty()) {
            podcast.setIcon(new Image(image, uri));
        }
        return podcast;
    }

    private static boolean hasClass(XmlPullParser parser, String name) {
        String attr = parser.getAttributeValue(null, "class");
        if (attr == null || attr.isEmpty()) {
            return false;
        }
        return attr.contains(name);
    }
}
