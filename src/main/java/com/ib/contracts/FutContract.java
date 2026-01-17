/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.contracts;

import com.ib.client.Contract;
import com.ib.client.Types.SecType;

/**
 * Convenience class for creating futures contracts.
 * Extends the base Contract class with futures-specific parameters.
 * Provides constructors for EUREX and custom currency futures.
 * 
 * @author Interactive Brokers
 * @version 1.0
 */
public class FutContract extends Contract {
    /**
     * Creates a futures contract on EUREX exchange with EUR currency.
     * 
     * @param symbol the futures symbol (e.g., "FESX", "FDAX")
     * @param lastTradeDateOrContractMonth expiration date in yyyyMMdd or yyyyMM format
     */
    public FutContract(String symbol, String lastTradeDateOrContractMonth) {
        symbol(symbol);
        secType(SecType.FUT);
        exchange("EUREX");
        currency("EUR");
        lastTradeDateOrContractMonth(lastTradeDateOrContractMonth);
    }

    /**
     * Creates a futures contract with specified currency.
     * Exchange routing is not specified and must be set separately.
     * 
     * @param symbol the futures symbol
     * @param lastTradeDateOrContractMonth expiration date in yyyyMMdd or yyyyMM format
     * @param currency the currency code (e.g., "USD", "EUR")
     */
    public FutContract(String symbol, String lastTradeDateOrContractMonth, String currency) {
        symbol(symbol);
        secType(SecType.FUT.name());
        currency(currency);
        lastTradeDateOrContractMonth(lastTradeDateOrContractMonth);
    }
}
