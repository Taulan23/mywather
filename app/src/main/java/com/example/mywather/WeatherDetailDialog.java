package com.example.mywather;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import com.example.mywather.HourlyForecastAdapter;
import android.app.TimePickerDialog;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Button;
import android.widget.Toast;
import java.util.Calendar;
import android.util.Log;
import android.content.SharedPreferences;
import android.provider.Settings;

public class WeatherDetailDialog extends DialogFragment {
    private WeatherData weatherData;
    private RecyclerView dailyForecastRecyclerView;
    private HourlyForecastAdapter dailyAdapter;
    private List<DailyForecast> dailyForecasts;
    private RequestQueue requestQueue;

    public WeatherDetailDialog(WeatherData weatherData) {
        this.weatherData = weatherData;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_weather_detail, container, false);

        // Инициализация RequestQueue
        requestQueue = Volley.newRequestQueue(requireContext());

        // Инициализация views
        TextView cityNameText = view.findViewById(R.id.cityNameText);
        TextView temperatureText = view.findViewById(R.id.temperatureText);
        TextView humidityText = view.findViewById(R.id.humidityText);
        TextView windSpeedText = view.findViewById(R.id.windSpeedText);
        dailyForecastRecyclerView = view.findViewById(R.id.dailyForecastRecyclerView);

        // Установка данны
        cityNameText.setText(weatherData.cityName);
        temperatureText.setText(String.format("%.1f°C", weatherData.temperature));
        humidityText.setText(String.format("Влажность: %d%%", weatherData.humidity));
        windSpeedText.setText(String.format("Ветер: %.1f м/с", weatherData.windSpeed));

        // Настройка RecyclerView для ежедневного прогноза
        setupDailyForecastRecyclerView();

        // Загружаем прогноз на 5 дней
        loadFiveDayForecast();

        // Добавляем обработчик для кнопки планирования
        Button scheduleButton = view.findViewById(R.id.scheduleNotificationButton);
        scheduleButton.setOnClickListener(v -> showTimePickerDialog());

