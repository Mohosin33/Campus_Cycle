package com.campuscycle.service.oop;

import com.campuscycle.model.Cycle;

/**
 * Strategy interface for calculating rental costs dynamically.
 * Part of the Strategy Design Pattern.
 */
public interface PricingStrategy {
    double calculateFinalPrice(Cycle cycle, int hours);
    String getStrategyName();
    double getDiscountPercentage();
}
