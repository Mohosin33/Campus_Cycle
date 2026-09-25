package com.campuscycle.model;

import com.campuscycle.service.oop.Identifiable;
import com.campuscycle.service.oop.PricingStrategy;

/**
 * Production Abstract base class representing a user in the CampusCycle system.
 * Features salted password hashing and prepaid campus wallet balance.
 */
public abstract class User implements Identifiable {
    protected int id;
    protected String username;
    protected String passwordHash;
    protected String passwordSalt;
    protected String fullName;
    protected String email;
    protected String phone;
    protected UserRole role;
    protected double walletBalance;
    protected boolean isActive;
    protected String createdAt;

    public User(int id, String username, String passwordHash, String passwordSalt,
                String fullName, String email, String phone, UserRole role,
                double walletBalance, boolean isActive, String createdAt) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.passwordSalt = passwordSalt;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.walletBalance = walletBalance;
        this.isActive = isActive;
        this.createdAt = createdAt;
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

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getPasswordSalt() {
        return passwordSalt;
    }

    public void setPasswordSalt(String passwordSalt) {
        this.passwordSalt = passwordSalt;
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

    public double getWalletBalance() {
        return walletBalance;
    }

    public void setWalletBalance(double walletBalance) {
        this.walletBalance = walletBalance;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return fullName + " (" + role.getDisplayName() + ")";
    }
}
