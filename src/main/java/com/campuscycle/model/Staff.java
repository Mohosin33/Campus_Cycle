package com.campuscycle.model;

import com.campuscycle.service.oop.PricingStrategy;
import com.campuscycle.service.oop.StaffPricingStrategy;

/**
 * Concrete subclass representing Faculty and Staff members.
 */
public class Staff extends User {
    private String employeeId;
    private String department;
    private static final PricingStrategy PRICING_STRATEGY = new StaffPricingStrategy();

    public Staff(int id, String username, String passwordHash, String passwordSalt,
                 String fullName, String email, String phone,
                 String employeeId, String department,
                 double walletBalance, boolean isActive, String createdAt) {
        super(id, username, passwordHash, passwordSalt, fullName, email, phone,
              UserRole.STAFF, walletBalance, isActive, createdAt);
        this.employeeId = employeeId;
        this.department = department;
    }

    @Override
    public String getDashboardTitle() {
        return "Staff & Faculty Mobility Portal";
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
        return employeeId;
    }

    @Override
    public String getDepartment() {
        return department;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}
