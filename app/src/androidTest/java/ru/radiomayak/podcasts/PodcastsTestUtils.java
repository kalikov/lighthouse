package ru.radiomayak.podcasts;

import ru.radiomayak.StringUtils;

public final class PodcastsTestUtils {
    private PodcastsTestUtils() {
    }

    public static boolean equals(Image image1, Image image2) {
        return image1 == image2 || image1 != null && image2 != null && StringUtils.equals(image1.getUrl(), image2.getUrl())
                && image1.getPrimaryColor() == image2.getPrimaryColor() && image1.getSecondaryColor() == image2.getSecondaryColor();
    }

    public static boolean equals(Podcast podcast1, Podcast podcast2) {
        return podcast1 == podcast2 || podcast1 != null && podcast2 != null
                && podcast1.getId() == podcast2.getId()
                && podcast1.getLength() == podcast2.getLength()
                && podcast1.getSeen() == podcast2.getSeen()
                && podcast1.getFavorite() == podcast2.getFavorite()
                && StringUtils.equals(podcast1.getName(), podcast2.getName())
                && StringUtils.equals(podcast1.getDescription(), podcast2.getDescription())
                && equals(podcast1.getIcon(), podcast2.getIcon())
                && equals(podcast1.getSplash(), podcast2.getSplash());
    }
}
