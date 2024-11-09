package com.example.mywather;

public class Constants {
    public static final String API_KEY = "779029a4d6d000a98cb6f6922e0a04ba";
    public static final String BASE_URL = "https://api.openweathermap.org/data/2.5/";

    // Константы для уведомлений
    public static final String CHANNEL_ID = "weather_notifications";
    public static final String CHANNEL_NAME = "Weather Updates";
    public static final String CHANNEL_DESCRIPTION = "Weather forecast notifications";
    public static final int NOTIFICATION_ID = 1;

    // Интервал обновления уведомлений (в миллисекундах)
    public static final long NOTIFICATION_INTERVAL = 3600000; // 1 час
}