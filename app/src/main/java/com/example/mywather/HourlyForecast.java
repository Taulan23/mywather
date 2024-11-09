package com.example.mywather;

public class HourlyForecast {
    public double temperature;
    public String time;
    public String iconCode;

    public HourlyForecast(double temperature, String time, String iconCode) {
        this.temperature = temperature;
        this.time = time;
        this.iconCode = iconCode;
    }
}