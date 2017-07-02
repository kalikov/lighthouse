package ru.radiomayak.media;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import ru.radiomayak.LighthouseApplication;
import ru.radiomayak.LighthouseTrack;
import ru.radiomayak.R;
import ru.radiomayak.podcasts.Podcast;
import ru.radiomayak.podcasts.Record;
import ru.radiomayak.podcasts.RecordsActivity;

public class MediaNotificationManager extends BroadcastReceiver {
    private static final String TAG = MediaNotificationManager.class.getSimpleName();

    private static final int NOTIFICATION_ID = 412;
    private static final int REQUEST_CODE = 100;

    public static final String ACTION_PAUSE = MediaNotificationManager.class.getPackage().getName() + ".pause";
    public static final String ACTION_PLAY = MediaNotificationManager.class.getPackage().getName() + ".play";
    public static final String ACTION_STOP = MediaNotificationManager.class.getPackage().getName() + ".stop";

    private final NotificationManagerCompat notificationManager;
    private final LighthouseApplication application;

    private final PendingIntent pauseIntent;
    private final PendingIntent playIntent;
    private final PendingIntent stopIntent;

    private NotificationCompat.Builder notificationBuilder;
    private NotificationCompat.Action playPauseAction;

    public MediaNotificationManager(LighthouseApplication application) {
        this.application = application;

        notificationManager = NotificationManagerCompat.from(application);

        String pkg = application.getPackageName();
        pauseIntent = PendingIntent.getBroadcast(application, REQUEST_CODE, new Intent(ACTION_PAUSE).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        playIntent = PendingIntent.getBroadcast(application, REQUEST_CODE, new Intent(ACTION_PLAY).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        stopIntent = PendingIntent.getBroadcast(application, REQUEST_CODE, new Intent(ACTION_STOP).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        notificationManager.cancelAll();
    }

    public boolean startNotification() {
        if (notificationBuilder == null) {
            notificationBuilder = new NotificationCompat.Builder(application);
            Notification notification = createNotification();
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_PAUSE);
            filter.addAction(ACTION_PLAY);
            filter.addAction(ACTION_STOP);
            application.registerReceiver(this, filter);

            notificationManager.notify(NOTIFICATION_ID, notification);
            return true;
        }
        return false;
    }

    public void updateNotification() {
        if (notificationBuilder != null) {
            Notification notification = createNotification();
            if (notification != null) {
                notificationManager.notify(NOTIFICATION_ID, notification);
            }
        }
    }

    public void stopNotification() {
        if (notificationBuilder != null) {
            notificationBuilder = null;
            playPauseAction = null;
            try {
                notificationManager.cancel(NOTIFICATION_ID);
                application.unregisterReceiver(this);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Received intent with action " + action);
        if (ACTION_PAUSE.equals(action)) {
            application.pause();
        } else if (ACTION_PLAY.equals(action)) {
            application.play();
        } else if (ACTION_STOP.equals(action)) {
            application.resetTrack();
        } else {
            Log.w(TAG, "Unknown intent ignored. Action=" + action);
        }
    }

    private PendingIntent createContentIntent(Podcast podcast, Record record) {
        Intent intent = new Intent(application, RecordsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(RecordsActivity.EXTRA_PODCAST, podcast);
        return PendingIntent.getActivity(application, REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private Notification createNotification() {
        addPlayPauseAction(notificationBuilder);

        LighthouseTrack track = application.getTrack();
        Podcast podcast = track.getPodcast();
        Record record = track.getRecord();

        notificationBuilder.setSmallIcon(R.drawable.notification_play)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setUsesChronometer(true)
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                .setDeleteIntent(stopIntent)
                .setContentIntent(createContentIntent(podcast, record))
                .setContentTitle(podcast.getName())
                .setContentText(record.getName());

        setNotificationPlaybackState(notificationBuilder);

        return notificationBuilder.build();
    }

    private void addPlayPauseAction(NotificationCompat.Builder builder) {
        Log.d(TAG, "updatePlayPauseAction");
        String label;
        int icon;
        PendingIntent intent;
        if (application.getMediaPlayer().isPlaying()) {
            label = application.getString(R.string.pause);
            icon = R.drawable.notification_pause;
            intent = pauseIntent;
        } else {
            label = application.getString(R.string.play);
            icon = R.drawable.notification_play;
            intent = playIntent;
        }
        if (playPauseAction == null) {
            playPauseAction = new NotificationCompat.Action(icon, label, intent);
            builder.addAction(playPauseAction);
        } else {
            playPauseAction.icon = icon;
            playPauseAction.title = label;
            playPauseAction.actionIntent = intent;
        }
    }

    private void setNotificationPlaybackState(NotificationCompat.Builder builder) {
        if (application.getMediaPlayer().getCurrentPosition() >= 0) {
            int duration = application.getMediaPlayer().getDuration();
            long progress = application.getMediaPlayer().getCurrentPosition();
            builder.setProgress(100, (int) (progress * 100 / duration), false);
        } else {
            builder.setProgress(0, 0, false);
        }
        builder.setWhen(0).setShowWhen(false).setUsesChronometer(false);
    }
}
