package com.campuscycle.model;

import com.campuscycle.service.oop.Identifiable;

/**
 * Model representing a payment transaction record.
 */
public class Payment implements Identifiable {
    private int id;
    private int rentalId;
    private double amount;
    private PaymentMethod method;
    private PaymentStatus status;
    private String transactionDate;
    private String transactionReference;

    public Payment(int id, int rentalId, double amount, PaymentMethod method,
                   PaymentStatus status, String transactionDate, String transactionReference) {
        this.id = id;
        this.rentalId = rentalId;
        this.amount = amount;
        this.method = method;
        this.status = status;
        this.transactionDate = transactionDate;
        this.transactionReference = transactionReference;
    }

    @Override
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getRentalId() {
        return rentalId;
    }

    public void setRentalId(int rentalId) {
        this.rentalId = rentalId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public void setMethod(PaymentMethod method) {
        this.method = method;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(String transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }
}
