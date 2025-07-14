package com.example.jadwalsholattv;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "PrayerTimeAlarmChannel";
    private static final String CHANNEL_NAME = "Alarm Waktu Sholat";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("AlarmReceiver", "onReceive dipanggil!");

        String prayerName = intent.getStringExtra("prayerName");

        if (prayerName == null) {
            prayerName = "Tidak Diketahui";
        }

        Log.d("AlarmReceiver", "Alarm untuk " + prayerName + " dipicu!");

        showNotification(context, prayerName);

        playAlarmSound(context);

        Toast.makeText(context, "Waktu Sholat " + prayerName + "!", Toast.LENGTH_LONG).show();
    }

    private void showNotification(Context context, String prayerName) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Notifikasi untuk waktu sholat");
            channel.enableVibration(true);
            channel.setSound(Settings.System.DEFAULT_ALARM_ALERT_URI, null);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Waktu Sholat Telah Tiba!")
                .setContentText("Saatnya sholat " + prayerName + ".")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_VIBRATE | NotificationCompat.DEFAULT_LIGHTS);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void playAlarmSound(Context context) {
        // Deklarasikan mediaPlayer di sini
        final MediaPlayer[] mediaPlayer = {null}; // Gunakan array agar bisa diakses di dalam lambda

        try {
            mediaPlayer[0] = MediaPlayer.create(context, Settings.System.DEFAULT_ALARM_ALERT_URI);

            if (mediaPlayer[0] != null) {
                mediaPlayer[0].setLooping(false);
                mediaPlayer[0].start();

                new android.os.Handler().postDelayed(() -> {
                    // Akses mediaPlayer melalui array-nya
                    if (mediaPlayer[0] != null && mediaPlayer[0].isPlaying()) {
                        mediaPlayer[0].stop();
                        mediaPlayer[0].release();
                        mediaPlayer[0] = null; // Set ke null setelah dirilis
                    }
                }, 10000);
            }
        } catch (Exception e) {
            Log.e("AlarmReceiver", "Gagal memutar suara alarm: " + e.getMessage());
            if (mediaPlayer[0] != null) {
                mediaPlayer[0].release();
                mediaPlayer[0] = null; // Set ke null setelah dirilis
            }
        }
    }
}