package com.example.mywather;

public class WeatherData {
    public String cityName;
    public double temperature;
    public int humidity;
    public double windSpeed;
    public String description;
    public String iconCode;

    public WeatherData(String cityName, double temperature, int humidity,
                       double windSpeed, String description, String iconCode) {
        this.cityName = cityName;
        this.temperature = temperature;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
        this.description = description;
        this.iconCode = iconCode;
    }
}