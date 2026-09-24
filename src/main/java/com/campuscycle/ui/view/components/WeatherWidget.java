package com.campuscycle.ui.view.components;

import com.campuscycle.model.WeatherReport;
import com.campuscycle.service.ThreadPoolManager;
import com.campuscycle.service.WeatherService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * UI Component demonstrating HTTP JSON Networking & Concurrency.
 * Fetches and displays live campus cycling weather advisories asynchronously.
 */
public class WeatherWidget extends HBox {
    private final WeatherService weatherService;
    private final Label tempLabel;
    private final Label conditionLabel;
    private final Label advisoryLabel;
    private final ProgressIndicator spinner;
    private final Button refreshBtn;

    public WeatherWidget() {
        this.weatherService = new WeatherService();

        setAlignment(Pos.CENTER_LEFT);
        setSpacing(12);
        setPadding(new Insets(8, 14, 8, 14));
        getStyleClass().add("weather-card");

        // Temperature & Condition
        tempLabel = new Label("⛅ --°C");
        tempLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: white;");

        conditionLabel = new Label("Fetching live weather...");
        conditionLabel.getStyleClass().add("weather-subtext");

        VBox tempBox = new VBox(2, tempLabel, conditionLabel);

        // Safety Advisory
        advisoryLabel = new Label("Safe Riding Advisory: Loading...");
        advisoryLabel.setStyle("-fx-text-fill: #fef08a; -fx-font-weight: 600; -fx-font-size: 12px;");

        spinner = new ProgressIndicator();
        spinner.setPrefSize(20, 20);
        spinner.setVisible(true);

        refreshBtn = new Button("↻");
        refreshBtn.setStyle("-fx-background-color: rgba(255,255,255,0.25); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 15; -fx-padding: 2 8 2 8; -fx-cursor: hand;");
        refreshBtn.setTooltip(new Tooltip("Refresh live weather from Open-Meteo JSON API"));
        refreshBtn.setOnAction(e -> refreshWeather());

        getChildren().addAll(tempBox, advisoryLabel, spinner, refreshBtn);

        // Initial fetch on background thread
        refreshWeather();
    }

    public void refreshWeather() {
        spinner.setVisible(true);
        refreshBtn.setDisable(true);
        conditionLabel.setText("Querying Open-Meteo HTTP API...");

        // Run Task on ThreadPool to satisfy Concurrency & Networking requirements
        Task<WeatherReport> weatherTask = weatherService.createFetchWeatherTask();

        weatherTask.setOnSucceeded(e -> {
            WeatherReport report = weatherTask.getValue();
            Platform.runLater(() -> {
                spinner.setVisible(false);
                refreshBtn.setDisable(false);
                if (report != null) {
                    tempLabel.setText(String.format("🌡 %.1f°C | 💨 %.1f km/h", report.getTemperature(), report.getWindSpeed()));
                    conditionLabel.setText(report.getConditionDescription() + " • Updated " + report.getFetchTime());
                    advisoryLabel.setText("🚲 " + report.getSafetyLevel() + " — " + report.getRecommendation());
                }
            });
        });

        weatherTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                spinner.setVisible(false);
                refreshBtn.setDisable(false);
                conditionLabel.setText("Weather data temporarily unavailable");
            });
        });

        ThreadPoolManager.getInstance().execute(weatherTask);
    }
}
