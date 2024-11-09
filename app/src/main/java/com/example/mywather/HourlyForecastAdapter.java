package com.example.mywather;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class HourlyForecastAdapter extends RecyclerView.Adapter<HourlyForecastAdapter.ForecastViewHolder> {
    private List<DailyForecast> forecasts;

    public HourlyForecastAdapter(List<DailyForecast> forecasts) {
        this.forecasts = forecasts;
    }

    @NonNull
    @Override
    public ForecastViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_daily_forecast, parent, false);
        return new ForecastViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ForecastViewHolder holder, int position) {
        DailyForecast forecast = forecasts.get(position);
        holder.bind(forecast);
    }

    @Override
    public int getItemCount() {
        return forecasts.size();
    }

    public void updateForecasts(List<DailyForecast> newForecasts) {
        this.forecasts.clear();
        this.forecasts.addAll(newForecasts);
        notifyDataSetChanged();
    }

    static class ForecastViewHolder extends RecyclerView.ViewHolder {
        private TextView dateText;
        private TextView temperatureText;
        private TextView descriptionText;
        private ImageView weatherIcon;

        public ForecastViewHolder(@NonNull View itemView) {
            super(itemView);
            dateText = itemView.findViewById(R.id.dateText);
            temperatureText = itemView.findViewById(R.id.temperatureText);
            descriptionText = itemView.findViewById(R.id.descriptionText);
            weatherIcon = itemView.findViewById(R.id.weatherIcon);
        }

        public void bind(DailyForecast forecast) {
            if (forecast.date != null) {
                dateText.setText(forecast.date);
            }
            temperatureText.setText(String.format("%.1f°C", forecast.temperature));
            descriptionText.setText(forecast.description);

            // Загрузка иконки погоды используя Glide
            if (forecast.iconCode != null) {
                String iconUrl = String.format("https://openweathermap.org/img/w/%s.png",
                        forecast.iconCode);
                Glide.with(itemView.getContext())
                        .load(iconUrl)
                        .into(weatherIcon);
            }
        }
    }
}