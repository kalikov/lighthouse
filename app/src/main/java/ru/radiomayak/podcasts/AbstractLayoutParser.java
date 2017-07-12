package ru.radiomayak.podcasts;

import org.xmlpull.v1.XmlPullParser;

abstract class AbstractLayoutParser {
    static void push(LayoutUtils.Stack stack, XmlPullParser xpp) {
        stack.push(xpp.getName(), xpp.getAttributeValue(null, "class"));
    }

    static void pop(LayoutUtils.Stack stack, XmlPullParser xpp) {
        stack.pop(xpp.getName());
    }

    static boolean hasClass(XmlPullParser parser, String name) {
        String attr = parser.getAttributeValue(null, "class");
        return LayoutUtils.hasClass(attr, name);
    }
}
