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

    /**
     * Places a combo/BAG order for spread strategies.
     * 
     * @param symbol the underlying symbol
     * @param legs the combo legs with individual actions
     * @param limitPrice the net credit/debit limit price
     * @param quantity number of spreads to trade
     * @param comboAction BUY or SELL
     * @param statusCallback callback for status updates
     */
    public void placeComboOrder(String symbol, List<ComboLeg> legs, double limitPrice, int quantity, 
                                String comboAction, Consumer<String> statusCallback) {
        if (legs == null || legs.isEmpty()) {
            statusCallback.accept("Error: No legs provided for combo order");
            return;
        }
        
        // Log order details for audit trail
        statusCallback.accept(String.format("Placing combo order: %s BAG, %d legs, Limit=$%.2f, Qty=%d", 
            symbol, legs.size(), limitPrice, quantity));
        
        Contract comboContract = new Contract();
        comboContract.symbol(symbol);
        comboContract.secType("BAG");
        comboContract.currency("USD");
        comboContract.exchange("SMART");
        comboContract.comboLegs(new ArrayList<>(legs));

        Order order = new Order();
        order.orderType("LMT");
        order.lmtPrice(limitPrice);
        order.action(comboAction);
        order.totalQuantity(Decimal.get(quantity));
        order.tif("GTC");

        controller.placeOrModifyOrder(comboContract, order, new ApiController.IOrderHandler() {
            @Override
            public void orderState(OrderState orderState, Order order) {
                String status = orderState.getStatus();
                statusCallback.accept("Order Status: " + status);
                
                // Log important state changes
                if ("Submitted".equals(status) || "PreSubmitted".equals(status)) {
                    statusCallback.accept("✓ Order successfully submitted to TWS");
                } else if ("Cancelled".equals(status)) {
                    statusCallback.accept("✗ Order was cancelled");
                } else if ("Filled".equals(status)) {
                    statusCallback.accept("✓ Order completely filled");
                }
            }

            @Override
            public void orderStatus(OrderStatus status, Decimal filled, Decimal remaining,
                                  double avgFillPrice, int permId, int parentId,
                                  double lastFillPrice, int clientId, String whyHeld,
                                  double mktCapPrice) {
                if (filled.longValue() > 0) {
                    statusCallback.accept(String.format("Filled: %s @ $%.2f (Remaining: %s)", 
                        filled, avgFillPrice, remaining));
                }
            }

            @Override
            public void handle(int errorCode, String errorMsg) {
                // Provide detailed error information
                statusCallback.accept(String.format("Error %d: %s", errorCode, errorMsg));
                
                // Common error codes and their meanings
                if (errorCode == 201) {
                    statusCallback.accept("→ Order rejected: Check contract details and strike prices");
                } else if (errorCode == 202) {
                    statusCallback.accept("→ Order cancelled");
                } else if (errorCode == 10147) {
                    statusCallback.accept("→ Market data permission required for this contract");
                }
            }
        });
    }
    
    /**
     * Determines if this is a credit spread (SELL) or debit spread (BUY).
     * 
     * For credit spreads (you receive money):
     * - All legs are SELL → Net credit → Use SELL combo
     * - Example: Short strangle, short straddle
     * 
     * For debit spreads (you pay money):
     * - All legs are BUY → Net debit → Use BUY combo
     * - Example: Long strangle, long straddle
     * 
     * For mixed spreads (calendar, diagonal):
     * - More SELL than BUY → Net credit → Use SELL combo
     * - More BUY than SELL → Net debit → Use BUY combo
     */
    private String determineCreditOrDebit(List<ComboLeg> legs) {
        if (legs.isEmpty()) {
            return "BUY";
        }
        
        int sellCount = 0;
        int buyCount = 0;
        
        for (ComboLeg leg : legs) {
            if ("SELL".equals(leg.action())) {
                sellCount++;
            } else if ("BUY".equals(leg.action())) {
                buyCount++;
            }
        }
        
        // If more SELL legs, it's a credit spread (you receive money) → SELL combo
        // If more BUY legs, it's a debit spread (you pay money) → BUY combo
        return (sellCount > buyCount) ? "SELL" : "BUY";
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
