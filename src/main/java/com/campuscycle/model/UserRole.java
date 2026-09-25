package com.campuscycle.model;

/**
 * Enumeration representing the three user roles in CampusCycle.
 * ADMIN   - Platform administrator
 * OWNER   - Cycle owner who lists cycles for rent
 * RIDER   - User who rents and rides cycles
 */
public enum UserRole {
    RIDER("Rider"),
    OWNER("Cycle Owner"),
    ADMIN("Administrator");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
