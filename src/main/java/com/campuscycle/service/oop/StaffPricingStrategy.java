package com.campuscycle.service.oop;

import com.campuscycle.model.Cycle;

/**
 * Concrete pricing strategy for University Faculty and Staff (15% Campus Discount).
 */
public class StaffPricingStrategy implements PricingStrategy {
    private static final double DISCOUNT = 0.15;

    @Override
    public double calculateFinalPrice(Cycle cycle, int hours) {
        if (cycle == null || hours <= 0) return 0.0;
        double baseCost = cycle.getHourlyRate() * hours;
        double discounted = baseCost * (1.0 - DISCOUNT);
        return Math.round(discounted * 100.0) / 100.0;
    }

    @Override
    public String getStrategyName() {
        return "Staff / Faculty Rate (15% OFF)";
    }

    @Override
    public double getDiscountPercentage() {
        return 15.0;
    }
}
