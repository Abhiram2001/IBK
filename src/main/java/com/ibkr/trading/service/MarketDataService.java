package com.ibkr.trading.service;

import com.ib.client.Contract;
import com.ib.client.TickAttrib;
import com.ib.client.TickType;
import com.ib.controller.ApiController;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Service for fetching market data from IB.
 * Provides functional interface for price queries.
 */
public class MarketDataService {
    private final ApiController controller;

    public MarketDataService(ApiController controller) {
        this.controller = controller;
    }

    public CompletableFuture<Double> getStockPrice(String symbol) {
        CompletableFuture<Double> future = new CompletableFuture<>();
        
        Contract contract = createStockContract(symbol);
        
        ApiController.TopMktDataAdapter listener = new ApiController.TopMktDataAdapter() {
            @Override
            public void tickPrice(TickType tickType, double price, TickAttrib attribs) {
                if (isValidPriceTick(tickType) && price > 0) {
                    future.complete(price);
                    controller.cancelTopMktData(this);
                }
            }
        };

        controller.reqTopMktData(contract, "", false, false, listener);
        return future;
    }

    public void subscribeToPrice(String symbol, Consumer<Double> priceConsumer) {
        Contract contract = createStockContract(symbol);
        
        ApiController.TopMktDataAdapter listener = new ApiController.TopMktDataAdapter() {
            @Override
            public void tickPrice(TickType tickType, double price, TickAttrib attribs) {
                if (isValidPriceTick(tickType) && price > 0) {
                    priceConsumer.accept(price);
                }
            }
        };

        controller.reqTopMktData(contract, "", false, false, listener);
    }

    private Contract createStockContract(String symbol) {
        Contract contract = new Contract();
        contract.symbol(symbol);
        contract.secType("STK");
        contract.exchange("SMART");
        contract.currency("USD");
        return contract;
    }

    private boolean isValidPriceTick(TickType tickType) {
        return tickType == TickType.LAST || 
               tickType == TickType.DELAYED_LAST || 
               tickType == TickType.CLOSE;
    }
}
