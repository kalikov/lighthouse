package ru.radiomayak;

import junit.framework.Assert;

import org.junit.Test;

public class StringUtilsTest {
    private static final String[][] normalization = new String[][]{
            {null, null},
            {"", ""},
            {" ", ""},
            {"x x", "x x"},
            {"    ", ""},
            {"y    y", "y y"},
            {" foo ", "foo"},
            {" foo bar", "foo bar"},
            {" bar  ", "bar"},
            {"foo bar  xyz", "foo bar xyz"},
            {"   hello,  world!", "hello, world!"},
            {"Well   well  well!", "Well well well!"},
            {"\u00A0", "\u00A0"},
            {" \u00A0", ""},
            {"c \u00A0c", "c c"},
            {"\u00A0\u00A0", "\u00A0\u00A0"},
            {" \u00A0 \u00A0 \u00A0", ""},
            {"\u00A0good\u00A0 morning!", "\u00A0good morning!"},
            {" \u00A0good\u00A0evening!", "good\u00A0evening!"},
    };

    private static final String[] empty = {"", null};

    private static final String[] nonEmpty = {"foo",  "x"};

    @Test
    public void shouldParseIntCorrectly() {
        Assert.assertEquals(1, StringUtils.parseInt("1", 0));
        Assert.assertEquals(Integer.MAX_VALUE, StringUtils.parseInt(String.valueOf(Integer.MAX_VALUE), 0));
        Assert.assertEquals(Integer.MIN_VALUE, StringUtils.parseInt(String.valueOf(Integer.MIN_VALUE), 0));
        Assert.assertEquals(10, StringUtils.parseInt("", 10));
        Assert.assertEquals(10, StringUtils.parseInt("foo", 10));
    }

    @Test
    public void shouldParseLongCorrectly() {
        Assert.assertEquals(1, StringUtils.parseLong("1", 0));
        Assert.assertEquals(Long.MAX_VALUE, StringUtils.parseLong(String.valueOf(Long.MAX_VALUE), 0));
        Assert.assertEquals(Long.MIN_VALUE, StringUtils.parseLong(String.valueOf(Long.MIN_VALUE), 0));
        Assert.assertEquals(10, StringUtils.parseLong("", 10));
        Assert.assertEquals(10, StringUtils.parseLong("foo", 10));
    }

    @Test
    public void shouldNormalizeCorrectly() {
        for (String[] strings : normalization) {
            shouldNormalizeCorrectly(strings[0], strings[1]);
        }
    }

    private static void shouldNormalizeCorrectly(String string, String expected) {
        Assert.assertEquals(expected, StringUtils.normalize(string));
    }

    @Test
    public void testNonEmpty() {
        for (String string : nonEmpty) {
            Assert.assertEquals(string, StringUtils.nonEmpty(string));
            Assert.assertEquals(string, StringUtils.nonEmpty(string, "foobar"));
        }
        for (String string : empty) {
            Assert.assertNull(StringUtils.nonEmpty(string));
            Assert.assertEquals("foobar", StringUtils.nonEmpty(string, "foobar"));
        }
    }

    @Test
    public void testRequireNonEmpty() {
        for (String string : nonEmpty) {
            Assert.assertEquals(string, StringUtils.requireNonEmpty(string));
        }
        for (String string : empty) {
            try {
                StringUtils.requireNonEmpty(string);
                Assert.fail();
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    @Test
    public void testNonEmptyTrimmed() {
        Assert.assertEquals("foo", StringUtils.nonEmptyTrimmed("foo"));
        Assert.assertEquals("foo", StringUtils.nonEmptyTrimmed("foo  "));
        Assert.assertEquals("foo", StringUtils.nonEmptyTrimmed("  foo"));
        Assert.assertEquals("\u00A0foo", StringUtils.nonEmptyTrimmed(" \u00A0foo"));
        Assert.assertEquals("x", StringUtils.nonEmptyTrimmed("x"));
        Assert.assertNull(StringUtils.nonEmptyTrimmed(" "));
        Assert.assertNull(StringUtils.nonEmptyTrimmed(""));
        Assert.assertNull(StringUtils.nonEmptyTrimmed(null));
    }

    @Test
    public void testNonEmptyNormalized() {
        Assert.assertEquals("foo", StringUtils.nonEmptyNormalized("foo"));
        Assert.assertEquals("foo", StringUtils.nonEmptyNormalized("foo  "));
        Assert.assertEquals("foo", StringUtils.nonEmptyNormalized("  foo"));
        Assert.assertEquals("foo", StringUtils.nonEmptyNormalized(" \u00A0foo"));
        Assert.assertEquals("x", StringUtils.nonEmptyNormalized("x"));
        Assert.assertEquals("\u00A0", StringUtils.nonEmptyNormalized("\u00A0"));
        Assert.assertNull(StringUtils.nonEmptyNormalized(" "));
        Assert.assertNull(StringUtils.nonEmptyNormalized(""));
        Assert.assertNull(StringUtils.nonEmptyNormalized(null));
    }

    @Test
    public void testJoin() {
        Assert.assertEquals("", StringUtils.join(new String[0], "-"));
        Assert.assertEquals("foo", StringUtils.join(new String[]{"foo"}, "-"));
        Assert.assertEquals("foo-bar", StringUtils.join(new String[]{"foo", "bar"}, "-"));
        Assert.assertEquals("foo-bar-baz", StringUtils.join(new String[]{"foo", "bar", "baz"}, "-"));
    }
}