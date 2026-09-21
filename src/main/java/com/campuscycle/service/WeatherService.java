package com.campuscycle.service;

import com.campuscycle.model.WeatherReport;
import javafx.concurrent.Task;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Networking Service demonstrating HTTP JSON API requests and parsing.
 * Fetches live weather conditions over HTTP and parses JSON to determine
 * bicycle safety advisories for students and staff.
 */
public class WeatherService {
    private static final Logger LOGGER = Logger.getLogger(WeatherService.class.getName());
    private static final String API_URL = "https://api.open-meteo.com/v1/forecast?latitude=23.8103&longitude=90.4125&current_weather=true";
    private final HttpClient httpClient;

    public WeatherService() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    }

    /**
     * Creates a JavaFX Task to fetch and parse weather JSON concurrently without freezing the UI.
     */
    public Task<WeatherReport> createFetchWeatherTask() {
        return new Task<>() {
            @Override
            protected WeatherReport call() throws Exception {
                updateMessage("Connecting to weather forecast service...");
                return fetchLiveWeather();
            }
        };
    }

    /**
     * Synchronous fetch method that makes HTTP request and parses JSON response.
     */
    public WeatherReport fetchLiveWeather() {
        try {
            LOGGER.info("Sending HTTP GET request to: " + API_URL);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Accept", "application/json")
                .header("User-Agent", "CampusCycle-JavaFX/1.0")
                .GET()
                .timeout(Duration.ofSeconds(8))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String jsonBody = response.body();
                LOGGER.info("HTTP 200 Received. Parsing JSON payload...");
                return parseWeatherJson(jsonBody);
            } else {
                LOGGER.warning("HTTP request failed with status: " + response.statusCode());
                return getFallbackWeather("HTTP " + response.statusCode() + " received");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Network error fetching weather, falling back to simulated campus sensor", e);
            return getFallbackWeather("Offline sensor fallback");
        }
    }

    /**
     * Demonstrates parsing JSON data using org.json.JSONObject.
     */
    public WeatherReport parseWeatherJson(String jsonString) {
        JSONObject root = new JSONObject(jsonString);
        JSONObject current = root.getJSONObject("current_weather");

        double temperature = current.getDouble("temperature");
        double windSpeed = current.getDouble("windspeed");
        int weatherCode = current.getInt("weathercode");
        boolean isDay = current.optInt("is_day", 1) == 1;

        String condition = decodeWeatherCode(weatherCode);
        String safetyLevel;
        String recommendation;

        if (weatherCode >= 51 && weatherCode <= 67) {
            safetyLevel = "Rainy: Exercise Caution";
            recommendation = "Roads might be slippery. Use fenders and reduce downhill speed.";
        } else if (weatherCode >= 71) {
            safetyLevel = "Severe Weather Alert";
            recommendation = "Adverse weather conditions. Indoor campus shuttle advised.";
        } else if (windSpeed > 25.0) {
            safetyLevel = "High Wind: Ride Steadily";
            recommendation = "Strong campus wind gusts. Keep both hands on handlebars.";
        } else if (temperature > 35.0) {
            safetyLevel = "High Heat Advisory";
            recommendation = "Stay hydrated and utilize shaded bike paths.";
        } else {
            safetyLevel = "Optimal Cycling Conditions";
            recommendation = "Great weather for an eco-friendly campus ride!";
        }

        String timeNow = LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm a, MMM dd"));

        return new WeatherReport(
            temperature,
            windSpeed,
            weatherCode,
            condition,
            isDay,
            safetyLevel,
            recommendation,
            timeNow
        );
    }

    private String decodeWeatherCode(int code) {
        switch (code) {
            case 0: return "Clear Sky";
            case 1: return "Mainly Clear";
            case 2: return "Partly Cloudy";
            case 3: return "Overcast";
            case 45: case 48: return "Foggy";
            case 51: case 53: case 55: return "Light Drizzle";
            case 61: case 63: case 65: return "Rain Showers";
            case 80: case 81: case 82: return "Heavy Showers";
            case 95: return "Thunderstorm";
            default: return "Mild Conditions";
        }
    }

    private WeatherReport getFallbackWeather(String note) {
        String timeNow = LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm a, MMM dd"));
        return new WeatherReport(
            26.5,
            12.0,
            1,
            "Pleasant Campus Breeze",
            true,
            "Good Cycling Conditions (" + note + ")",
            "Perfect temperature for commuting between campus faculties.",
            timeNow
        );
    }
}
