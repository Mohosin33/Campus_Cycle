package com.campuscycle.service.oop;

import com.campuscycle.model.User;

/**
 * Interface defining behaviors of any item that can be rented within CampusCycle.
 * Demonstrates Interface implementation in Advanced OOP.
 */
public interface Rentable {
    boolean isAvailable();
    void rentOut(User user, int durationHours);
    void returnToStation(int stationId);
    double calculateCost(int durationHours, PricingStrategy strategy);
    String getDisplayName();
}
