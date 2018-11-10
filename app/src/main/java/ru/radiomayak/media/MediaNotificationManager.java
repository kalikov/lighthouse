package ru.radiomayak.media;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import java.util.Objects;

import ru.radiomayak.LighthouseTrack;
import ru.radiomayak.R;
import ru.radiomayak.graphics.BitmapInfo;
import ru.radiomayak.podcasts.MainActivity;
import ru.radiomayak.podcasts.Podcast;
import ru.radiomayak.podcasts.PodcastImageCache;
import ru.radiomayak.podcasts.PodcastsUtils;
import ru.radiomayak.podcasts.Record;

public class MediaNotificationManager extends BroadcastReceiver {
    private static final String TAG = MediaNotificationManager.class.getSimpleName();

    private static final int NOTIFICATION_ID = 412;
    private static final int REQUEST_CODE = 100;

    public static final String ACTION_PAUSE = MediaNotificationManager.class.getPackage().getName() + ".pause";
    public static final String ACTION_PLAY = MediaNotificationManager.class.getPackage().getName() + ".play";
    public static final String ACTION_STOP = MediaNotificationManager.class.getPackage().getName() + ".stop";

    private final NotificationManagerCompat notificationManager;
    private final String channelId;

    private final MediaPlayerService service;

    private final PendingIntent pauseIntent;
    private final PendingIntent playIntent;
    private final PendingIntent stopIntent;

    private NotificationCompat.Builder notificationBuilder;

    public MediaNotificationManager(MediaPlayerService service) {
        this.service = service;

        notificationManager = NotificationManagerCompat.from(service);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelId = createNotificationChannel(service, "my_service", "My Background Service");
        } else {
            channelId = "default";
        }

        String pkg = service.getPackageName();
        pauseIntent = PendingIntent.getBroadcast(service, REQUEST_CODE, new Intent(ACTION_PAUSE).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        playIntent = PendingIntent.getBroadcast(service, REQUEST_CODE, new Intent(ACTION_PLAY).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        stopIntent = PendingIntent.getBroadcast(service, REQUEST_CODE, new Intent(ACTION_STOP).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        notificationManager.cancelAll();
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel(Context context, String channelId, String channelName) {
        NotificationChannel chan = new NotificationChannel(channelId, channelName, NotificationManagerCompat.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility( Notification.VISIBILITY_PRIVATE);
        NotificationManager service = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Objects.requireNonNull(service);
        service.createNotificationChannel(chan);

        return channelId;
    }

    public boolean startNotification() {
        if (notificationBuilder == null) {
            NotificationCompat.Builder builder = notificationBuilder = new NotificationCompat.Builder(service, channelId);
            Notification notification = createNotification(builder);
            if (notification == null) {
                notificationBuilder = null;
                return false;
            }
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_PAUSE);
            filter.addAction(ACTION_PLAY);
            filter.addAction(ACTION_STOP);
            service.registerReceiver(this, filter);

            service.startForeground(NOTIFICATION_ID, notification);
            return true;
        }
        return false;
    }

    public void updateNotification() {
        NotificationCompat.Builder builder = notificationBuilder;
        if (builder != null) {
            Notification notification = createNotification(builder);
            if (notification == null) {
                stopNotification();
            } else {
                notificationManager.notify(NOTIFICATION_ID, notification);
            }
        }
    }

    public void stopNotification() {
        if (notificationBuilder != null) {
            notificationBuilder = null;
            try {
                service.stopForeground(true);
                service.unregisterReceiver(this);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Received intent with action " + action);
        if (ACTION_PAUSE.equals(action)) {
            service.pause();
        } else if (ACTION_PLAY.equals(action)) {
            service.play();
        } else if (ACTION_STOP.equals(action)) {
            service.resetTrack();
        } else {
            Log.w(TAG, "Unknown intent ignored. Action=" + action);
        }
    }

    private PendingIntent createContentIntent(Podcast podcast, Record record) {
        Intent intent = new Intent(service.getApplicationContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(MainActivity.EXTRA_PODCAST, podcast);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        return PendingIntent.getActivity(service.getApplicationContext(), REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Nullable
    private Notification createNotification(NotificationCompat.Builder builder) {
        LighthouseTrack track = service.getTrack();
        if (track == null) {
            return null;
        }
        Podcast podcast = track.getPodcast();
        Record record = track.getRecord();

        RemoteViews remoteViews = new RemoteViews(service.getPackageName(), R.layout.notification);
        RemoteViews bigRemoteViews = new RemoteViews(service.getPackageName(), R.layout.notification_big);

        builder.setSmallIcon(R.drawable.notification_icon)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentTitle(podcast.getName())
                .setContent(remoteViews)
                .setCustomBigContentView(bigRemoteViews)
                .setContentIntent(createContentIntent(podcast, record));

        setRecordState(remoteViews, podcast, record);
        setRecordState(bigRemoteViews, podcast, record);

        setPlayPauseState(remoteViews);
        setPlayPauseState(bigRemoteViews);

        setNotificationPlaybackState(bigRemoteViews);
        builder.setOngoing(service.isPlaying());

        return builder.build();
    }

    private void setRecordState(RemoteViews remoteViews, Podcast podcast, Record record) {
        remoteViews.setTextViewText(android.R.id.title, podcast.getName());
        remoteViews.setTextViewText(android.R.id.text1, record.getName());

        BitmapInfo bitmapInfo = PodcastImageCache.getInstance().getIcon(podcast.getId());
        if (bitmapInfo != null && !bitmapInfo.getBitmap().isRecycled()) {
            remoteViews.setImageViewBitmap(android.R.id.icon, bitmapInfo.getBitmap());
        }
    }

    private void setPlayPauseState(RemoteViews remoteViews) {
        remoteViews.setOnClickPendingIntent(android.R.id.button1, playIntent);
        remoteViews.setOnClickPendingIntent(android.R.id.button2, pauseIntent);
        remoteViews.setOnClickPendingIntent(android.R.id.button3, stopIntent);

        if (service.isPlaying()) {
            remoteViews.setViewVisibility(android.R.id.button1, View.GONE);
            remoteViews.setViewVisibility(android.R.id.button2, View.VISIBLE);
        } else {
            remoteViews.setViewVisibility(android.R.id.button1, View.VISIBLE);
            remoteViews.setViewVisibility(android.R.id.button2, View.GONE);
        }
    }

    private void setNotificationPlaybackState(RemoteViews remoteViews) {
        long pos = service.getCurrentPosition();
        long duration = service.getDuration();
        remoteViews.setTextViewText(android.R.id.text2, PodcastsUtils.formatTime(pos) + '/' + PodcastsUtils.formatTime(duration));
    }
}
