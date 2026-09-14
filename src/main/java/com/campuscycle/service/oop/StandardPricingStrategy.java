package com.campuscycle.service.oop;

import com.campuscycle.model.Cycle;

/**
 * Standard pricing strategy (regular base hourly price).
 */
public class StandardPricingStrategy implements PricingStrategy {
    @Override
    public double calculateFinalPrice(Cycle cycle, int hours) {
        if (cycle == null || hours <= 0) return 0.0;
        double baseCost = cycle.getHourlyRate() * hours;
        return Math.round(baseCost * 100.0) / 100.0;
    }

    @Override
    public String getStrategyName() {
        return "Standard Campus Rate";
    }

    @Override
    public double getDiscountPercentage() {
        return 0.0;
    }
}
