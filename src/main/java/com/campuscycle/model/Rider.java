package com.campuscycle.model;

import com.campuscycle.service.oop.PricingStrategy;
import com.campuscycle.service.oop.RiderPricingStrategy;

/**
 * Concrete subclass representing a Rider — someone who rents and uses cycles.
 * Riders earn loyalty points on each completed rental.
 */
public class Rider extends User {
    private String riderId;
    private String department;
    private int loyaltyPoints;
    private static final PricingStrategy PRICING_STRATEGY = new RiderPricingStrategy();

    public Rider(int id, String username, String passwordHash, String passwordSalt,
                 String fullName, String email, String phone,
                 String riderId, String department, int loyaltyPoints,
                 double walletBalance, boolean isActive, String createdAt) {
        super(id, username, passwordHash, passwordSalt, fullName, email, phone,
              UserRole.RIDER, walletBalance, isActive, createdAt);
        this.riderId = riderId;
        this.department = department;
        this.loyaltyPoints = loyaltyPoints;
    }

    @Override
    public String getDashboardTitle() {
        return "Rider Mobility Portal";
    }

    @Override
    public PricingStrategy getPricingStrategy() {
        return PRICING_STRATEGY;
    }

    @Override
    public boolean canManageCycles() {
        return false;
    }

    @Override
    public boolean canRentCycles() {
        return isActive;
    }

    @Override
    public String getRoleSpecificId() {
        return riderId;
    }

    @Override
    public String getDepartment() {
        return department;
    }

    public String getRiderId() { return riderId; }
    public void setRiderId(String riderId) { this.riderId = riderId; }
    public void setDepartment(String department) { this.department = department; }
    public int getLoyaltyPoints() { return loyaltyPoints; }
    public void setLoyaltyPoints(int loyaltyPoints) { this.loyaltyPoints = loyaltyPoints; }
}
