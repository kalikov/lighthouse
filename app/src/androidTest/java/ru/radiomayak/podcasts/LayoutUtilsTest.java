package ru.radiomayak.podcasts;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class LayoutUtilsTest {
    @Test
    public void shouldCleanHtml() {
        Assert.assertEquals("foo: bar xyz? hello,\u00A0world!.", LayoutUtils.clean("<p style=\"text-align: justify;\">foo: &nbsp;bar xyz?</p>\r\n\r\n<p>hello,&nbsp;world!.</p>"));
    }
}
