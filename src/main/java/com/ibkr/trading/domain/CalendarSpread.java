package com.ibkr.trading.domain;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Represents a Calendar Spread strategy.
 * Involves selling near-term options and buying longer-term options
 * at the same or different strikes to profit from time decay.
 */
public class CalendarSpread extends SpreadStrategy {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    
    private final String symbol;
    private final LocalDate nearExpiry;
    private final LocalDate farExpiry;
    private final double callSellOffset;
    private final double putSellOffset;
    private final double callBuyOffset;
    private final double putBuyOffset;
    private final boolean includeCall;
    private final boolean includePut;

    private CalendarSpread(Builder builder) {
        super(builder.spotPrice, builder.quantity);
        this.symbol = builder.symbol;
        this.nearExpiry = builder.nearExpiry;
        this.farExpiry = builder.farExpiry;
        this.callSellOffset = builder.callSellOffset;
        this.putSellOffset = builder.putSellOffset;
        this.callBuyOffset = builder.callBuyOffset;
        this.putBuyOffset = builder.putBuyOffset;
        this.includeCall = builder.includeCall;
        this.includePut = builder.includePut;
    }

    @Override
    public String getStrategyName() {
        return "Calendar Spread";
    }

    public OptionContract getSellCallContract() {
        if (!includeCall) return null;
        return OptionContract.builder()
                .symbol(symbol)
                .strike(roundToNearestStrike(spotPrice + callSellOffset))
                .expiration(nearExpiry.format(DATE_FORMAT))
                .call()
                .build();
    }

    public OptionContract getSellPutContract() {
        if (!includePut) return null;
        return OptionContract.builder()
                .symbol(symbol)
                .strike(roundToNearestStrike(spotPrice - putSellOffset))
                .expiration(nearExpiry.format(DATE_FORMAT))
                .put()
                .build();
    }

    public OptionContract getBuyCallContract() {
        if (!includeCall) return null;
        return OptionContract.builder()
                .symbol(symbol)
                .strike(roundToNearestStrike(spotPrice + callBuyOffset))
                .expiration(farExpiry.format(DATE_FORMAT))
                .call()
                .build();
    }

    public OptionContract getBuyPutContract() {
        if (!includePut) return null;
        return OptionContract.builder()
                .symbol(symbol)
                .strike(roundToNearestStrike(spotPrice - putBuyOffset))
                .expiration(farExpiry.format(DATE_FORMAT))
                .put()
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String symbol = "";
        private double spotPrice;
        private int quantity = 1;
        private LocalDate nearExpiry;
        private LocalDate farExpiry;
        private double callSellOffset = 5;
        private double putSellOffset = 5;
        private double callBuyOffset = 8;
        private double putBuyOffset = 8;
        private boolean includeCall = true;
        private boolean includePut = true;

        public Builder symbol(String symbol) {
            this.symbol = symbol;
            return this;
        }

        public Builder spotPrice(double spotPrice) {
            this.spotPrice = spotPrice;
            return this;
        }

        public Builder quantity(int quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder nearExpiry(LocalDate date) {
            this.nearExpiry = date;
            return this;
        }

        public Builder farExpiry(LocalDate date) {
            this.farExpiry = date;
            return this;
        }

        public Builder callSellOffset(double offset) {
            this.callSellOffset = Math.max(0, offset);
            return this;
        }

        public Builder putSellOffset(double offset) {
            this.putSellOffset = Math.max(0, offset);
            return this;
        }

        public Builder callBuyOffset(double offset) {
            this.callBuyOffset = Math.max(0, offset);
            return this;
        }

        public Builder putBuyOffset(double offset) {
            this.putBuyOffset = Math.max(0, offset);
            return this;
        }

        public Builder includeCall(boolean include) {
            this.includeCall = include;
            return this;
        }

        public Builder includePut(boolean include) {
            this.includePut = include;
            return this;
        }

        public CalendarSpread build() {
            if (nearExpiry == null || farExpiry == null) {
                throw new IllegalArgumentException("Both expiry dates are required");
            }
            if (!nearExpiry.isBefore(farExpiry)) {
                throw new IllegalArgumentException("Near expiry must be before far expiry");
            }
            return new CalendarSpread(this);
        }
    }
}
