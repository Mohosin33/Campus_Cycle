package com.campuscycle.model;

/**
 * Cycle availability and operational status.
 */
public enum CycleStatus {
    AVAILABLE("Available"),
    RENTED("Rented"),
    MAINTENANCE("In Maintenance");

    private final String statusText;

    CycleStatus(String statusText) {
        this.statusText = statusText;
    }

    public String getStatusText() {
        return statusText;
    }

    @Override
    public String toString() {
        return statusText;
    }
}
