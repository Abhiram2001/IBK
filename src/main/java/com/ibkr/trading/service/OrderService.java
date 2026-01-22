package com.ibkr.trading.service;

import com.ib.client.*;
import com.ib.controller.ApiController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Service for placing and managing orders with IB.
 * Provides functional interface for order operations.
 */
public class OrderService {
    private final ApiController controller;

    public OrderService(ApiController controller) {
        this.controller = controller;
    }

    public CompletableFuture<List<ContractDetails>> getContractDetails(Contract contract) {
        CompletableFuture<List<ContractDetails>> future = new CompletableFuture<>();
        
        controller.reqContractDetails(contract, detailsList -> {
            if (detailsList.isEmpty()) {
                future.completeExceptionally(
                    new IllegalStateException("No contract details found")
                );
            } else {
                future.complete(detailsList);
            }
        });
        
        return future;
    }

    public void placeComboOrder(List<ComboLeg> legs, double limitPrice, int quantity, 
                                Consumer<String> statusCallback) {
        Contract comboContract = new Contract();
        comboContract.symbol("SPY");
        comboContract.secType("BAG");
        comboContract.currency("USD");
        comboContract.exchange("SMART");
        comboContract.comboLegs(new ArrayList<>(legs));

        Order order = new Order();
        order.orderType("LMT");
        order.lmtPrice(limitPrice);
        order.action("BUY");
        order.totalQuantity(Decimal.get(quantity));
        order.tif("GTC");

        controller.placeOrModifyOrder(comboContract, order, new ApiController.IOrderHandler() {
            @Override
            public void orderState(OrderState orderState, Order order) {
                statusCallback.accept("Order Status: " + orderState.getStatus());
            }

            @Override
            public void orderStatus(OrderStatus status, Decimal filled, Decimal remaining,
                                  double avgFillPrice, int permId, int parentId,
                                  double lastFillPrice, int clientId, String whyHeld,
                                  double mktCapPrice) {
                statusCallback.accept("Order " + status + " - Filled: " + filled + ", Remaining: " + remaining);
            }

            @Override
            public void handle(int errorCode, String errorMsg) {
                statusCallback.accept("Error: " + errorCode + " - " + errorMsg);
            }
        });
    }

    public void placeSimpleOrder(Contract contract, String action, int quantity, 
                                 double limitPrice, Consumer<String> statusCallback) {
        Order order = new Order();
        order.orderType("LMT");
        order.lmtPrice(limitPrice);
        order.action(action);
        order.totalQuantity(Decimal.get(quantity));
        order.tif("GTC");

        controller.placeOrModifyOrder(contract, order, new ApiController.IOrderHandler() {
            @Override
            public void orderState(OrderState orderState, Order order) {
                statusCallback.accept("Order Status: " + orderState.getStatus());
            }

            @Override
            public void orderStatus(OrderStatus status, Decimal filled, Decimal remaining,
                                  double avgFillPrice, int permId, int parentId,
                                  double lastFillPrice, int clientId, String whyHeld,
                                  double mktCapPrice) {
                statusCallback.accept("Status: " + status);
            }

            @Override
            public void handle(int errorCode, String errorMsg) {
                statusCallback.accept("Error: " + errorCode + " - " + errorMsg);
            }
        });
    }

    /**
     * Places a multi-stock order (individual stock order for batch strategies).
     * 
     * @param symbol the stock symbol
     * @param quantity number of shares
     * @param limitPrice limit price per share
     * @param isSell true for SELL, false for BUY
     * @return CompletableFuture that completes with true on success
     */
    public CompletableFuture<Boolean> placeMultiStockOrder(String symbol, int quantity, 
                                                           double limitPrice, boolean isSell) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        Contract contract = new Contract();
        contract.symbol(symbol);
        contract.secType("STK");
        contract.exchange("SMART");
        contract.currency("USD");
        
        Order order = new Order();
        order.orderType("LMT");
        order.lmtPrice(limitPrice);
        order.action(isSell ? "SELL" : "BUY");
        order.totalQuantity(Decimal.get(quantity));
        order.tif("GTC");
        
        controller.placeOrModifyOrder(contract, order, new ApiController.IOrderHandler() {
            @Override
            public void orderState(OrderState orderState, Order order) {
                if ("Submitted".equals(orderState.getStatus()) || 
                    "PreSubmitted".equals(orderState.getStatus())) {
                    future.complete(true);
                }
            }
            
            @Override
            public void orderStatus(OrderStatus status, Decimal filled, Decimal remaining,
                                  double avgFillPrice, int permId, int parentId,
                                  double lastFillPrice, int clientId, String whyHeld,
                                  double mktCapPrice) {
                // Status updates handled by orderState
            }
            
            @Override
            public void handle(int errorCode, String errorMsg) {
                if (errorCode != 0) {
                    future.completeExceptionally(new RuntimeException(errorMsg));
                }
            }
        });
        
        return future;
    }
}
