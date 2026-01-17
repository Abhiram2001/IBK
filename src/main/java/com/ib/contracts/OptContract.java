/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.contracts;

import com.ib.client.Contract;
import com.ib.client.Types.SecType;

/**
 * Convenience class for creating option contracts.
 * Extends the base Contract class with option-specific parameters.
 * Defaults to SMART routing and USD currency.
 * 
 * @author Interactive Brokers
 * @version 1.0
 */
public class OptContract extends Contract {
    /**
     * Creates an option contract with SMART routing.
     * 
     * @param symbol the underlying symbol (e.g., "SPY", "AAPL")
     * @param lastTradeDateOrContractMonth expiration date in yyyyMMdd or yyyyMM format
     * @param strike strike price
     * @param right option right - "C" for call, "P" for put
     */
    public OptContract(String symbol, String lastTradeDateOrContractMonth, double strike, String right) {
        this(symbol, "SMART", lastTradeDateOrContractMonth, strike, right);
    }

    /**
     * Creates an option contract with specified exchange.
     * 
     * @param symbol the underlying symbol (e.g., "SPY", "AAPL")
     * @param exchange the exchange to route to (e.g., "SMART", "CBOE")
     * @param lastTradeDateOrContractMonth expiration date in yyyyMMdd or yyyyMM format
     * @param strike strike price
     * @param right option right - "C" for call, "P" for put
     */
    public OptContract(String symbol, String exchange, String lastTradeDateOrContractMonth, double strike, String right) {
        symbol(symbol);
        secType(SecType.OPT.name());
        exchange(exchange);
        currency("USD");
        lastTradeDateOrContractMonth(lastTradeDateOrContractMonth);
        strike(strike);
        right(right);
    }
}
