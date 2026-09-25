package com.campuscycle.model;

import com.campuscycle.service.oop.Identifiable;

/**
 * Double-entry ledger record for Campus Pay wallet operations.
 */
public class WalletTransaction implements Identifiable {
    public enum TransactionType {
        DEPOSIT("Wallet Deposit / Top-up"),
        RENTAL_CHARGE("Cycle Rental Payment"),
        OVERDUE_FINE("Overtime Penalty Charge"),
        REFUND("Refund / Adjustment");

        private final String label;
        TransactionType(String label) {
            this.label = label;
        }
        public String getLabel() {
            return label;
        }
    }

    private int id;
    private int userId;
    private double amount;
    private TransactionType type;
    private double balanceAfter;
    private String timestamp;
    private String description;
    private String referenceCode;

    public WalletTransaction(int id, int userId, double amount, TransactionType type,
                             double balanceAfter, String timestamp, String description, String referenceCode) {
        this.id = id;
        this.userId = userId;
        this.amount = amount;
        this.type = type;
        this.balanceAfter = balanceAfter;
        this.timestamp = timestamp;
        this.description = description;
        this.referenceCode = referenceCode;
    }

    @Override
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public double getAmount() {
        return amount;
    }

    public TransactionType getType() {
        return type;
    }

    public double getBalanceAfter() {
        return balanceAfter;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getDescription() {
        return description;
    }

    public String getReferenceCode() {
        return referenceCode;
    }
}
