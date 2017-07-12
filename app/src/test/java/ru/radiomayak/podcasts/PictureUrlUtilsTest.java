package ru.radiomayak.podcasts;

import org.junit.Assert;
import org.junit.Test;

public class PictureUrlUtilsTest {
    private static final String[] valid = new String[] {
            "http://cdn-st1.rtr-vesti.ru/vh/pictures/bq/675/112.jpg",
            "http://cdn-st4.rtr-vesti.ru/vh/pictures/bq/128/712/3.jpg",
            "http://cdn-st3.rtr-vesti.ru/vh/pictures/it/103/080/2.jpg",
            "https://cdn-st2.rtr-vesti.ru/vh/pictures/o/123/222/5.jpg",
            "https://cdn-st1.rtr-vesti.ru/vh/pictures/r/729/288.jpg",
            "https://cdn-st2.rtr-vesti.ru/vh/pictures/xw/131/137/7.jpg",
            "https://cdn-st3.rtr-vesti.ru/vh/pictures/xw/603/302.png"
    };

    private static final String[] r = new String[] {
            "http://cdn-st1.rtr-vesti.ru/vh/pictures/r/675/112.jpg",
            "http://cdn-st4.rtr-vesti.ru/vh/pictures/r/128/712/3.jpg",
            "http://cdn-st3.rtr-vesti.ru/vh/pictures/r/103/080/2.jpg",
            "https://cdn-st2.rtr-vesti.ru/vh/pictures/r/123/222/5.jpg",
            "https://cdn-st1.rtr-vesti.ru/vh/pictures/r/729/288.jpg",
            "https://cdn-st2.rtr-vesti.ru/vh/pictures/r/131/137/7.jpg",
            "https://cdn-st3.rtr-vesti.ru/vh/pictures/r/603/302.png"
    };

    @Test
    public void shouldGetCorrectUrl() {
        int i = 0;
        for (String url : valid) {
            Assert.assertEquals(r[i], PictureUrlUtils.getPictureUrl(url, PictureUrlUtils.Size.XS_SQUARE));
            i++;
        }
    }
}
