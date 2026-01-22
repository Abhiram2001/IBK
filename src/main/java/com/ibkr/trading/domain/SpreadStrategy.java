package com.ibkr.trading.domain;

/**
 * Base class for spread trading strategies.
 * Encapsulates common strategy parameters and behavior.
 */
public abstract class SpreadStrategy {
    protected final double spotPrice;
    protected final int quantity;
    
    protected SpreadStrategy(double spotPrice, int quantity) {
        if (spotPrice <= 0) {
            throw new IllegalArgumentException("Spot price must be positive");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        this.spotPrice = spotPrice;
        this.quantity = quantity;
    }

    public double getSpotPrice() {
        return spotPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public abstract String getStrategyName();
    
    protected double roundToNearestStrike(double price) {
        return Math.round(price);
    }
}
