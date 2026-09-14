package com.campuscycle.model;

import com.campuscycle.service.oop.Identifiable;
import com.campuscycle.service.oop.PricingStrategy;

/**
 * Abstract base class representing a user in the CampusCycle system.
 * Demonstrates Abstraction and Inheritance (Advanced OOP Concept).
 */
public abstract class User implements Identifiable {
    protected int id;
    protected String username;
    protected String password;
    protected String fullName;
    protected String email;
    protected String phone;
    protected UserRole role;

    public User(int id, String username, String password, String fullName, String email, String phone, UserRole role) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.role = role;
    }

    // Abstract methods to be overridden by subclasses (Polymorphism)
    public abstract String getDashboardTitle();
    public abstract PricingStrategy getPricingStrategy();
    public abstract boolean canManageCycles();
    public abstract boolean canRentCycles();
    public abstract String getRoleSpecificId();
    public abstract String getDepartment();

    @Override
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    @Override
    public String toString() {
        return fullName + " (" + role.getDisplayName() + ")";
    }
}
