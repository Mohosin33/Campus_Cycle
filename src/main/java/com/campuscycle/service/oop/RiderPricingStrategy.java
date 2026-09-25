package com.campuscycle.service.oop;

import com.campuscycle.model.Cycle;

/**
 * Concrete pricing strategy for Campus Riders (20% Campus Mobility Discount).
 * Demonstrates Strategy Pattern and Polymorphism.
 */
public class RiderPricingStrategy implements PricingStrategy {
    private static final double DISCOUNT_RATE = 0.20; // 20% discount

    @Override
    public double calculateFinalPrice(Cycle cycle, int hours) {
        if (cycle == null || hours <= 0) return 0.0;
        double baseCost = cycle.getHourlyRate() * hours;
        double discountedCost = baseCost * (1.0 - DISCOUNT_RATE);
        return Math.round(discountedCost * 100.0) / 100.0;
    }

    @Override
    public String getStrategyName() {
        return "Campus Rider Discount Rate (20% OFF)";
    }

    @Override
    public double getDiscountPercentage() {
        return 20.0;
    }
}
