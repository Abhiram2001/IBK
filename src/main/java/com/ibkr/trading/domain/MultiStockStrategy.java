package com.ibkr.trading.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Domain model representing a multi-stock trading strategy.
 * Allows simultaneous placement of multiple stock orders with individual configurations.
 * 
 * <p>This strategy does not extend SpreadStrategy as it handles multiple independent orders
 * rather than traditional spread trading.</p>
 * 
 * @author IBK Trading System
 * @version 2.0
 */
public class MultiStockStrategy {
    private final List<StockOrder> orders;
    
    /**
     * Represents a single stock order within the multi-stock strategy.
     */
    public static class StockOrder {
        private final String symbol;
        private final int quantity;
        private final double limitPrice;
        private final boolean isSell;
        
        private StockOrder(Builder builder) {
            this.symbol = builder.symbol;
            this.quantity = builder.quantity;
            this.limitPrice = builder.limitPrice;
            this.isSell = builder.isSell;
        }
        
        public String getSymbol() { return symbol; }
        public int getQuantity() { return quantity; }
        public double getLimitPrice() { return limitPrice; }
        public boolean isSell() { return isSell; }
        public String getAction() { return isSell ? "SELL" : "BUY"; }
        
        public static Builder builder() {
            return new Builder();
        }
        
        /**
         * Builder for StockOrder.
         */
        public static class Builder {
            private String symbol;
            private int quantity;
            private double limitPrice;
            private boolean isSell;
            
            public Builder symbol(String symbol) {
                this.symbol = symbol;
                return this;
            }
            
            public Builder quantity(int quantity) {
                this.quantity = quantity;
                return this;
            }
            
            public Builder limitPrice(double limitPrice) {
                this.limitPrice = limitPrice;
                return this;
            }
            
            public Builder isSell(boolean isSell) {
                this.isSell = isSell;
                return this;
            }
            
            public Builder isBuy(boolean isBuy) {
                this.isSell = !isBuy;
                return this;
            }
            
            public StockOrder build() {
                if (symbol == null || symbol.trim().isEmpty()) {
                    throw new IllegalArgumentException("Symbol cannot be empty");
                }
                if (quantity <= 0) {
                    throw new IllegalArgumentException("Quantity must be positive");
                }
                if (limitPrice <= 0) {
                    throw new IllegalArgumentException("Limit price must be positive");
                }
                return new StockOrder(this);
            }
        }
    }
    
    private MultiStockStrategy(Builder builder) {
        this.orders = new ArrayList<>(builder.orders);
    }
    
    public List<StockOrder> getOrders() {
        return new ArrayList<>(orders);
    }
    
    /**
     * Returns the strategy name.
     * 
     * @return the name of this strategy
     */
    public String getStrategyName() {
        return "Multi-Stock Strategy";
    }
    
    /**
     * Returns a description of the strategy.
     * 
     * @return description with order count
     */
    public String getDescription() {
        return String.format("Multi-stock strategy with %d orders", orders.size());
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder for MultiStockStrategy.
     */
    public static class Builder {
        private final List<StockOrder> orders = new ArrayList<>();
        
        public Builder addOrder(StockOrder order) {
            this.orders.add(order);
            return this;
        }
        
        public Builder addOrder(String symbol, int quantity, double limitPrice, boolean isSell) {
            StockOrder order = StockOrder.builder()
                .symbol(symbol)
                .quantity(quantity)
                .limitPrice(limitPrice)
                .isSell(isSell)
                .build();
            this.orders.add(order);
            return this;
        }
        
        public Builder clearOrders() {
            this.orders.clear();
            return this;
        }
        
        public MultiStockStrategy build() {
            if (orders.isEmpty()) {
                throw new IllegalArgumentException("At least one order is required");
            }
            return new MultiStockStrategy(this);
        }
    }
    
    /**
     * Creates a default multi-stock strategy with common tech stocks.
     * 
     * @return MultiStockStrategy with default values for AAPL, MSFT, GOOGL, AMZN
     */
    public static MultiStockStrategy createDefault() {
        return builder()
            .addOrder("AAPL", 100, 150.0, true)
            .addOrder("MSFT", 100, 350.0, true)
            .addOrder("GOOGL", 100, 140.0, true)
            .addOrder("AMZN", 100, 175.0, true)
            .build();
    }
}
