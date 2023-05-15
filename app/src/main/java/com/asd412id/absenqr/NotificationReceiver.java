package com.asd412id.absenqr;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.PowerManager;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.io.IOException;

public class NotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("STOP_ALARM".equals(intent.getAction())) {
            Intent alarmIntent = new Intent(context, NotificationReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, intent.getIntExtra("code", 0), alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(pendingIntent);
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.cancel(intent.getIntExtra("code", 0));
            MediaPlayer mediaPlayer = GlobalVariables.mediaPlayerMap.get(intent.getIntExtra("code", 0));
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.release();
                GlobalVariables.mediaPlayerMap.remove(intent.getIntExtra("code", 0));
            }
        } else {
            String CHANNEL_ID = "NOTIFICATION";
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setColor(Color.CYAN)
                    .setContentTitle(intent.getStringExtra("ruang"))
                    .setContentText(intent.getStringExtra("name"))
                    .setOngoing(true)
                    .setSound(null)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setAutoCancel(false);

            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            MediaPlayer mediaPlayer = new MediaPlayer();
            GlobalVariables.mediaPlayerMap.put(intent.getIntExtra("code", 0), mediaPlayer);
            try {
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                mediaPlayer.setDataSource(context, alarmSound);
                mediaPlayer.setLooping(true); // set the sound to repeat
                mediaPlayer.prepare();
                mediaPlayer.start();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Add the stop intent to the notification
            Intent stopIntent = new Intent(context, NotificationReceiver.class);
            stopIntent.setAction("STOP_ALARM");
            stopIntent.putExtra("code", intent.getIntExtra("code", 0));
            PendingIntent stopPendingIntent = PendingIntent.getBroadcast(context, intent.getIntExtra("code", 0), stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            builder.addAction(R.drawable.ic_stop, "Hentikan Pengingat", stopPendingIntent);

            // Show the notification
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            notificationManager.notify(intent.getIntExtra("code", 0), builder.build());
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "MyApp::MyWakeLockTag");
            wakeLock.acquire(10*60*1000L /*10 minutes*/);
            wakeLock.release();
        }
    }
}
