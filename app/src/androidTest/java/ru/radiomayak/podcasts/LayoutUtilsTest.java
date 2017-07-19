package ru.radiomayak.podcasts;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class LayoutUtilsTest {
    private static final String[][] strings = new String[][] {
            {"<p style=\"text-align: justify;\">foo: &nbsp;bar xyz?</p>\r\n\r\n<p>hello,&nbsp;world!.</p>", "foo: bar xyz? hello,\u00A0world!."},
            {"Происходят из крестьян сельца Добродеева Ярославской губернии", "Происходят из крестьян сельца Добродеева Ярославской губернии"}
    };

    @Test
    public void shouldCleanHtml() {
        for (String[] item : strings) {
            Assert.assertEquals(item[1], LayoutUtils.clean(item[0]));
        }
    }
}
