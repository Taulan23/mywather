package com.example.mywather;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import android.util.Log;

public class WeatherAdapter extends RecyclerView.Adapter<WeatherAdapter.WeatherViewHolder> {
    private static final String TAG = "WeatherAdapter";
    private List<WeatherData> weatherList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(WeatherData weatherData);
    }

    public WeatherAdapter(List<WeatherData> weatherList, OnItemClickListener listener) {
        this.weatherList = weatherList;
        this.listener = listener;
        Log.d(TAG, "Created adapter with " + weatherList.size() + " items");
    }

    @NonNull
    @Override
    public WeatherViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_weather, parent, false);
        Log.d(TAG, "Created new ViewHolder");
        return new WeatherViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WeatherViewHolder holder, int position) {
        WeatherData weatherData = weatherList.get(position);
        Log.d(TAG, "Binding data for city: " + weatherData.cityName);
        holder.bind(weatherData, listener);
    }

    @Override
    public int getItemCount() {
        Log.d(TAG, "Getting item count: " + weatherList.size());
        return weatherList.size();
    }

    static class WeatherViewHolder extends RecyclerView.ViewHolder {
        private final ImageView weatherIcon;
        private final TextView cityName;
        private final TextView temperature;
        private final TextView description;
        private final TextView humidity;
        private final TextView windSpeed;

        public WeatherViewHolder(@NonNull View itemView) {
            super(itemView);
            weatherIcon = itemView.findViewById(R.id.weatherIcon);
            cityName = itemView.findViewById(R.id.cityName);
            temperature = itemView.findViewById(R.id.temperature);
            description = itemView.findViewById(R.id.description);
            humidity = itemView.findViewById(R.id.humidity);
            windSpeed = itemView.findViewById(R.id.windSpeed);
        }

        public void bind(WeatherData weatherData, OnItemClickListener listener) {
            cityName.setText(weatherData.cityName);
            temperature.setText(String.format("%.1f°C", weatherData.temperature));
            description.setText(weatherData.description);
            humidity.setText(String.format("Влажность: %d%%", weatherData.humidity));
            windSpeed.setText(String.format("Ветер: %.1f м/с", weatherData.windSpeed));

            int iconResource = getWeatherIcon(weatherData.iconCode);
            weatherIcon.setImageResource(iconResource);

            itemView.setOnClickListener(v -> {
                Log.d(TAG, "Clicked on city: " + weatherData.cityName);
                listener.onItemClick(weatherData);
            });
        }

        private int getWeatherIcon(String iconCode) {
            switch (iconCode) {
                case "01d":
                case "01n":
                    return R.drawable.ic_sunny;
                case "02d":
                case "02n":
                case "03d":
                case "03n":
                case "04d":
                case "04n":
                    return R.drawable.ic_cloudy;
                case "09d":
                case "09n":
                case "10d":
                case "10n":
                    return R.drawable.ic_rainy;
                default:
                    return R.drawable.ic_sunny;
            }
        }
    }
}