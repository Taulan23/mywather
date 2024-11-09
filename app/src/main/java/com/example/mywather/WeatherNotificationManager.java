package com.example.mywather;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;

import java.util.Calendar;
import static com.example.mywather.Constants.API_KEY;
import static com.example.mywather.Constants.BASE_URL;

public class WeatherNotificationManager {
    private static final String CHANNEL_ID = "weather_channel";
    private static final int NOTIFICATION_ID = 1;

    public static void scheduleNotification(Context context, String city, int hour, int minute) {
        createNotificationChannel(context);

        // Создаем Intent для BroadcastReceiver
        Intent intent = new Intent(context, WeatherAlarmReceiver.class);
        intent.putExtra("city", city);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Устанавливаем время уведомления
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        // Если время уже прошло, добавляем день
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        // Настраиваем ежедневное повторение
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                pendingIntent
        );

        Toast.makeText(
                context,
                "Уведомления настроены на " + hour + ":" + String.format("%02d", minute),
                Toast.LENGTH_SHORT
        ).show();
    }

    private static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Прогноз погоды";
            String description = "Уведомления о погоде";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager =
                    context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public static class WeatherAlarmReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String city = intent.getStringExtra("city");
            fetchWeatherAndNotify(context, city);
        }

        private void fetchWeatherAndNotify(Context context, String city) {
            String url = String.format("%sweather?q=%s&appid=%s&units=metric&lang=ru",
                    BASE_URL, city, API_KEY);

            RequestQueue queue = Volley.newRequestQueue(context);
            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.GET,
                    url,
                    null,
                    response -> {
                        try {
                            double temp = response.getJSONObject("main").getDouble("temp");
                            String desc = response.getJSONArray("weather")
                                    .getJSONObject(0)
                                    .getString("description");

                            showNotification(context, city, temp, desc);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    },
                    error -> error.printStackTrace()
            );

            queue.add(request);
        }

        private void showNotification(Context context, String city, double temp, String desc) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notifications)
                    .setContentTitle("Погода в " + city)
                    .setContentText(String.format("%.1f°C, %s", temp, desc))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            try {
                notificationManager.notify(NOTIFICATION_ID, builder.build());
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }
}