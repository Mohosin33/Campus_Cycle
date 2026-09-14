package com.campuscycle.model;

/**
 * Enumeration representing user roles within CampusCycle.
 */
public enum UserRole {
    STUDENT("Student"),
    STAFF("Staff / Faculty"),
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
