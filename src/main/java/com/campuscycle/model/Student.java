package com.campuscycle.model;

import com.campuscycle.service.oop.PricingStrategy;
import com.campuscycle.service.oop.StudentPricingStrategy;

/**
 * Concrete subclass representing a Student user.
 */
public class Student extends User {
    private String studentId;
    private String department;
    private int loyaltyPoints;
    private static final PricingStrategy PRICING_STRATEGY = new StudentPricingStrategy();

    public Student(int id, String username, String passwordHash, String passwordSalt,
                   String fullName, String email, String phone,
                   String studentId, String department, int loyaltyPoints,
                   double walletBalance, boolean isActive, String createdAt) {
        super(id, username, passwordHash, passwordSalt, fullName, email, phone,
              UserRole.STUDENT, walletBalance, isActive, createdAt);
        this.studentId = studentId;
        this.department = department;
        this.loyaltyPoints = loyaltyPoints;
    }

    @Override
    public String getDashboardTitle() {
        return "Student Mobility Hub";
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
        return studentId;
    }

    @Override
    public String getDepartment() {
        return department;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public int getLoyaltyPoints() {
        return loyaltyPoints;
    }

    public void setLoyaltyPoints(int loyaltyPoints) {
        this.loyaltyPoints = loyaltyPoints;
    }
}
