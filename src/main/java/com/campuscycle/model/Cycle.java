package com.campuscycle.model;

import com.campuscycle.service.oop.Identifiable;
import com.campuscycle.service.oop.PricingStrategy;
import com.campuscycle.service.oop.Rentable;

/**
 * Model representing a bicycle or e-bike in the fleet.
 * Implements Rentable and Identifiable interfaces (Advanced OOP Concept).
 */
public class Cycle implements Rentable, Identifiable {
    private int id;
    private String model;
    private String brand;
    private CycleType type;
    private double hourlyRate;
    private CycleStatus status;
    private int stationId;
    private String stationName;
    private int batteryPercentage; // 0-100 for Electric, -1 if manual
    private int totalRides;
    private String lastMaintainedDate;
    private int ownerId;       // FK → users.id where role = OWNER
    private String ownerName;  // denormalized for display

    public Cycle(int id, String model, String brand, CycleType type, double hourlyRate,
                 CycleStatus status, int stationId, String stationName, int batteryPercentage,
                 int totalRides, String lastMaintainedDate, int ownerId, String ownerName) {
        this.id = id;
        this.model = model;
        this.brand = brand;
        this.type = type;
        this.hourlyRate = hourlyRate;
        this.status = status;
        this.stationId = stationId;
        this.stationName = stationName;
        this.batteryPercentage = batteryPercentage;
        this.totalRides = totalRides;
        this.lastMaintainedDate = lastMaintainedDate;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
    }

    /** Convenience constructor (without owner — for anonymous/admin cycles) */
    public Cycle(int id, String model, String brand, CycleType type, double hourlyRate,
                 CycleStatus status, int stationId, String stationName, int batteryPercentage,
                 int totalRides, String lastMaintainedDate) {
        this(id, model, brand, type, hourlyRate, status, stationId, stationName,
             batteryPercentage, totalRides, lastMaintainedDate, 0, "");
    }

    /** Short constructor used in forms/tests */
    public Cycle(int id, String model, String brand, CycleType type, double hourlyRate,
                 CycleStatus status, int stationId, int batteryPercentage) {
        this(id, model, brand, type, hourlyRate, status, stationId, "Station " + stationId,
             batteryPercentage, 0, "2026-09-01", 0, "");
    }

    /** Owner-linked constructor */
    public Cycle(int id, String model, String brand, CycleType type, double hourlyRate,
                 CycleStatus status, int stationId, int batteryPercentage, int ownerId) {
        this(id, model, brand, type, hourlyRate, status, stationId, "Station " + stationId,
             batteryPercentage, 0, "2026-09-01", ownerId, "");
    }


    // Implementing Rentable interface methods
    @Override
    public boolean isAvailable() {
        return this.status == CycleStatus.AVAILABLE;
    }

    @Override
    public void rentOut(User user, int durationHours) {
        this.status = CycleStatus.RENTED;
        this.totalRides++;
    }

    @Override
    public void returnToStation(int newStationId) {
        this.status = CycleStatus.AVAILABLE;
        this.stationId = newStationId;
    }

    @Override
    public double calculateCost(int durationHours, PricingStrategy strategy) {
        if (strategy != null) {
            return strategy.calculateFinalPrice(this, durationHours);
        }
        return hourlyRate * durationHours;
    }

    @Override
    public String getDisplayName() {
        return brand + " " + model + " (" + type.getLabel() + ")";
    }

    // Getters and Setters
    @Override
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public CycleType getType() {
        return type;
    }

    public void setType(CycleType type) {
        this.type = type;
    }

    public double getHourlyRate() {
        return hourlyRate;
    }

    public void setHourlyRate(double hourlyRate) {
        this.hourlyRate = hourlyRate;
    }

    public CycleStatus getStatus() {
        return status;
    }

    public void setStatus(CycleStatus status) {
        this.status = status;
    }

    public int getStationId() {
        return stationId;
    }

    public void setStationId(int stationId) {
        this.stationId = stationId;
    }

    public String getStationName() { return stationName; }
    public void setStationName(String stationName) { this.stationName = stationName; }
    public int getBatteryPercentage() { return batteryPercentage; }
    public void setBatteryPercentage(int batteryPercentage) { this.batteryPercentage = batteryPercentage; }
    public int getTotalRides() { return totalRides; }
    public void setTotalRides(int totalRides) { this.totalRides = totalRides; }
    public String getLastMaintainedDate() { return lastMaintainedDate; }
    public void setLastMaintainedDate(String lastMaintainedDate) { this.lastMaintainedDate = lastMaintainedDate; }

    public int getOwnerId() { return ownerId; }
    public void setOwnerId(int ownerId) { this.ownerId = ownerId; }
    public String getOwnerName() { return ownerName != null ? ownerName : ""; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    @Override
    public String toString() {
        return getDisplayName() + " [#" + id + "] - " + status;
    }
}
