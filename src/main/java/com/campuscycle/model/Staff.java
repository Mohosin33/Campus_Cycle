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

    public Staff(int id, String username, String password, String fullName, String email, String phone,
                 String employeeId, String department) {
        super(id, username, password, fullName, email, phone, UserRole.STAFF);
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
        return true;
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
