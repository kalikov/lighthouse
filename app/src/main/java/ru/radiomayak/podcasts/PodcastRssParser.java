package ru.radiomayak.podcasts;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Stack;

import ru.radiomayak.StringUtils;

class PodcastRssParser {
    Records parse(InputStream input) throws IOException {
        Records records = new Records();
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);

            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(input, null);

            long id = 0;
            String name = null;
            String url = null;
            String description = null;
            String date = null;
            String duration = null;

            Deque<String> path = new ArrayDeque<>(5);

            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    String tag = xpp.getName();
                    path.push(tag);
                    if ("item".equalsIgnoreCase(tag)) {
                        id = 0;
                        name = null;
                        url = null;
                        description = null;
                        date = null;
                        duration = null;
                    } else if ("enclosure".equalsIgnoreCase(tag)) {
                        url = StringUtils.nonEmpty(xpp.getAttributeValue(null, "url"), url);
                        id = 1;
                    }
                } else if (eventType == XmlPullParser.TEXT) {
                    String tag = path.peek();
                    if ("title".equalsIgnoreCase(tag) && path.contains("item")) {
                        name = StringUtils.nonEmpty(xpp.getText());
                    } else if ("itunes:summary".equalsIgnoreCase(tag)) {
                        description = StringUtils.nonEmpty(xpp.getText());
                    } else if ("guid".equalsIgnoreCase(tag)) {
                        url = StringUtils.nonEmpty(xpp.getText(), url);
                        id = 1;
                    } else if ("pubDate".equalsIgnoreCase(tag)) {
                        date = xpp.getText();
                    } else if ("itunes:duration".equalsIgnoreCase(tag)) {
                        duration = xpp.getText();
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    path.pop();
                    String tag = xpp.getName();
                    if ("channel".equalsIgnoreCase(tag)) {
                        break;
                    }
                    if ("item".equalsIgnoreCase(tag) && id != 0 && name != null && url != null) {
                        Record record = new Record(id, name, url);
                        record.setDescription(description);
                        record.setDate(date);
                        record.setDuration(duration);
                        records.add(record);
                    }
                }
                eventType = xpp.next();
            }
            return records;
        } catch (XmlPullParserException e) {
            throw new IOException(e);
        }
    }
}
