package com.campuscycle.service.oop;

import com.campuscycle.model.Cycle;

/**
 * Concrete pricing strategy for University Students (25% Campus Discount).
 */
public class StudentPricingStrategy implements PricingStrategy {
    private static final double DISCOUNT = 0.25;

    @Override
    public double calculateFinalPrice(Cycle cycle, int hours) {
        if (cycle == null || hours <= 0) return 0.0;
        double baseCost = cycle.getHourlyRate() * hours;
        double discounted = baseCost * (1.0 - DISCOUNT);
        return Math.round(discounted * 100.0) / 100.0;
    }

    @Override
    public String getStrategyName() {
        return "Student Discount Rate (25% OFF)";
    }

    @Override
    public double getDiscountPercentage() {
        return 25.0;
    }
}
