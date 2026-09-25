package com.campuscycle.model;

import com.campuscycle.service.oop.PricingStrategy;
import com.campuscycle.service.oop.StandardPricingStrategy;

/**
 * Concrete subclass representing a Cycle Owner.
 * Owners list their own cycles on the platform for Riders to rent.
 * They earn revenue from rentals of their cycles.
 */
public class Owner extends User {
    private String ownerId;         // e.g. NID or business ID
    private String department;      // area/location of operations
    private double totalEarnings;   // cumulative rental income
    private static final PricingStrategy PRICING_STRATEGY = new StandardPricingStrategy();

    public Owner(int id, String username, String passwordHash, String passwordSalt,
                 String fullName, String email, String phone,
                 String ownerId, String department,
                 double walletBalance, boolean isActive, String createdAt) {
        super(id, username, passwordHash, passwordSalt, fullName, email, phone,
              UserRole.OWNER, walletBalance, isActive, createdAt);
        this.ownerId = ownerId;
        this.department = department;
        this.totalEarnings = 0.0;
    }

    @Override
    public String getDashboardTitle() {
        return "Owner Fleet Management Portal";
    }

    @Override
    public PricingStrategy getPricingStrategy() {
        return PRICING_STRATEGY;
    }

    @Override
    public boolean canManageCycles() {
        return isActive; // Owners manage their own cycles
    }

    @Override
    public boolean canRentCycles() {
        return false; // Owners list cycles, they don't rent them
    }

    @Override
    public String getRoleSpecificId() {
        return ownerId;
    }

    @Override
    public String getDepartment() {
        return department;
    }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public void setDepartment(String department) { this.department = department; }
    public double getTotalEarnings() { return totalEarnings; }
    public void setTotalEarnings(double totalEarnings) { this.totalEarnings = totalEarnings; }
}
