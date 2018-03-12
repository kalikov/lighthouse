package ru.radiomayak.podcasts;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import ru.radiomayak.R;

public class DownloadNotificationManager extends BroadcastReceiver {
    private static final String TAG = DownloadNotificationManager.class.getSimpleName();

    private static final int NOTIFICATION_ID = 413;
    private static final int REQUEST_CODE = 100;

    public static final String ACTION_CANCEL = DownloadNotificationManager.class.getPackage().getName() + ".CANCEL";

    private final NotificationManagerCompat notificationManager;
    private final String channelId;

    private final DownloadService service;

    private final PendingIntent cancelIntent;

    private NotificationCompat.Builder notificationBuilder;
    private boolean addAction = true;

    public DownloadNotificationManager(DownloadService service) {
        this.service = service;

        notificationManager = NotificationManagerCompat.from(service);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelId = NotificationChannel.DEFAULT_CHANNEL_ID;
        } else {
            channelId = "default";
        }

        String pkg = service.getPackageName();
        cancelIntent = PendingIntent.getBroadcast(service, REQUEST_CODE, new Intent(ACTION_CANCEL).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        notificationManager.cancelAll();
    }

    public boolean startNotification(String text) {
        if (notificationBuilder == null) {
            notificationBuilder = new NotificationCompat.Builder(service, channelId);
            addAction = true;
            Notification notification = createNotification(text);
            if (notification == null) {
                notificationBuilder = null;
                return false;
            }
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_CANCEL);
            service.registerReceiver(this, filter);

            service.startForeground(NOTIFICATION_ID, notification);
            return true;
        }
        return false;
    }

    public void updateNotification(String text) {
        if (notificationBuilder != null) {
            Notification notification = createNotification(text);
            if (notification == null) {
                stopNotification();
            } else {
                notificationManager.notify(NOTIFICATION_ID, notification);
            }
        }
    }

    public void updateNotification(int progress, int max) {
        if (notificationBuilder != null) {
            notificationBuilder.setProgress(max, progress, false);
            Notification notification = notificationBuilder.build();
            notificationManager.notify(NOTIFICATION_ID, notification);
        }
    }

    public void updateDoneNotification() {
        if (notificationBuilder != null) {
            notificationBuilder.setProgress(0, 0, false);
            notificationBuilder.setContentTitle(service.getResources().getString(R.string.saved));
            Notification notification = notificationBuilder.build();
            notificationManager.notify(NOTIFICATION_ID, notification);
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
        if (ACTION_CANCEL.equals(action)) {
            service.cancel();
        } else {
            Log.w(TAG, "Unknown intent ignored. Action=" + action);
        }
    }

//    private PendingIntent createContentIntent(Podcast podcast, Record record) {
//        TaskStackBuilder stackBuilder = TaskStackBuilder.create(service.getApplicationContext());
//        stackBuilder.addParentStack(RecordsActivity.class);
//
//        Intent intent = new Intent(service.getApplicationContext(), RecordsActivity.class);
//        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
//        intent.putExtra(RecordsActivity.EXTRA_PODCAST, podcast);
//        stackBuilder.addNextIntent(intent);
//        return stackBuilder.getPendingIntent(REQUEST_CODE, PendingIntent.FLAG_UPDATE_CURRENT);
//    }

    @Nullable
    private Notification createNotification(String text) {
//        LighthouseTrack track = service.getTrack();
//        if (track == null) {
//            return null;
//        }
//        Podcast podcast = track.getPodcast();
//        Record record = track.getRecord();
//
        notificationBuilder.setSmallIcon(android.R.drawable.stat_sys_download)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentTitle(service.getResources().getString(R.string.downloading))
                .setProgress(0, 0, true)
//                .setContentIntent(createContentIntent(podcast, record))
                .setOngoing(true);

        if (text != null) {
            notificationBuilder.setContentText(text);
        }

        if (addAction) {
            String cancelText = service.getResources().getString(android.R.string.cancel);
            notificationBuilder.addAction(android.R.drawable.ic_menu_close_clear_cancel, cancelText, cancelIntent);
            addAction = false;
        }

        return notificationBuilder.build();
    }
}
