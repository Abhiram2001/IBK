package com.ibkr.trading.service;

import com.ib.client.Contract;
import com.ib.client.TickAttrib;
import com.ib.client.TickType;
import com.ib.controller.ApiController;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Service for monitoring option prices and triggering alerts when target prices are reached.
 * Supports multiple concurrent price monitors with independent alert thresholds.
 * Thread-safe implementation using concurrent collections.
 * 
 * @author IBK Trading System
 * @version 2.0
 */
public class PriceMonitorService {
    
    /**
     * Listener interface for price alerts and updates.
     */
    public interface PriceAlertListener {
        /**
         * Called when price reaches within alert threshold of target.
         * 
         * @param order the monitored order
         * @param currentPrice current market price
         * @param distanceToTarget distance from current price to target
         */
        void onPriceAlert(MonitoredOrder order, double currentPrice, double distanceToTarget);
        
        /**
         * Called on every price update for the monitored contract.
         * 
         * @param order the monitored order
         * @param currentPrice updated market price
         */
        void onPriceUpdate(MonitoredOrder order, double currentPrice);
    }
    
    /**
     * Represents an order being monitored for price targets.
     */
    public static class MonitoredOrder {
        public final String id;
        public final Contract contract;
        public final double targetPrice;
        public final double alertThreshold;
        public final String action;
        public double currentPrice = 0.0;
        public boolean alertTriggered = false;
        public long lastUpdateTime = 0;
        
        public MonitoredOrder(String id, Contract contract, double targetPrice, 
                            double alertThreshold, String action) {
            this.id = id;
            this.contract = contract;
            this.targetPrice = targetPrice;
            this.alertThreshold = alertThreshold;
            this.action = action;
        }
        
        /**
         * Calculates absolute distance from current price to target price.
         */
        public double getDistanceToTarget() {
            if (currentPrice == 0.0) return Double.MAX_VALUE;
            return Math.abs(targetPrice - currentPrice);
        }
        
        /**
         * Calculates distance as percentage of target price.
         */
        public double getDistancePercentage() {
            if (currentPrice == 0.0 || targetPrice == 0.0) return 100.0;
            return (Math.abs(targetPrice - currentPrice) / targetPrice) * 100.0;
        }
        
        /**
         * Determines if alert should be triggered based on current distance.
         */
        public boolean shouldAlert() {
            return !alertTriggered && getDistanceToTarget() <= alertThreshold;
        }
        
        @Override
        public String toString() {
            return String.format("%s %s @ $%.2f (Current: $%.2f, Target: $%.2f)", 
                action, contract.symbol(), contract.strike(), currentPrice, targetPrice);
        }
    }
    
    private final ApiController controller;
    private final Map<String, MonitoredOrder> monitoredOrders;
    private final Map<String, ApiController.TopMktDataAdapter> dataListeners;
    private final List<PriceAlertListener> alertListeners;
    
    public PriceMonitorService(ApiController controller) {
        this.controller = controller;
        this.monitoredOrders = new ConcurrentHashMap<>();
        this.dataListeners = new ConcurrentHashMap<>();
        this.alertListeners = new ArrayList<>();
    }
    
    /**
     * Registers a listener to receive price alerts and updates.
     */
    public void addAlertListener(PriceAlertListener listener) {
        synchronized (alertListeners) {
            alertListeners.add(listener);
        }
    }
    
    /**
     * Removes a previously registered listener.
     */
    public void removeAlertListener(PriceAlertListener listener) {
        synchronized (alertListeners) {
            alertListeners.remove(listener);
        }
    }
    
    /**
     * Starts monitoring a contract for price alerts.
     * 
     * @param contract the option contract to monitor
     * @param targetPrice the desired price target
     * @param alertThreshold distance from target to trigger alert
     * @param action BUY or SELL action
     * @return unique monitoring ID
     */
    public String startMonitoring(Contract contract, double targetPrice, 
                                  double alertThreshold, String action) {
        String orderId = UUID.randomUUID().toString().substring(0, 8);
        MonitoredOrder order = new MonitoredOrder(orderId, contract, targetPrice, alertThreshold, action);
        monitoredOrders.put(orderId, order);
        
        ApiController.TopMktDataAdapter listener = new ApiController.TopMktDataAdapter() {
            @Override
            public void tickPrice(TickType tickType, double price, TickAttrib attribs) {
                if (isValidPriceTick(tickType) && price > 0) {
                    order.currentPrice = price;
                    order.lastUpdateTime = System.currentTimeMillis();
                    
                    notifyPriceUpdate(order, price);
                    
                    if (order.shouldAlert()) {
                        order.alertTriggered = true;
                        notifyAlert(order, price, order.getDistanceToTarget());
                    }
                }
            }
        };
        
        dataListeners.put(orderId, listener);
        controller.reqTopMktData(contract, "", false, false, listener);
        
        return orderId;
    }
    
    /**
     * Stops monitoring a specific order.
     */
    public void stopMonitoring(String orderId) {
        ApiController.TopMktDataAdapter listener = dataListeners.remove(orderId);
        if (listener != null) {
            controller.cancelTopMktData(listener);
        }
        monitoredOrders.remove(orderId);
    }
    
    /**
     * Stops all active monitoring.
     */
    public void stopAllMonitoring() {
        List<String> orderIds = new ArrayList<>(monitoredOrders.keySet());
        for (String orderId : orderIds) {
            stopMonitoring(orderId);
        }
    }
    
    /**
     * Gets a specific monitored order by ID.
     */
    public MonitoredOrder getOrder(String orderId) {
        return monitoredOrders.get(orderId);
    }
    
    /**
     * Gets all currently monitored orders.
     */
    public Collection<MonitoredOrder> getAllOrders() {
        return new ArrayList<>(monitoredOrders.values());
    }
    
    /**
     * Resets the alert status for an order, allowing it to trigger again.
     */
    public void resetAlert(String orderId) {
        MonitoredOrder order = monitoredOrders.get(orderId);
        if (order != null) {
            order.alertTriggered = false;
        }
    }
    
    private void notifyAlert(MonitoredOrder order, double currentPrice, double distance) {
        SwingUtilities.invokeLater(() -> {
            synchronized (alertListeners) {
                for (PriceAlertListener listener : alertListeners) {
                    try {
                        listener.onPriceAlert(order, currentPrice, distance);
                    } catch (Exception e) {
                        // Log but don't fail other listeners
                        System.err.println("Error in alert listener: " + e.getMessage());
                    }
                }
            }
        });
    }
    
    private void notifyPriceUpdate(MonitoredOrder order, double currentPrice) {
        SwingUtilities.invokeLater(() -> {
            synchronized (alertListeners) {
                for (PriceAlertListener listener : alertListeners) {
                    try {
                        listener.onPriceUpdate(order, currentPrice);
                    } catch (Exception e) {
                        // Log but don't fail other listeners
                        System.err.println("Error in update listener: " + e.getMessage());
                    }
                }
            }
        });
    }
    
    private boolean isValidPriceTick(TickType tickType) {
        return tickType == TickType.LAST || 
               tickType == TickType.DELAYED_LAST || 
               tickType == TickType.BID || 
               tickType == TickType.ASK || 
               tickType == TickType.CLOSE;
    }
}
