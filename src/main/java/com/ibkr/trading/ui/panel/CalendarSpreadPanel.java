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
    private final UpperField limitPriceField = new UpperField();
    private final JCheckBox includeCallCheckbox = new JCheckBox("Include Call", true);
    private final JCheckBox includePutCheckbox = new JCheckBox("Include Put", true);
    private final JTextArea statusArea = new JTextArea(3, 40);
    
    private CalendarSpread currentStrategy;
    private ContractDetails callSellDetails;
    private ContractDetails putSellDetails;
    private ContractDetails callBuyDetails;
    private ContractDetails putBuyDetails;

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

        add(mainPanel, BorderLayout.CENTER);
        
        // Fixed-height status area attached to bottom
        statusArea.setEditable(false);
        statusArea.setLineWrap(true);
        statusArea.setWrapStyleWord(true);
        statusArea.setBackground(getBackground());
        JScrollPane statusScroll = new JScrollPane(statusArea);
        statusScroll.setPreferredSize(new Dimension(500, 60));
        statusScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        add(statusScroll, BorderLayout.SOUTH);
    }

    private VerticalPanel createInputPanel() {
        VerticalPanel panel = new VerticalPanel();
        panel.add("Near Expiry", nearExpiryChooser);
        panel.add("Far Expiry", farExpiryChooser);
        panel.add("Spot Price", spotPriceField);
        panel.add("Call Sell Offset", callSellOffsetField);
        panel.add("Call Buy Offset", callBuyOffsetField);
        panel.add("Put Sell Offset", putSellOffsetField);
        panel.add("Put Buy Offset", putBuyOffsetField);
        panel.add("Quantity", quantityField);
        panel.add("Limit Price", limitPriceField);
        
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
                updateStatus("Fetching price...");
                marketDataService.getStockPrice("SPY")
                    .thenAccept(price -> SwingUtilities.invokeLater(() -> {
                        spotPriceField.setText(String.format("%.2f", price));
                        populateDefaults();
                        updateStatus("Price fetched: $" + String.format("%.2f", price));
                    }))
                    .exceptionally(ex -> {
                        SwingUtilities.invokeLater(() -> 
                            updateStatus("Error: " + ex.getMessage()));
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
                    updateStatus("Validating contracts...");
                    validateContracts();
                } catch (Exception e) {
                    updateStatus("Error: " + e.getMessage());
                }
            }
        };
    }

    private HtmlButton createPlaceOrderButton() {
        return new HtmlButton("Place Order") {
            @Override
            protected void actionPerformed() {
                if (currentStrategy == null) {
                    updateStatus("Please validate contracts first");
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
        limitPriceField.setText("0.50");
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
        
        // Reset contract details
        callSellDetails = null;
        putSellDetails = null;
        callBuyDetails = null;
        putBuyDetails = null;
        
        // Validate sell call contract
        if (currentStrategy.getSellCallContract() != null) {
            futures.add(orderService.getContractDetails(
                currentStrategy.getSellCallContract().toIBContract())
                .thenApply(list -> {
                    if (list.size() != 1) {
                        throw new IllegalStateException("Expected 1 call sell contract, found " + list.size());
                    }
                    callSellDetails = list.get(0);
                    return callSellDetails;
                }));
        }
        
        // Validate buy call contract
        if (currentStrategy.getBuyCallContract() != null) {
            futures.add(orderService.getContractDetails(
                currentStrategy.getBuyCallContract().toIBContract())
                .thenApply(list -> {
                    if (list.size() != 1) {
                        throw new IllegalStateException("Expected 1 call buy contract, found " + list.size());
                    }
                    callBuyDetails = list.get(0);
                    return callBuyDetails;
                }));
        }
        
        // Validate sell put contract
        if (currentStrategy.getSellPutContract() != null) {
            futures.add(orderService.getContractDetails(
                currentStrategy.getSellPutContract().toIBContract())
                .thenApply(list -> {
                    if (list.size() != 1) {
                        throw new IllegalStateException("Expected 1 put sell contract, found " + list.size());
                    }
                    putSellDetails = list.get(0);
                    return putSellDetails;
                }));
        }
        
        // Validate buy put contract
        if (currentStrategy.getBuyPutContract() != null) {
            futures.add(orderService.getContractDetails(
                currentStrategy.getBuyPutContract().toIBContract())
                .thenApply(list -> {
                    if (list.size() != 1) {
                        throw new IllegalStateException("Expected 1 put buy contract, found " + list.size());
                    }
                    putBuyDetails = list.get(0);
                    return putBuyDetails;
                }));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenRun(() -> SwingUtilities.invokeLater(() -> 
                updateStatus("All contracts validated successfully. Ready to place order.")))
            .exceptionally(ex -> {
                SwingUtilities.invokeLater(() -> 
                    updateStatus("Validation failed: " + ex.getMessage()));
                return null;
            });
    }

    private void placeOrder() {
        // Verify all required contracts are validated
        if (!areContractsValidated()) {
            updateStatus("Please validate contracts first");
            return;
        }
        
        updateStatus("Creating BAG order...");
        
        // Create combo legs
        List<ComboLeg> legs = new ArrayList<>();
        
        // Add SELL call leg (near-term)
        if (callSellDetails != null) {
            ComboLeg leg = new ComboLeg();
            leg.conid(callSellDetails.contract().conid());
            leg.ratio(1);
            leg.action("SELL");
            leg.exchange("SMART");
            legs.add(leg);
        }
        
        // Add SELL put leg (near-term)
        if (putSellDetails != null) {
            ComboLeg leg = new ComboLeg();
            leg.conid(putSellDetails.contract().conid());
            leg.ratio(1);
            leg.action("SELL");
            leg.exchange("SMART");
            legs.add(leg);
        }
        
        // Add BUY call leg (far-term)
        if (callBuyDetails != null) {
            ComboLeg leg = new ComboLeg();
            leg.conid(callBuyDetails.contract().conid());
            leg.ratio(1);
            leg.action("BUY");
            leg.exchange("SMART");
            legs.add(leg);
        }
        
        // Add BUY put leg (far-term)
        if (putBuyDetails != null) {
            ComboLeg leg = new ComboLeg();
            leg.conid(putBuyDetails.contract().conid());
            leg.ratio(1);
            leg.action("BUY");
            leg.exchange("SMART");
            legs.add(leg);
        }
        
        if (legs.isEmpty()) {
            updateStatus("Error: No legs to place");
            return;
        }
        
        // Place combo order
        double limitPrice = limitPriceField.getDouble();
        int quantity = quantityField.getInt();
        
        updateStatus("Placing BAG order with " + legs.size() + " legs...");
        
        orderService.placeComboOrder(legs, limitPrice, quantity, 
            status -> SwingUtilities.invokeLater(() -> updateStatus(status)));
    }
    
    private boolean areContractsValidated() {
        boolean includeCall = includeCallCheckbox.isSelected();
        boolean includePut = includePutCheckbox.isSelected();
        
        if (includeCall && (callSellDetails == null || callBuyDetails == null)) {
            return false;
        }
        if (includePut && (putSellDetails == null || putBuyDetails == null)) {
            return false;
        }
        return includeCall || includePut;
    }
    
    private void updateStatus(String message) {
        statusArea.setText(message);
    }

    private LocalDate toLocalDate(java.util.Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
