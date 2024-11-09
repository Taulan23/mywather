package com.example.mywather;

import android.os.Bundle;
import android.widget.SearchView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import android.widget.Toast;
import com.android.volley.DefaultRetryPolicy;
import static com.example.mywather.Constants.API_KEY;
import static com.example.mywather.Constants.BASE_URL;
import android.util.Log;
import android.app.TimePickerDialog;
import java.util.Calendar;
import androidx.recyclerview.widget.DividerItemDecoration;
import com.android.volley.NetworkError;
import com.android.volley.TimeoutError;
import com.android.volley.NoConnectionError;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.net.Uri;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.core.widget.NestedScrollView;
import android.media.AudioAttributes;
import android.graphics.Color;
import android.app.Notification;
import android.media.RingtoneManager;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.app.AlarmManager;
import androidx.annotation.NonNull;
import android.content.Context;
import android.content.Intent;
import androidx.appcompat.app.AlertDialog;
import java.io.File;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private RecyclerView citiesRecyclerView;
    private WeatherAdapter weatherAdapter;
    private List<WeatherData> weatherDataList;
    private RequestQueue requestQueue;
    private boolean isDarkTheme = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Запрашиваем разрешения при запуске
        requestNotificationPermissions();
        createNotificationChannel();

        initializeViews();
        setupRecyclerView();
        setupSearchView();
        setupThemeButton();
        setupNotificationButton();
        setupSwipeRefresh();

        requestQueue = Volley.newRequestQueue(this);
        requestQueue.getCache().clear();

        loadDefaultCitiesParallel();
    }

    private void initializeViews() {
        citiesRecyclerView = findViewById(R.id.citiesRecyclerView);
        requestQueue = Volley.newRequestQueue(this);
        weatherDataList = new ArrayList<>();
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        citiesRecyclerView.setLayoutManager(layoutManager);

        weatherAdapter = new WeatherAdapter(weatherDataList, this::onCityClick);
        citiesRecyclerView.setAdapter(weatherAdapter);

        // Добавляем разделители между элементами
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(
                citiesRecyclerView.getContext(),
                layoutManager.getOrientation()
        );
        citiesRecyclerView.addItemDecoration(dividerItemDecoration);
    }

    private void setupSearchView() {
        SearchView searchView = findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                weatherDataList.clear();
                weatherAdapter.notifyDataSetChanged();
                fetchWeatherData(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }

    private void setupThemeButton() {
        findViewById(R.id.themeButton).setOnClickListener(v -> toggleTheme());
    }

    private void setupNotificationButton() {
        findViewById(R.id.notificationButton).setOnClickListener(v -> {
            // Открываем настройки уведомлений приложения
            Intent intent = new Intent();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            } else {
                intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
                intent.putExtra("app_package", getPackageName());
                intent.putExtra("app_uid", getApplicationInfo().uid);
            }
            startActivity(intent);
        });
    }

    private void loadDefaultCitiesParallel() {
        weatherDataList.clear();
        weatherAdapter.notifyDataSetChanged();

        String[] defaultCities = {"Chelyabinsk", "Moscow", "Saint Petersburg", "Yekaterinburg", "Novosibirsk"};

        // Создаем список запросов
        List<JsonObjectRequest> requests = new ArrayList<>();

        // Подготавливаем все запросы
        for (String city : defaultCities) {
            String url = String.format("%sweather?q=%s&appid=%s&units=metric&lang=ru",
                    BASE_URL, city, API_KEY);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.GET,
                    url,
                    null,
                    response -> {
                        try {
                            WeatherData weatherData = parseWeatherData(response);
                            runOnUiThread(() -> {
                                updateWeatherList(weatherData);
                            });
                        } catch (JSONException e) {
                            showError("Ошибка при обработке данных для города " + city);
                        }
                    },
                    error -> {
                        String errorMessage;
                        if (error.networkResponse != null && error.networkResponse.statusCode == 404) {
                            errorMessage = "Город " + city + " не найден";
                        } else {
                            errorMessage = "Ошибка загрузки данных для города " + city;
                        }
                        showError(errorMessage);
                    }
            );

            // Устанавливаем высокий приоритет и короткий таймаут
            request.setShouldCache(false); // Отключаем кеширование
            request.setRetryPolicy(new DefaultRetryPolicy(
                    5000, // 5 секунд таймаут
                    1,    // Без повторных попыток
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));

            requests.add(request);
        }

        // Отправляем все запросы одновременно
        for (JsonObjectRequest request : requests) {
            requestQueue.add(request);
        }
    }

    private void fetchWeatherData(String city) {
        String url = String.format("%sweather?q=%s&appid=%s&units=metric&lang=ru",
                BASE_URL, city, API_KEY);

        Log.d(TAG, "Fetching weather data for: " + city + " URL: " + url);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        Log.d(TAG, "Response received for " + city + ": " + response.toString());
                        WeatherData weatherData = parseWeatherData(response);
                        runOnUiThread(() -> {
                            updateWeatherList(weatherData);
                            Toast.makeText(this,
                                    "Данные для города " + city + " обновлены",
                                    Toast.LENGTH_SHORT).show();
                        });
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing data for " + city, e);
                        showError("Ошибка пр обраотке данных для города " + city);
                    }
                },
                error -> {
                    Log.e(TAG, "Error fetching data for " + city, error);
                    String errorMessage;
                    if (error.networkResponse != null) {
                        switch (error.networkResponse.statusCode) {
                            case 404:
                                errorMessage = "Город " + city + " не найден";
                                break;
                            case 401:
                                errorMessage = "Ошибка авторизации API";
                                break;
                            default:
                                errorMessage = "Ошибка сервера: " + error.networkResponse.statusCode;
                        }
                    } else if (error instanceof NetworkError ||
                            error instanceof NoConnectionError) {
                        errorMessage = "Проверте подключение к интернету";
                    } else if (error instanceof TimeoutError) {
                        errorMessage = "Превышено время ожидания ответа";
                    } else {
                        errorMessage = "Ошибка загрузки данных: " + error.getMessage();
                    }
                    showError(errorMessage);
                }
        );

        // Увеличиваем таймаут и количество попыток
        request.setRetryPolicy(new DefaultRetryPolicy(
                30000, // 30 секунд таймаут
                2,     // 2 попытки
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        requestQueue.add(request);
    }

    private WeatherData parseWeatherData(JSONObject response) throws JSONException {
        String cityName = response.getString("name");
        JSONObject main = response.getJSONObject("main");
        double temperature = main.getDouble("temp");
        int humidity = main.getInt("humidity");

        JSONObject wind = response.getJSONObject("wind");
        double windSpeed = wind.getDouble("speed");

        JSONObject weather = response.getJSONArray("weather").getJSONObject(0);
        String description = weather.getString("description");
        String iconCode = weather.getString("icon");

        return new WeatherData(cityName, temperature, humidity, windSpeed, description, iconCode);
    }

    private void updateWeatherList(WeatherData newData) {
        // Проверяем, нет ли уже такого города
        for (int i = 0; i < weatherDataList.size(); i++) {
            if (weatherDataList.get(i).cityName.equals(newData.cityName)) {
                weatherDataList.set(i, newData);
                weatherAdapter.notifyItemChanged(i);
                return;
            }
        }

        // Если город новый, добавляем его
        weatherDataList.add(newData);
        weatherAdapter.notifyItemInserted(weatherDataList.size() - 1);
    }

    private void showError(String message) {
        Log.e(TAG, "Error: " + message);
        runOnUiThread(() -> {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        });
    }

    private void onCityClick(WeatherData weatherData) {
        WeatherDetailDialog dialog = new WeatherDetailDialog(weatherData);
        dialog.show(getSupportFragmentManager(), "weather_detail");
    }

    private void toggleTheme() {
        isDarkTheme = !isDarkTheme;
        if (isDarkTheme) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    Constants.CHANNEL_ID,
                    Constants.CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(Constants.CHANNEL_DESCRIPTION);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            channel.setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
            );
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            channel.setBypassDnd(true);
            channel.enableLights(true);
            channel.setLightColor(Color.RED);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void requestNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {

                // Показываем объяснение, зачем нужны разрешения
                new AlertDialog.Builder(this)
                        .setTitle("Разрешение на уведомления")
                        .setMessage("Для получения уведомлений о погоде необходимо предоставить разрешение.")
                        .setPositiveButton("OK", (dialog, which) -> {
                            requestPermissions(
                                    new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                                    1
                            );
                        })
                        .setNegativeButton("Отмена", null)
                        .show();
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (!alarmManager.canScheduleExactAlarms()) {
                new AlertDialog.Builder(this)
                        .setTitle("Точные будильники")
                        .setMessage("Для корректной работы уведомлений необходимо разрешить использование точных будильников.")
                        .setPositiveButton("Настройки", (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                            startActivity(intent);
                        })
                        .setNegativeButton("Отмена", null)
                        .show();
            }
        }
    }

    private void setupSwipeRefresh() {
        SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            // Очищаем кеш при обновлении
            requestQueue.getCache().clear();
            clearAppCache();

            weatherDataList.clear();
            weatherAdapter.notifyDataSetChanged();
            loadDefaultCitiesParallel();
            swipeRefreshLayout.setRefreshing(false);
        });
    }

    private void clearAppCache() {
        try {
            // Очищаем кеш приложения
            File cacheDir = getCacheDir();
            File appDir = new File(cacheDir.getParent());
            if (appDir.exists()) {
                String[] children = appDir.list();
                for (String s : children) {
                    if (!s.equals("lib")) {
                        deleteDir(new File(appDir, s));
                    }
                }
            }
            // Очищаем внешний кеш если он есть
            if (getExternalCacheDir() != null) {
                deleteDir(getExternalCacheDir());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    boolean success = deleteDir(new File(dir, child));
                    if (!success) {
                        return false;
                    }
                }
            }
        }
        return dir != null && dir.delete();
    }
} 