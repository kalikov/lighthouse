package ru.radiomayak.podcasts;

import android.content.Context;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

import ru.radiomayak.R;

public final class PodcastsUtils {
    private static final String PODCAST_ICON_FILE = "icon-%s";
    private static final String PODCAST_SPLASH_FILE = "splash-%s";

    private static final String ZERO_TIME_TEXT = "00:00";

    private static String[] months;

    private PodcastsUtils() {
    }

    public static void initialize(Context context) {
        months = context.getString(R.string.months).split(",");
    }

    public static String formatDate(long time) {
        Calendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(time);
        return calendar.get(Calendar.DAY_OF_MONTH) + " " + months[calendar.get(Calendar.MONTH)] + " " + calendar.get(Calendar.YEAR);
    }

    public static int parseMonth(String month) {
        for (int i = 0; i < months.length; i++) {
            if (months[i].equalsIgnoreCase(month)) {
                return i + 1;
            }
        }
        return 1;
    }

    public static String formatTime(long msecs) {
        if (msecs <= 0) {
            return ZERO_TIME_TEXT;
        }
        return formatSeconds(msecs / 1000);
    }

    public static String formatSeconds(long seconds) {
        if (seconds <= 0) {
            return ZERO_TIME_TEXT;
        }
        long secs = seconds % 60;
        long mins = (seconds / 60) % 60;
        long hours = seconds / 3600;
        if (hours > 0) {
            return String.format(Locale.ROOT, "%d:%02d:%02d", hours, mins, secs);
        }
        return String.format(Locale.ROOT, "%02d:%02d", mins, secs);
    }

    static String getPodcastIconFilename(Podcast podcast) {
        return String.format(PODCAST_ICON_FILE, String.valueOf(podcast.getId()));
    }

    static String getPodcastSplashFilename(Podcast podcast) {
        return String.format(PODCAST_SPLASH_FILE, String.valueOf(podcast.getId()));
    }
}
