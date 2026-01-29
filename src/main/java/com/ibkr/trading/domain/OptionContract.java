package com.ibkr.trading.domain;

import com.ib.client.Contract;

import java.util.Objects;

/**
 * Domain object representing an option contract with a builder pattern.
 * Provides an immutable, fluent interface for creating option contracts.
 */
public class OptionContract {
    private final String symbol;
    private final String secType;
    private final String exchange;
    private final String currency;
    private final String expiration;
    private final double strike;
    private final String right;
    private final String multiplier;

    private OptionContract(Builder builder) {
        this.symbol = builder.symbol;
        this.secType = builder.secType;
        this.exchange = builder.exchange;
        this.currency = builder.currency;
        this.expiration = builder.expiration;
        this.strike = builder.strike;
        this.right = builder.right;
        this.multiplier = builder.multiplier;
    }

    public Contract toIBContract() {
        Contract contract = new Contract();
        contract.symbol(symbol);
        contract.secType(secType);
        contract.exchange(exchange);
        contract.currency(currency);
        contract.lastTradeDateOrContractMonth(expiration);
        contract.strike(strike);
        contract.right(right);
        contract.multiplier(multiplier);
        return contract;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String symbol = "";
        private String secType = "OPT";
        private String exchange = "SMART";
        private String currency = "USD";
        private String expiration;
        private double strike;
        private String right;
        private String multiplier = "100";

        public Builder symbol(String symbol) {
            this.symbol = symbol;
            return this;
        }

        public Builder expiration(String expiration) {
            this.expiration = expiration;
            return this;
        }

        public Builder strike(double strike) {
            this.strike = strike;
            return this;
        }

        public Builder call() {
            this.right = "C";
            return this;
        }

        public Builder put() {
            this.right = "P";
            return this;
        }

        public Builder right(String right) {
            this.right = right;
            return this;
        }

        public OptionContract build() {
            Objects.requireNonNull(expiration, "Expiration date is required");
            Objects.requireNonNull(right, "Option right (C/P) is required");
            if (strike <= 0) {
                throw new IllegalArgumentException("Strike must be positive");
            }
            return new OptionContract(this);
        }
    }

    @Override
    public String toString() {
        return String.format("%s %s %s %.2f %s", symbol, expiration, right, strike, secType);
    }
}