        return view;
    }

    private void setupDailyForecastRecyclerView() {
        dailyForecasts = new ArrayList<>();
        dailyAdapter = new HourlyForecastAdapter(dailyForecasts);
        dailyForecastRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        dailyForecastRecyclerView.setAdapter(dailyAdapter);
    }

    private void loadFiveDayForecast() {
        String url = String.format("%sforecast?q=%s&appid=%s&units=metric&lang=ru",
                Constants.BASE_URL, weatherData.cityName, Constants.API_KEY);

        Log.d("WeatherDetailDialog", "Loading forecast for: " + weatherData.cityName);
        Log.d("WeatherDetailDialog", "URL: " + url);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    Log.d("WeatherDetailDialog", "Received response: " + response.toString());
                    try {
                        JSONArray list = response.getJSONArray("list");
                        dailyForecasts.clear();

                        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                        SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM, HH:mm", new Locale("ru"));

                        for (int i = 0; i < list.length(); i++) {
                            JSONObject forecast = list.getJSONObject(i);

                            String dtTxt = forecast.getString("dt_txt");
                            Date date = inputFormat.parse(dtTxt);
                            String formattedDate = outputFormat.format(date);

                            JSONObject main = forecast.getJSONObject("main");
                            double temp = main.getDouble("temp");

                            JSONArray weatherArray = forecast.getJSONArray("weather");
                            JSONObject weather = weatherArray.getJSONObject(0);
                            String description = weather.getString("description");
                            String iconCode = weather.getString("icon");

                            DailyForecast dailyForecast = new DailyForecast(
                                    weatherData.cityName,
                                    temp,
                                    description
                            );
                            dailyForecast.date = formattedDate;
                            dailyForecast.iconCode = iconCode;

                            dailyForecasts.add(dailyForecast);
                            Log.d("WeatherDetailDialog", "Added forecast: " + formattedDate + ", " + temp + "°C");
                        }

                        requireActivity().runOnUiThread(() -> {
                            if (dailyAdapter != null) {
                                dailyAdapter.notifyDataSetChanged();
                                Log.d("WeatherDetailDialog", "Notified adapter, items count: " + dailyForecasts.size());
                            }
                        });

                    } catch (Exception e) {
                        Log.e("WeatherDetailDialog", "Error parsing response", e);
                        showError("Ошибка при загрузке прогноза: " + e.getMessage());
                    }
                },
                error -> {
                    Log.e("WeatherDetailDialog", "Network error", error);
                    showError("Ошибка сети: " + error.getMessage());
                }
        );

        request.setShouldCache(false);
        requestQueue.add(request);
    }

    private void showError(String message) {
        requireActivity().runOnUiThread(() -> {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setLayout(width, height);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (requestQueue != null) {
            requestQueue.cancelAll(this);
        }
    }

    private void showTimePickerDialog() {
        Calendar currentTime = Calendar.getInstance();
        new TimePickerDialog(requireContext(),
                (view, hourOfDay, minute) -> {
                    // Находим прогноз на выбранное время
                    findForecastForTime(hourOfDay, minute);
                },
                currentTime.get(Calendar.HOUR_OF_DAY),
                currentTime.get(Calendar.MINUTE),
                true
        ).show();
    }

    private void findForecastForTime(int hourOfDay, int minute) {
        // Создаем календарь с выбранным временем
        Calendar selectedTime = Calendar.getInstance();
        selectedTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
        selectedTime.set(Calendar.MINUTE, minute);

        // Если выбранное время уже прошло, добавляем день
        if (selectedTime.getTimeInMillis() <= System.currentTimeMillis()) {
            selectedTime.add(Calendar.DAY_OF_MONTH, 1);
        }

        // Ищем ближайший прогноз к выбранному времени
        DailyForecast closestForecast = null;
        long minTimeDiff = Long.MAX_VALUE;

        SimpleDateFormat inputFormat = new SimpleDateFormat("dd MMM, HH:mm", new Locale("ru"));

        for (DailyForecast forecast : dailyForecasts) {
            try {
                // Парсим дату прогноза
                Date forecastDate = inputFormat.parse(forecast.date);
                Calendar forecastCal = Calendar.getInstance();
                forecastCal.setTime(forecastDate);

                // Устанавливаем тот же день, что и в выбранном времени
                forecastCal.set(Calendar.YEAR, selectedTime.get(Calendar.YEAR));
                forecastCal.set(Calendar.MONTH, selectedTime.get(Calendar.MONTH));
                forecastCal.set(Calendar.DAY_OF_MONTH, selectedTime.get(Calendar.DAY_OF_MONTH));

                // Вычисляем разницу во времени
                long timeDiff = Math.abs(forecastCal.getTimeInMillis() - selectedTime.getTimeInMillis());

                if (timeDiff < minTimeDiff) {
                    minTimeDiff = timeDiff;
                    closestForecast = forecast;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (closestForecast != null) {
            scheduleNotification(closestForecast, selectedTime);
        } else {
            Toast.makeText(requireContext(),
                    "Не удалось найти прогноз на указанное время",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void scheduleNotification(DailyForecast forecast, Calendar selectedTime) {
        try {
            String notificationText = String.format("Ожидается %s, температура %.1f°C",
                    forecast.description, forecast.temperature);

            Intent intent = new Intent(requireContext(), WeatherAlarmReceiver.class);
            intent.putExtra("cityName", weatherData.cityName);
            intent.putExtra("weatherInfo", notificationText);

            // Создаем уникальный ID для уведомления
            int notificationId = (weatherData.cityName + selectedTime.getTimeInMillis()).hashCode();

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    requireContext(),
                    notificationId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager alarmManager = (AlarmManager)
                    requireContext().getSystemService(Context.ALARM_SERVICE);

            if (alarmManager != null) {
                // Отменяем предыдущие уведомления для этого города
                alarmManager.cancel(pendingIntent);

                // Устанавливаем точное время для уведомления
                Calendar notificationTime = Calendar.getInstance();
                notificationTime.set(Calendar.HOUR_OF_DAY, selectedTime.get(Calendar.HOUR_OF_DAY));
                notificationTime.set(Calendar.MINUTE, selectedTime.get(Calendar.MINUTE));
                notificationTime.set(Calendar.SECOND, 0);
                notificationTime.set(Calendar.MILLISECOND, 0);

                // Если выбранное время уже прошло, добавляем день
                if (notificationTime.getTimeInMillis() <= System.currentTimeMillis()) {
                    notificationTime.add(Calendar.DAY_OF_MONTH, 1);
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        // Устанавливаем одноразовое уведомление
                        alarmManager.setExact(
                                AlarmManager.RTC_WAKEUP,
                                notificationTime.getTimeInMillis(),
                                pendingIntent
                        );
                    } else {
                        Intent alarmIntent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                        startActivity(alarmIntent);
                        return;
                    }
                } else {
                    // Для более старых версий Android
                    alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            notificationTime.getTimeInMillis(),
                            pendingIntent
                    );
                }

                String timeText = String.format("%02d:%02d",
                        selectedTime.get(Calendar.HOUR_OF_DAY),
                        selectedTime.get(Calendar.MINUTE));

                Toast.makeText(requireContext(),
                        "Уведомление запланировано на " + timeText + "\n" + notificationText,
                        Toast.LENGTH_LONG).show();

                Log.d("WeatherDetailDialog", "One-time notification scheduled for " + timeText);
            }
        } catch (Exception e) {
            Log.e("WeatherDetailDialog", "Error scheduling notification", e);
            Toast.makeText(requireContext(),
                    "Ошибка при планировании уведомления",
                    Toast.LENGTH_LONG).show();
        }
    }
}