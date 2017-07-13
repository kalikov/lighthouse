package ru.radiomayak;

import junit.framework.Assert;

import org.junit.Test;

public class StringUtilsTest {
    private final String[][] normalization = new String[][] {
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

    @Test
    public void shouldNormalizeCorrectly() {
        for (String[] strings : normalization) {
            shouldNormalizeCorrectly(strings[0], strings[1]);
        }
    }

    private static void shouldNormalizeCorrectly(String string, String expected) {
        Assert.assertEquals(expected, StringUtils.normalize(string));
    }
}
