package com.campuscycle.model;

import com.campuscycle.service.oop.Identifiable;
import com.campuscycle.service.oop.PricingStrategy;
import com.campuscycle.service.oop.Rentable;

/**
 * Model representing a bicycle or e-bike in the fleet.
 * Dockless / free-floating campus architecture with location tracking.
 * Implements Rentable and Identifiable interfaces (Advanced OOP Concept).
 */
public class Cycle implements Rentable, Identifiable {
    private int id;
    private String model;
    private String brand;
    private CycleType type;
    private double hourlyRate;
    private CycleStatus status;
    private String location;       // Dockless campus location (e.g. "Library Gate", "Engineering Complex")
    private int batteryPercentage; // 0-100 for Electric, -1 if manual
    private int totalRides;
    private String lastMaintainedDate;
    private int ownerId;           // FK → users.id where role = OWNER
    private String ownerName;      // denormalized for display

    public Cycle(int id, String model, String brand, CycleType type, double hourlyRate,
                 CycleStatus status, String location, int batteryPercentage,
                 int totalRides, String lastMaintainedDate, int ownerId, String ownerName) {
        this.id = id;
        this.model = model;
        this.brand = brand;
        this.type = type;
        this.hourlyRate = hourlyRate;
        this.status = status;
        this.location = (location != null && !location.trim().isEmpty()) ? location.trim() : "Campus Core";
        this.batteryPercentage = batteryPercentage;
        this.totalRides = totalRides;
        this.lastMaintainedDate = lastMaintainedDate;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
    }

    /** Convenience constructor with location */
    public Cycle(int id, String model, String brand, CycleType type, double hourlyRate,
                 CycleStatus status, String location, int batteryPercentage, int ownerId) {
        this(id, model, brand, type, hourlyRate, status, location, batteryPercentage, 0, "2026-09-01", ownerId, "");
    }

    /** Short constructor used in tests/creation */
    public Cycle(int id, String model, String brand, CycleType type, double hourlyRate,
                 CycleStatus status, String location, int batteryPercentage) {
        this(id, model, brand, type, hourlyRate, status, location, batteryPercentage, 0, "2026-09-01", 0, "");
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
    public void returnCycle() {
        this.status = CycleStatus.AVAILABLE;
    }

    @Override
    public double calculateCost(int durationHours, PricingStrategy strategy) {
        if (strategy != null) {
            return strategy.calculateFinalPrice(this, durationHours);
        }
        return this.hourlyRate * durationHours;
    }

    public String getDisplayName() {
        return brand + " " + model + " (" + type.getLabel() + ")";
    }

    // Getters and Setters
    @Override
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public CycleType getType() { return type; }
    public void setType(CycleType type) { this.type = type; }

    public double getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(double hourlyRate) { this.hourlyRate = hourlyRate; }

    public CycleStatus getStatus() { return status; }
    public void setStatus(CycleStatus status) { this.status = status; }

    public String getLocation() { return location != null ? location : "Campus Core"; }
    public void setLocation(String location) { this.location = location; }

    /** Backwards-compatibility alias for location */
    public String getStationName() { return getLocation(); }
    public int getStationId() { return 0; }

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
        return getDisplayName() + " [#" + id + "] @ " + getLocation() + " - " + status;
    }
}
