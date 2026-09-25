package com.campuscycle.model;

import com.campuscycle.service.oop.PricingStrategy;
import com.campuscycle.service.oop.StandardPricingStrategy;

/**
 * Concrete subclass representing System Administrators.
 */
public class Admin extends User {
    private String adminBadgeId;
    private String department;
    private static final PricingStrategy PRICING_STRATEGY = new StandardPricingStrategy();

    public Admin(int id, String username, String passwordHash, String passwordSalt,
                 String fullName, String email, String phone,
                 String adminBadgeId, String department,
                 double walletBalance, boolean isActive, String createdAt) {
        super(id, username, passwordHash, passwordSalt, fullName, email, phone,
              UserRole.ADMIN, walletBalance, isActive, createdAt);
        this.adminBadgeId = adminBadgeId;
        this.department = department != null ? department : "Campus Logistics";
    }

    @Override
    public String getDashboardTitle() {
        return "CampusCycle Fleet & Operations Management Console";
    }

    @Override
    public PricingStrategy getPricingStrategy() {
        return PRICING_STRATEGY;
    }

    @Override
    public boolean canManageCycles() {
        return true;
    }

    @Override
    public boolean canRentCycles() {
        return true;
    }

    @Override
    public String getRoleSpecificId() {
        return adminBadgeId;
    }

    @Override
    public String getDepartment() {
        return department;
    }

    public String getAdminBadgeId() {
        return adminBadgeId;
    }

    public void setAdminBadgeId(String adminBadgeId) {
        this.adminBadgeId = adminBadgeId;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}
