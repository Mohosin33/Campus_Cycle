package com.campuscycle.model;

import com.campuscycle.service.oop.PricingStrategy;
import com.campuscycle.service.oop.StudentPricingStrategy;

/**
 * Concrete subclass representing a Student user.
 * Demonstrates inheritance and polymorphism.
 */
public class Student extends User {
    private String studentId;
    private String department;
    private int loyaltyPoints;
    private static final PricingStrategy PRICING_STRATEGY = new StudentPricingStrategy();

    public Student(int id, String username, String password, String fullName, String email, String phone,
                   String studentId, String department, int loyaltyPoints) {
        super(id, username, password, fullName, email, phone, UserRole.STUDENT);
        this.studentId = studentId;
        this.department = department;
        this.loyaltyPoints = loyaltyPoints;
    }

    public Student(int id, String username, String password, String fullName, String email, String phone,
                   String studentId, String department) {
        this(id, username, password, fullName, email, phone, studentId, department, 10);
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
        return true;
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
