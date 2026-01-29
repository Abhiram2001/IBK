package com.ibkr.trading.service;

import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.controller.ApiController;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service for validating trading symbols via IB API.
 * Checks if a symbol exists and is tradeable on the exchange.
 */
public class SymbolValidationService {
    private final ApiController controller;
    
    public SymbolValidationService(ApiController controller) {
        this.controller = controller;
    }
    
    /**
     * Validates if a stock symbol exists and is tradeable.
     * 
     * @param symbol the stock symbol to validate (e.g., "SPY", "AAPL")
     * @return true if the symbol is valid and tradeable, false otherwise
     */
    public boolean validateSymbol(String symbol) {
        try {
            Contract contract = new Contract();
            contract.symbol(symbol);
            contract.secType("STK");
            contract.currency("USD");
            contract.exchange("SMART");
            
            AtomicBoolean isValid = new AtomicBoolean(false);
            CountDownLatch latch = new CountDownLatch(1);
            
            controller.reqContractDetails(contract, new ApiController.IContractDetailsHandler() {
                @Override
                public void contractDetails(List<ContractDetails> list) {
                    isValid.set(!list.isEmpty());
                    latch.countDown();
                }
            });
            
            // Wait up to 5 seconds for response
            boolean completed = latch.await(5, TimeUnit.SECONDS);
            return completed && isValid.get();
            
        } catch (Exception e) {
            return false;
        }
    }
}
