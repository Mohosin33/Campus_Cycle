package com.campuscycle.service.oop;

import com.campuscycle.model.Cycle;

/**
 * Concrete pricing strategy for Campus Riders.
 * Keeps the same rental logic without any promotional discount.
 */
public class RiderPricingStrategy implements PricingStrategy {
    private static final double DISCOUNT_RATE = 0.0;

    @Override
    public double calculateFinalPrice(Cycle cycle, int hours) {
        if (cycle == null || hours <= 0) return 0.0;
        double baseCost = cycle.getHourlyRate() * hours;
        double discountedCost = baseCost * (1.0 - DISCOUNT_RATE);
        return Math.round(discountedCost * 100.0) / 100.0;
    }

    @Override
    public String getStrategyName() {
        return "Campus Rider Rate";
    }

    @Override
    public double getDiscountPercentage() {
        return 0.0;
    }
}
