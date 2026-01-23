package apidemo.stategies;

import com.ib.client.Contract;
import com.ib.client.TickAttrib;
import com.ib.client.TickType;
import com.ib.controller.ApiController;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class PriceMonitor {
    
    public interface PriceAlertListener {
        void onPriceAlert(MonitoredOrder order, double currentPrice, double distance);
        void onPriceUpdate(MonitoredOrder order, double currentPrice);
    }
    
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
        
        public double getDistanceToTarget() {
            if (currentPrice == 0.0) return Double.MAX_VALUE;
            return Math.abs(targetPrice - currentPrice);
        }
        
        public double getDistancePercentage() {
            if (currentPrice == 0.0 || targetPrice == 0.0) return 100.0;
            return (Math.abs(targetPrice - currentPrice) / targetPrice) * 100.0;
        }
        
        public boolean shouldAlert() {
            return !alertTriggered && getDistanceToTarget() <= alertThreshold;
        }
        
        @Override
        public String toString() {
            return String.format("%s %s @ %.2f (Current: %.2f, Target: %.2f)", 
                action, contract.symbol(), contract.strike(), currentPrice, targetPrice);
        }
    }
    
    private final ApiController controller;
    private final Map<String, MonitoredOrder> monitoredOrders;
    private final Map<String, ApiController.TopMktDataAdapter> dataListeners;
    private final List<PriceAlertListener> alertListeners;
    
    public PriceMonitor(ApiController controller) {
        this.controller = controller;
        this.monitoredOrders = new ConcurrentHashMap<>();
        this.dataListeners = new ConcurrentHashMap<>();
        this.alertListeners = new ArrayList<>();
    }
    
    public void addAlertListener(PriceAlertListener listener) {
        alertListeners.add(listener);
    }
    
    public void removeAlertListener(PriceAlertListener listener) {
        alertListeners.remove(listener);
    }
    
    public String startMonitoring(Contract contract, double targetPrice, 
                                  double alertThreshold, String action) {
        String orderId = UUID.randomUUID().toString().substring(0, 8);
        MonitoredOrder order = new MonitoredOrder(orderId, contract, targetPrice, alertThreshold, action);
        monitoredOrders.put(orderId, order);
        
        ApiController.TopMktDataAdapter listener = new ApiController.TopMktDataAdapter() {
            @Override
            public void tickPrice(TickType tickType, double price, TickAttrib attribs) {
                if (tickType == TickType.LAST || tickType == TickType.DELAYED_LAST || 
                    tickType == TickType.BID || tickType == TickType.ASK || tickType == TickType.CLOSE) {
                    
                    if (price > 0) {
                        order.currentPrice = price;
                        order.lastUpdateTime = System.currentTimeMillis();
                        
                        notifyPriceUpdate(order, price);
                        
                        if (order.shouldAlert()) {
                            order.alertTriggered = true;
                            notifyAlert(order, price, order.getDistanceToTarget());
                        }
                    }
                }
            }
        };
        
        dataListeners.put(orderId, listener);
        controller.reqTopMktData(contract, "", false, false, listener);
        
        return orderId;
    }
    
    public void stopMonitoring(String orderId) {
        ApiController.TopMktDataAdapter listener = dataListeners.remove(orderId);
        if (listener != null) {
            controller.cancelTopMktData(listener);
        }
        monitoredOrders.remove(orderId);
    }
    
    public void stopAllMonitoring() {
        List<String> orderIds = new ArrayList<>(monitoredOrders.keySet());
        for (String orderId : orderIds) {
            stopMonitoring(orderId);
        }
    }
    
    public MonitoredOrder getOrder(String orderId) {
        return monitoredOrders.get(orderId);
    }
    
    public Collection<MonitoredOrder> getAllOrders() {
        return new ArrayList<>(monitoredOrders.values());
    }
    
    public void resetAlert(String orderId) {
        MonitoredOrder order = monitoredOrders.get(orderId);
        if (order != null) {
            order.alertTriggered = false;
        }
    }
    
    private void notifyAlert(MonitoredOrder order, double currentPrice, double distance) {
        SwingUtilities.invokeLater(() -> {
            for (PriceAlertListener listener : alertListeners) {
                listener.onPriceAlert(order, currentPrice, distance);
            }
        });
    }
    
    private void notifyPriceUpdate(MonitoredOrder order, double currentPrice) {
        SwingUtilities.invokeLater(() -> {
            for (PriceAlertListener listener : alertListeners) {
                listener.onPriceUpdate(order, currentPrice);
            }
        });
    }
    
    public static Contract createContractFromParsedOrder(OrderParser.ParsedOrder parsed) {
        Contract contract = new Contract();
        contract.symbol(parsed.symbol);
        contract.secType("OPT");
        contract.exchange("SMART");
        contract.currency("USD");
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String expiryDate = parsed.expiryDate.format(formatter);
        contract.lastTradeDateOrContractMonth(expiryDate);
        
        contract.strike(parsed.strikePrice);
        contract.right(parsed.optionType.equals("CALL") ? "C" : "P");
        contract.multiplier("100");
        
        return contract;
    }
}
