package com.example.mywather;
 class DailyForecast {
    public double temperature;
    public String date;
    public String iconCode;
    public String description;

    private String cityName;

    public DailyForecast(String cityName, double temperature, String description) {
        this.cityName = cityName;
        this.temperature = temperature;
        this.description = description;
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    @Override
    public String toString() {
        return cityName + ": " + temperature + "°C, " + description;
    }
}