package ru.radiomayak.http;

import ru.radiomayak.http.message.BasicNameValuePair;

import org.junit.Assert;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;

public class HttpUtilsTest {
    @Test
    public void testOneParam() throws UnsupportedEncodingException {
        HttpRequestParams params = HttpUtils.parseQuery("foo=bar", "UTF-8");
        Assert.assertEquals(Collections.singletonList(pair("foo", "bar")), params.getList());
    }

    @Test
    public void testEncodedParam() throws UnsupportedEncodingException {
        HttpRequestParams params = HttpUtils.parseQuery("%02hello%7f bye", "UTF-8");
        Assert.assertEquals(Collections.singletonList(pair("\u0002hello\u007f bye", null)), params.getList());
    }

    @Test
    public void testMultipleParams() throws UnsupportedEncodingException {
        HttpRequestParams params = HttpUtils.parseQuery("q=&lt;asdf&gt;", "UTF-8");
        Assert.assertEquals(Arrays.asList(pair("q", ""), pair("lt;asdf", null), pair("gt;", null)), params.getList());
    }

    private static NameValuePair pair(String name, String value) {
        return new BasicNameValuePair(name, value);
    }
}
