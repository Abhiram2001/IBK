package com.ibkr.trading.domain;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Represents a Strangle strategy.
 * Involves selling both call and put options at different strikes,
 * profiting when the underlying stays within a range.
 */
public class StrangleStrategy extends SpreadStrategy {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    
    private final String symbol;
    private final LocalDate expiry;
    private final double callStrikeOffset;
    private final double putStrikeOffset;

    private StrangleStrategy(Builder builder) {
        super(builder.spotPrice, builder.quantity);
        this.symbol = builder.symbol;
        this.expiry = builder.expiry;
        this.callStrikeOffset = builder.callStrikeOffset;
        this.putStrikeOffset = builder.putStrikeOffset;
    }

    @Override
    public String getStrategyName() {
        return "Strangle";
    }

    public OptionContract getCallContract() {
        return OptionContract.builder()
                .symbol(symbol)
                .strike(roundToNearestStrike(spotPrice + callStrikeOffset))
                .expiration(expiry.format(DATE_FORMAT))
                .call()
                .build();
    }

    public OptionContract getPutContract() {
        return OptionContract.builder()
                .symbol(symbol)
                .strike(roundToNearestStrike(spotPrice - putStrikeOffset))
                .expiration(expiry.format(DATE_FORMAT))
                .put()
                .build();
    }
    
    /**
     * Validates that the strangle has different strikes for call and put.
     * A strangle requires different strikes to avoid a "riskless" combination.
     */
    public void validate() {
        double callStrike = roundToNearestStrike(spotPrice + callStrikeOffset);
        double putStrike = roundToNearestStrike(spotPrice - putStrikeOffset);
        
        if (Math.abs(callStrike - putStrike) < 0.01) {
            throw new IllegalArgumentException(
                String.format("Invalid strangle: Call strike ($%.2f) and Put strike ($%.2f) are the same. " +
                    "A strangle requires different strike prices. Adjust offsets or spot price.",
                    callStrike, putStrike));
        }
        
        if (callStrike <= putStrike) {
            throw new IllegalArgumentException(
                String.format("Invalid strangle: Call strike ($%.2f) must be higher than Put strike ($%.2f). " +
                    "Verify spot price ($%.2f) and offsets (Call: +$%.2f, Put: -$%.2f).",
                    callStrike, putStrike, spotPrice, callStrikeOffset, putStrikeOffset));
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String symbol = "";
        private double spotPrice;
        private int quantity = 1;
        private LocalDate expiry;
        private double callStrikeOffset = 5;
        private double putStrikeOffset = 5;

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

        public Builder expiry(LocalDate date) {
            this.expiry = date;
            return this;
        }

        public Builder callStrikeOffset(double offset) {
            this.callStrikeOffset = Math.max(0, offset);
            return this;
        }

        public Builder putStrikeOffset(double offset) {
            this.putStrikeOffset = Math.max(0, offset);
            return this;
        }

        public StrangleStrategy build() {
            if (expiry == null) {
                throw new IllegalArgumentException("Expiry date is required");
            }
            return new StrangleStrategy(this);
        }
    }
}
