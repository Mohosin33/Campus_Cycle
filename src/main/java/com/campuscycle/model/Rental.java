package com.campuscycle.model;

import com.campuscycle.service.oop.Identifiable;

/**
 * Model representing a rental transaction / booking record.
 * Supports dockless pickup and return locations.
 */
public class Rental implements Identifiable {
    private int id;
    private int userId;
    private String userName;
    private int cycleId;
    private String cycleName;
    private String startTime;
    private String endTime;
    private int durationHours;
    private double totalCost;
    private RentalStatus status;
    private String pickupLocation;
    private String returnLocation;
    private String notes;

    public Rental(int id, int userId, String userName, int cycleId, String cycleName,
                  String startTime, String endTime, int durationHours, double totalCost,
                  RentalStatus status, String pickupLocation, String returnLocation, String notes) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.cycleId = cycleId;
        this.cycleName = cycleName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationHours = durationHours;
        this.totalCost = totalCost;
        this.status = status;
        this.pickupLocation = pickupLocation != null ? pickupLocation : "Campus Core";
        this.returnLocation = returnLocation;
        this.notes = notes;
    }

    /** Compatibility constructor for legacy code/tests */
    public Rental(int id, int userId, String userName, int cycleId, String cycleName,
                  String startTime, String endTime, int durationHours, double totalCost,
                  RentalStatus status, int startStationId, int endStationId, String notes) {
        this(id, userId, userName, cycleId, cycleName, startTime, endTime, durationHours, totalCost,
             status, "Campus Location #" + startStationId,
             endStationId > 0 ? "Campus Location #" + endStationId : null, notes);
    }

    public Rental(int userId, int cycleId, String startTime, int durationHours, double totalCost, String pickupLocation) {
        this(0, userId, "", cycleId, "", startTime, null, durationHours, totalCost, RentalStatus.ACTIVE, pickupLocation, null, "Campus ride");
    }

    @Override
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public int getCycleId() { return cycleId; }
    public void setCycleId(int cycleId) { this.cycleId = cycleId; }

    public String getCycleName() { return cycleName; }
    public void setCycleName(String cycleName) { this.cycleName = cycleName; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public int getDurationHours() { return durationHours; }
    public void setDurationHours(int durationHours) { this.durationHours = durationHours; }

    public double getTotalCost() { return totalCost; }
    public void setTotalCost(double totalCost) { this.totalCost = totalCost; }

    public RentalStatus getStatus() { return status; }
    public void setStatus(RentalStatus status) { this.status = status; }

    public String getPickupLocation() { return pickupLocation; }
    public void setPickupLocation(String pickupLocation) { this.pickupLocation = pickupLocation; }

    public String getReturnLocation() { return returnLocation; }
    public void setReturnLocation(String returnLocation) { this.returnLocation = returnLocation; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    // Compatibility getters
    public int getStartStationId() { return 0; }
    public void setStartStationId(int id) {}
    public int getEndStationId() { return 0; }
    public void setEndStationId(int id) {}

    public boolean isActive() {
        return status == RentalStatus.ACTIVE;
    }
}
