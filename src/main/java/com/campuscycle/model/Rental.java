package com.campuscycle.model;

import com.campuscycle.service.oop.Identifiable;

/**
 * Model representing a rental transaction / booking record.
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
    private int startStationId;
    private int endStationId;
    private String notes;

    public Rental(int id, int userId, String userName, int cycleId, String cycleName,
                  String startTime, String endTime, int durationHours, double totalCost,
                  RentalStatus status, int startStationId, int endStationId, String notes) {
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
        this.startStationId = startStationId;
        this.endStationId = endStationId;
        this.notes = notes;
    }

    public Rental(int userId, int cycleId, String startTime, int durationHours, double totalCost, int startStationId) {
        this(0, userId, "", cycleId, "", startTime, null, durationHours, totalCost, RentalStatus.ACTIVE, startStationId, 0, "Campus ride");
    }

    @Override
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public int getCycleId() {
        return cycleId;
    }

    public void setCycleId(int cycleId) {
        this.cycleId = cycleId;
    }

    public String getCycleName() {
        return cycleName;
    }

    public void setCycleName(String cycleName) {
        this.cycleName = cycleName;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public int getDurationHours() {
        return durationHours;
    }

    public void setDurationHours(int durationHours) {
        this.durationHours = durationHours;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }

    public RentalStatus getStatus() {
        return status;
    }

    public void setStatus(RentalStatus status) {
        this.status = status;
    }

    public int getStartStationId() {
        return startStationId;
    }

    public void setStartStationId(int startStationId) {
        this.startStationId = startStationId;
    }

    public int getEndStationId() {
        return endStationId;
    }

    public void setEndStationId(int endStationId) {
        this.endStationId = endStationId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isActive() {
        return status == RentalStatus.ACTIVE;
    }
}
