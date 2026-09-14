package com.campuscycle.model;

/**
 * Cycle category types available on campus.
 */
public enum CycleType {
    STANDARD("Standard City", 1.0),
    GEARED("Multi-Speed Geared", 1.25),
    ELECTRIC("E-Bike (Electric)", 1.75),
    MOUNTAIN("All-Terrain Mountain", 1.35);

    private final String label;
    private final double rateMultiplier;

    CycleType(String label, double rateMultiplier) {
        this.label = label;
        this.rateMultiplier = rateMultiplier;
    }

    public String getLabel() {
        return label;
    }

    public double getRateMultiplier() {
        return rateMultiplier;
    }

    @Override
    public String toString() {
        return label;
    }
}
