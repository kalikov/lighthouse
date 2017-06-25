package ru.radiomayak.http;

import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.radiomayak.StringUtils;

public class HttpRange {
    private static final Pattern BYTES_PATTERN = Pattern.compile("^\\s*bytes(=|\\s)\\s*");
    private static final Pattern RANGE_PATTERN = Pattern.compile("^\\s*(\\d+)\\-(\\d+)?\\s*(,|/\\s*(\\d+)\\s*$|$)");

    private int from;
    private int to;
    private int length;

    public HttpRange(int from, int to) {
        this.from = from;
        this.to = to;
    }

    public HttpRange(int from, int to, int length) {
        this.from = from;
        this.to = to;
        this.length = length;
    }

    public int getFrom() {
        return from;
    }

    public void setFrom(int from) {
        this.from = from;
    }

    public int getTo() {
        return to;
    }

    public void setTo(int to) {
        this.to = to;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    @Nullable
    public static HttpRange parseFirst(String string) {
        Matcher matcher = matcher(string);
        if (matcher != null) {
            return next(matcher);
        }
        return null;
    }

    @Nullable
    public static HttpRange[] parse(String string) {
        Matcher matcher = matcher(string);
        if (matcher != null) {
            List<HttpRange> ranges = new ArrayList<>();
            HttpRange range;
            while ((range = next(matcher)) != null) {
                ranges.add(range);
            }
            return ranges.toArray(new HttpRange[ranges.size()]);
        }
        return null;
    }

    @Nullable
    public static HttpRange parseBounding(String string) {
        Matcher matcher = matcher(string);
        if (matcher != null) {
            HttpRange bounding = null;
            HttpRange range;
            while ((range = next(matcher)) != null) {
                if (bounding == null) {
                    bounding = range;
                } else {
                    bounding.setFrom(Math.min(bounding.getFrom(), range.getFrom()));
                    bounding.setTo(bounding.getTo() == 0 || range.getTo() == 0 ? 0 : Math.max(bounding.getTo(), range.getTo()));
                    bounding.setLength(Math.max(bounding.getLength(), range.getLength()));
                }
            }
            return bounding;
        }
        return null;
    }

    @Nullable
    private static Matcher matcher(String string) {
        if (string == null) {
            return null;
        }
        Matcher bytesMatcher = BYTES_PATTERN.matcher(string);
        if (bytesMatcher.find()) {
            Matcher rangeMatcher = RANGE_PATTERN.matcher(string);
            rangeMatcher.region(bytesMatcher.end(), string.length());
            return rangeMatcher;
        }
        return null;
    }

    @Nullable
    private static HttpRange next(Matcher matcher) {
        if (matcher.find()) {
            int from = StringUtils.parseInt(matcher.group(1), 0);
            int to = StringUtils.parseInt(matcher.group(2), 0);
            int length = StringUtils.parseInt(matcher.group(4), 0);
            matcher.region(matcher.end(), matcher.regionEnd());
            return new HttpRange(from, to, length);
        }
        return null;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof HttpRange)) {
            return false;
        }
        HttpRange other = (HttpRange) object;
        return from == other.from && to == other.to && length == other.length;
    }

    @Override
    public int hashCode() {
        return 31 * from + to ^ length;
    }

    @Override
    public String toString() {
        String string = to > 0 ? "bytes " + from + "-" + to : "bytes " + from + "-";
        if (length > 0) {
            return string + "/" + length;
        }
        return string;
    }
}
