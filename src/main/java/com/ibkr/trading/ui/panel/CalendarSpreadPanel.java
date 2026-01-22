package com.ibkr.trading.ui.panel;

import apidemo.util.HtmlButton;
import apidemo.util.UpperField;
import apidemo.util.VerticalPanel;
import com.ibkr.trading.domain.CalendarSpread;
import com.ibkr.trading.domain.OptionContract;
import com.ibkr.trading.service.*;
import com.ib.client.ComboLeg;
import com.ib.client.ContractDetails;
import com.toedter.calendar.JDateChooser;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Panel for executing Calendar Spread strategies using functional approach.
 */
public class CalendarSpreadPanel extends JPanel {
    private final MarketDataService marketDataService;
    private final OrderService orderService;
    
    private final JDateChooser nearExpiryChooser;
    private final JDateChooser farExpiryChooser;
    private final UpperField spotPriceField = new UpperField();
    private final UpperField callSellOffsetField = new UpperField();
    private final UpperField putSellOffsetField = new UpperField();
    private final UpperField callBuyOffsetField = new UpperField();
    private final UpperField putBuyOffsetField = new UpperField();
    private final UpperField quantityField = new UpperField();
    private final JCheckBox includeCallCheckbox = new JCheckBox("Include Call", true);
    private final JCheckBox includePutCheckbox = new JCheckBox("Include Put", true);
    private final JLabel statusLabel = new JLabel();
    
    private CalendarSpread currentStrategy;

    public CalendarSpreadPanel(ConnectionService connectionService) {
        this.marketDataService = new MarketDataService(connectionService.getController());
        this.orderService = new OrderService(connectionService.getController());
        this.nearExpiryChooser = UIComponents.createDateChooser();
        this.farExpiryChooser = UIComponents.createDateChooser();
        
        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());

        JLabel titleLabel = new JLabel("Calendar Spread Strategy");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        VerticalPanel inputPanel = createInputPanel();
        VerticalPanel buttonPanel = createButtonPanel();

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(inputPanel);
        mainPanel.add(buttonPanel);
        mainPanel.add(statusLabel);

        add(mainPanel, BorderLayout.CENTER);
    }

    private VerticalPanel createInputPanel() {
        VerticalPanel panel = new VerticalPanel();
        panel.add("Near Expiry", nearExpiryChooser);
        panel.add("Far Expiry", farExpiryChooser);
        panel.add("Spot Price", spotPriceField);
        panel.add("Quantity", quantityField);
        panel.add("Call Sell Offset", callSellOffsetField);
        panel.add("Call Buy Offset", callBuyOffsetField);
        panel.add("Put Sell Offset", putSellOffsetField);
        panel.add("Put Buy Offset", putBuyOffsetField);
        
        JPanel checkboxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        checkboxPanel.add(includeCallCheckbox);
        checkboxPanel.add(includePutCheckbox);
        panel.add("Options", checkboxPanel);
        
        return panel;
    }

    private VerticalPanel createButtonPanel() {
        VerticalPanel panel = new VerticalPanel();
        panel.add(createFetchPriceButton());
        panel.add(createValidateButton());
        panel.add(createPlaceOrderButton());
        return panel;
    }

    private HtmlButton createFetchPriceButton() {
        return new HtmlButton("Fetch SPY Price") {
            @Override
            protected void actionPerformed() {
                statusLabel.setText("Fetching price...");
                marketDataService.getStockPrice("SPY")
                    .thenAccept(price -> SwingUtilities.invokeLater(() -> {
                        spotPriceField.setText(String.format("%.2f", price));
                        populateDefaults();
                        statusLabel.setText("Price fetched: $" + String.format("%.2f", price));
                    }))
                    .exceptionally(ex -> {
                        SwingUtilities.invokeLater(() -> 
                            statusLabel.setText("Error: " + ex.getMessage()));
                        return null;
                    });
            }
        };
    }

    private HtmlButton createValidateButton() {
        return new HtmlButton("Validate Contracts") {
            @Override
            protected void actionPerformed() {
                try {
                    currentStrategy = buildStrategy();
                    statusLabel.setText("Validating contracts...");
                    validateContracts();
                } catch (Exception e) {
                    statusLabel.setText("Error: " + e.getMessage());
                }
            }
        };
    }

    private HtmlButton createPlaceOrderButton() {
        return new HtmlButton("Place Order") {
            @Override
            protected void actionPerformed() {
                if (currentStrategy == null) {
                    statusLabel.setText("Please validate contracts first");
                    return;
                }
                placeOrder();
            }
        };
    }

    private void populateDefaults() {
        LocalDate today = LocalDate.now();
        nearExpiryChooser.setDate(java.sql.Date.valueOf(today.plusDays(7)));
        farExpiryChooser.setDate(java.sql.Date.valueOf(today.plusDays(14)));
        quantityField.setText("1");
        callSellOffsetField.setText("5");
        putSellOffsetField.setText("5");
        callBuyOffsetField.setText("8");
        putBuyOffsetField.setText("8");
    }

    private CalendarSpread buildStrategy() {
        return CalendarSpread.builder()
            .spotPrice(spotPriceField.getDouble())
            .quantity(quantityField.getInt())
            .nearExpiry(toLocalDate(nearExpiryChooser.getDate()))
            .farExpiry(toLocalDate(farExpiryChooser.getDate()))
            .callSellOffset(callSellOffsetField.getDouble())
            .callBuyOffset(callBuyOffsetField.getDouble())
            .putSellOffset(putSellOffsetField.getDouble())
            .putBuyOffset(putBuyOffsetField.getDouble())
            .includeCall(includeCallCheckbox.isSelected())
            .includePut(includePutCheckbox.isSelected())
            .build();
    }

    private void validateContracts() {
        List<CompletableFuture<ContractDetails>> futures = new ArrayList<>();
        
        if (currentStrategy.getSellCallContract() != null) {
            futures.add(orderService.getContractDetails(
                currentStrategy.getSellCallContract().toIBContract())
                .thenApply(list -> list.get(0)));
        }
        if (currentStrategy.getBuyCallContract() != null) {
            futures.add(orderService.getContractDetails(
                currentStrategy.getBuyCallContract().toIBContract())
                .thenApply(list -> list.get(0)));
        }
        if (currentStrategy.getSellPutContract() != null) {
            futures.add(orderService.getContractDetails(
                currentStrategy.getSellPutContract().toIBContract())
                .thenApply(list -> list.get(0)));
        }
        if (currentStrategy.getBuyPutContract() != null) {
            futures.add(orderService.getContractDetails(
                currentStrategy.getBuyPutContract().toIBContract())
                .thenApply(list -> list.get(0)));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenRun(() -> SwingUtilities.invokeLater(() -> 
                statusLabel.setText("All contracts validated. Ready to place order.")))
            .exceptionally(ex -> {
                SwingUtilities.invokeLater(() -> 
                    statusLabel.setText("Validation failed: " + ex.getMessage()));
                return null;
            });
    }

    private void placeOrder() {
        statusLabel.setText("Placing order...");
        statusLabel.setText("Order placement - connect to OrderService as needed");
    }

    private LocalDate toLocalDate(java.util.Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
