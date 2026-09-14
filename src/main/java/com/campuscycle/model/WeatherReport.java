package com.campuscycle.model;

/**
 * Model representing live campus weather data parsed from external JSON HTTP API.
 * Used for rider safety recommendations.
 */
public class WeatherReport {
    private final double temperature;
    private final double windSpeed;
    private final int weatherCode;
    private final String conditionDescription;
    private final boolean isDay;
    private final String safetyLevel;
    private final String recommendation;
    private final String fetchTime;

    public WeatherReport(double temperature, double windSpeed, int weatherCode,
                         String conditionDescription, boolean isDay,
                         String safetyLevel, String recommendation, String fetchTime) {
        this.temperature = temperature;
        this.windSpeed = windSpeed;
        this.weatherCode = weatherCode;
        this.conditionDescription = conditionDescription;
        this.isDay = isDay;
        this.safetyLevel = safetyLevel;
        this.recommendation = recommendation;
        this.fetchTime = fetchTime;
    }

    public double getTemperature() {
        return temperature;
    }

    public double getWindSpeed() {
        return windSpeed;
    }

    public int getWeatherCode() {
        return weatherCode;
    }

    public String getConditionDescription() {
        return conditionDescription;
    }

    public boolean isDay() {
        return isDay;
    }

    public String getSafetyLevel() {
        return safetyLevel;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public String getFetchTime() {
        return fetchTime;
    }

    @Override
    public String toString() {
        return String.format("%.1f°C | %s | Wind: %.1f km/h (%s)", temperature, conditionDescription, windSpeed, safetyLevel);
    }
}
