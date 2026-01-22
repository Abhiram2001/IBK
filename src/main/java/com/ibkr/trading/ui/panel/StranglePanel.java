package com.ibkr.trading.ui.panel;

import apidemo.util.HtmlButton;
import apidemo.util.UpperField;
import apidemo.util.VerticalPanel;
import com.ibkr.trading.domain.StrangleStrategy;
import com.ibkr.trading.service.*;
import com.toedter.calendar.JDateChooser;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Panel for executing Strangle strategies.
 */
public class StranglePanel extends JPanel {
    private final MarketDataService marketDataService;
    private final OrderService orderService;
    
    private final JDateChooser expiryChooser;
    private final UpperField spotPriceField = new UpperField();
    private final UpperField callOffsetField = new UpperField();
    private final UpperField putOffsetField = new UpperField();
    private final UpperField quantityField = new UpperField();
    private final JLabel statusLabel = new JLabel();

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
        mainPanel.add(statusLabel);

        add(mainPanel, BorderLayout.CENTER);
    }

    private VerticalPanel createInputPanel() {
        VerticalPanel panel = new VerticalPanel();
        panel.add("Expiry Date", expiryChooser);
        panel.add("Spot Price", spotPriceField);
        panel.add("Quantity", quantityField);
        panel.add("Call Strike Offset", callOffsetField);
        panel.add("Put Strike Offset", putOffsetField);
        return panel;
    }

    private VerticalPanel createButtonPanel() {
        VerticalPanel panel = new VerticalPanel();
        panel.add(createFetchPriceButton());
        panel.add(createValidateButton());
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
                    StrangleStrategy strategy = buildStrategy();
                    statusLabel.setText("Strategy created: " + strategy.getStrategyName());
                } catch (Exception e) {
                    statusLabel.setText("Error: " + e.getMessage());
                }
            }
        };
    }

    private void populateDefaults() {
        LocalDate today = LocalDate.now();
        expiryChooser.setDate(java.sql.Date.valueOf(today.plusDays(30)));
        quantityField.setText("1");
        callOffsetField.setText("5");
        putOffsetField.setText("5");
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

    private LocalDate toLocalDate(java.util.Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
