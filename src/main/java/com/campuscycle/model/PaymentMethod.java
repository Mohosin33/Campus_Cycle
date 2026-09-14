package com.campuscycle.model;

/**
 * Payment methods accepted by the CampusCycle system.
 */
public enum PaymentMethod {
    CAMPUS_CARD("University Campus Card"),
    CREDIT_CARD("Credit / Debit Card"),
    BKASH("bKash / Mobile Wallet"),
    CASH("Cash at Return Desk");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
