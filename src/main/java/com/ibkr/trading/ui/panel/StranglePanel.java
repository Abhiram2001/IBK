package com.ibkr.trading.ui.panel;

import apidemo.util.HtmlButton;
import apidemo.util.UpperField;
import apidemo.util.VerticalPanel;
import com.ib.client.Contract;
import com.ibkr.trading.domain.OptionContract;
import com.ibkr.trading.domain.StrangleStrategy;
import com.ibkr.trading.service.*;
import com.toedter.calendar.JDateChooser;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Panel for executing Strangle option strategies on SPY.
 * A strangle involves selling both a call and put option at different strike prices,
 * both out-of-the-money. This strategy profits when the underlying stays within a range.
 */
public class StranglePanel extends JPanel {
    private final MarketDataService marketDataService;
    private final OrderService orderService;
    
    private final JDateChooser expiryChooser;
    private final UpperField spotPriceField = new UpperField();
    private final UpperField callOffsetField = new UpperField();
    private final UpperField putOffsetField = new UpperField();
    private final UpperField quantityField = new UpperField();
    private final UpperField limitPriceField = new UpperField();
    private final JTextArea statusArea = new JTextArea(3, 40);
    
    private HtmlButton placeOrderButton;
    private Contract callContract;
    private Contract putContract;
    private int contractsValidated = 0;
    private static final int TOTAL_CONTRACTS = 2;

    public StranglePanel(ConnectionService connectionService) {
        this.marketDataService = new MarketDataService(connectionService.getController());
        this.orderService = new OrderService(connectionService.getController());
        this.expiryChooser = UIComponents.createDateChooser();
        
        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());

        JLabel titleLabel = new JLabel("Strangle Strategy");
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
        panel.add("Expiry Date", expiryChooser);
        panel.add("Spot Price", spotPriceField);
        panel.add("Call Strike Offset", callOffsetField);
        panel.add("Put Strike Offset", putOffsetField);
        panel.add("Quantity", quantityField);
        panel.add("Limit Price (per contract)", limitPriceField);
        return panel;
    }

    private VerticalPanel createButtonPanel() {
        VerticalPanel panel = new VerticalPanel();
        panel.add(createFetchPriceButton());
        panel.add(createValidateButton());
        
        placeOrderButton = new HtmlButton("Place Order") {
            @Override
            protected void actionPerformed() {
                updateStatus("Placing orders...");
                placeStrangleOrder();
            }
        };
        placeOrderButton.setVisible(false);
        panel.add(placeOrderButton);
        
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
                    StrangleStrategy strategy = buildStrategy();
                    updateStatus("Validating contracts...");
                    validateContracts(strategy);
                } catch (Exception e) {
                    updateStatus("Error: " + e.getMessage());
                }
            }
        };
    }

    private void populateDefaults() {
        LocalDate today = LocalDate.now();
        expiryChooser.setDate(java.sql.Date.valueOf(today.plusDays(7)));
        quantityField.setText("1");
        callOffsetField.setText("5");
        putOffsetField.setText("5");
        limitPriceField.setText("0.50");
    }

    private StrangleStrategy buildStrategy() {
        return StrangleStrategy.builder()
            .spotPrice(spotPriceField.getDouble())
            .quantity(quantityField.getInt())
            .expiry(toLocalDate(expiryChooser.getDate()))
            .callStrikeOffset(callOffsetField.getDouble())
            .putStrikeOffset(putOffsetField.getDouble())
            .build();
    }

    private void validateContracts(StrangleStrategy strategy) {
        contractsValidated = 0;
        placeOrderButton.setVisible(false);
        
        OptionContract callOption = strategy.getCallContract();
        OptionContract putOption = strategy.getPutContract();
        
        callContract = callOption.toIBContract();
        putContract = putOption.toIBContract();
        
        // Validate call contract
        orderService.getContractDetails(callContract)
            .thenAccept(details -> SwingUtilities.invokeLater(() -> {
                if (details.size() > 1) {
                    updateStatus("ERROR: Multiple call contracts found");
                    return;
                }
                contractsValidated++;
                checkValidationComplete();
            }))
            .exceptionally(ex -> {
                SwingUtilities.invokeLater(() -> 
                    updateStatus("Call contract error: " + ex.getMessage()));
                return null;
            });
        
        // Validate put contract
        orderService.getContractDetails(putContract)
            .thenAccept(details -> SwingUtilities.invokeLater(() -> {
                if (details.size() > 1) {
                    updateStatus("ERROR: Multiple put contracts found");
                    return;
                }
                contractsValidated++;
                checkValidationComplete();
            }))
            .exceptionally(ex -> {
                SwingUtilities.invokeLater(() -> 
                    updateStatus("Put contract error: " + ex.getMessage()));
                return null;
            });
    }
    
    private void checkValidationComplete() {
        if (contractsValidated == TOTAL_CONTRACTS) {
            placeOrderButton.setVisible(true);
            updateStatus("Contracts validated successfully. Ready to place order.");
        }
    }
    
    private void placeStrangleOrder() {
        int quantity = quantityField.getInt();
        double limitPrice = limitPriceField.getDouble();
        
        // Place call order (SELL)
        orderService.placeSimpleOrder(callContract, "SELL", quantity, limitPrice, 
            status -> SwingUtilities.invokeLater(() -> 
                updateStatus("Call order: " + status)));
        
        // Place put order (SELL)
        orderService.placeSimpleOrder(putContract, "SELL", quantity, limitPrice,
            status -> SwingUtilities.invokeLater(() -> {
                updateStatus("Put order: " + status);
                if (status.contains("Submitted") || status.contains("PreSubmitted")) {
                    placeOrderButton.setVisible(false);
                }
            }));
    }
    
    private void updateStatus(String message) {
        statusArea.setText(message);
    }

    private LocalDate toLocalDate(java.util.Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
