package apidemo.stategies;

import apidemo.util.HtmlButton;
import apidemo.util.UpperField;
import apidemo.util.VerticalPanel;
import com.ib.client.*;
import com.ib.controller.ApiController;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

public class CalendarSpreadStrategyPanel extends JPanel {
    private UpperField m_currentExpiryDate = new UpperField();
    private UpperField m_nextExpiryDate = new UpperField();
    private final UpperField m_spotPrice = new UpperField();
    private UpperField m_sellLegLengthFromSpotPrice = new UpperField();
    private JCheckBox m_useSellStikesForBuying = new JCheckBox();
    private UpperField m_buyLegLengthFromSellPrice = new UpperField();

    private Contract m_putSellContract, m_callSellContract, m_putBuyContract, m_callBuyContract;

    enum ContractType {
        PUT_SELL, CALL_SELL, PUT_BUY, CALL_BUY;
    }

    // New member variables for legs
    private ComboLeg m_sellCallLeg, m_sellPutLeg, m_buyCallLeg, m_buyPutLeg;


    transient ApiController.TopMktDataAdapter m_stockListener = new ApiController.TopMktDataAdapter() {
        @Override public void tickPrice(TickType tickType, double price, TickAttrib attribs) {
            if (tickType == TickType.LAST || tickType == TickType.DELAYED_LAST || tickType == TickType.CLOSE) { //TODO: Remove close
                m_spotPrice.setText( "" + customRound(price));
                CalendarSpreadStrategy.INSTANCE.controller().cancelTopMktData(m_stockListener);

                createAndPopulateContracts(m_spotPrice.getDouble());
            }
        }
    };

    private void createAndPopulateContracts(double spotPrice) {
        createContracts(spotPrice);

        //
        populateContractDetails(m_callSellContract, ContractType.CALL_SELL);
        populateContractDetails(m_putSellContract, ContractType.PUT_SELL);
        populateContractDetails(m_callBuyContract, ContractType.CALL_BUY);
        populateContractDetails(m_putBuyContract, ContractType.PUT_BUY);
    }

    public CalendarSpreadStrategyPanel() {
        VerticalPanel p = getInputPanel();
        VerticalPanel butPanel = getButtonPanel();
        setLayout(new BorderLayout());
        add(p, BorderLayout.WEST);
        add(butPanel);
    }

    private VerticalPanel getInputPanel() {
        VerticalPanel p = new VerticalPanel();
        p.add("Current expiry", m_currentExpiryDate);
        p.add("Next expiry", m_nextExpiryDate);
        p.add("Spot price", m_spotPrice);
        p.add("Sell legs length from spot", m_sellLegLengthFromSpotPrice);
        p.add("Use Sell strikes for buying", m_useSellStikesForBuying);
        p.add("Buy legs length from sell", m_buyLegLengthFromSellPrice);
        return p;
    }

    private VerticalPanel getButtonPanel() {

        HtmlButton populateDates = new HtmlButton("Populate dates") {
            @Override
            protected void actionPerformed() {
                populateDates();
            }
        };

        HtmlButton populateContracts = new HtmlButton("Populate contracts") {
            @Override
            protected void actionPerformed() {
                populateContractDetails();
            }
        };

        HtmlButton placeOrder = new HtmlButton("Place order") {
            @Override
            protected void actionPerformed() {
                onPlaceOrder();
            }
        };

        VerticalPanel butPanel = new VerticalPanel();
        butPanel.add(populateDates);
        butPanel.add(populateContracts);
        butPanel.add(placeOrder);
        return butPanel;
    }

    private void populateDates() {
        LocalDate todayDate = LocalDate.now();
        // Create Calendar instance and set it to today's date
        Calendar calendar = Calendar.getInstance();
        calendar.set(todayDate.getYear(), todayDate.getMonthValue() - 1, todayDate.getDayOfMonth());
        Date today = calendar.getTime();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

        // After 7 days
        calendar.add(Calendar.DAY_OF_MONTH, 7);
        String dateAfter7Days = sdf.format(calendar.getTime());

        //Next week
        calendar.setTime(today);
        calendar.add(Calendar.DAY_OF_MONTH, 14);
        String dateAfter14Days = sdf.format(calendar.getTime());

        m_currentExpiryDate.setText(dateAfter7Days);
        m_nextExpiryDate.setText(dateAfter14Days);
    }

    protected void populateContractDetails(Contract contract, final ContractType type) {
        CalendarSpreadStrategy.INSTANCE.controller().reqContractDetails(contract, list -> {
            for (ContractDetails details : list) {
                System.out.println(details.contract());
                populateLegs(details.contract(), type);
            }
        });
    }

    protected void populateContractDetails() {
        onOK();
    }

    protected void onPlaceOrder() {
        Contract comboContract = new Contract();
        comboContract.symbol("SPY");
        comboContract.secType("BAG");
        comboContract.currency("USD");
        comboContract.exchange("SMART");

        // Adding all four legs
        comboContract.comboLegs(new ArrayList<>(Arrays.asList(m_sellCallLeg, m_sellPutLeg, m_buyCallLeg, m_buyPutLeg)));

        Order comboOrder = new Order();
        comboOrder.orderType("LMT");
        comboOrder.lmtPrice(0.50);
        comboOrder.action("BUY");
        comboOrder.totalQuantity(Decimal.get(1));
        comboOrder.tif("GTC");

        CalendarSpreadStrategy.INSTANCE.controller().placeOrModifyOrder(comboContract, comboOrder, null);
    }

    private void onOK() {
        if (m_spotPrice.getText().isEmpty()) {
            fetchCurrentSPYPrice();
        } else {

        }
    }

    // Fetch the current SPY price if the spot price is not provided
    private void fetchCurrentSPYPrice() {
        Contract spyContract = new Contract();
        spyContract.symbol("SPY");
        spyContract.secType("STK");
        spyContract.exchange("SMART");
        spyContract.currency("USD");
        CalendarSpreadStrategy.INSTANCE.controller().reqTopMktData(spyContract, "", false, false, m_stockListener);
    }

    // Populate contracts based on the spot price
    private void createContracts(double spotPrice) {
        String dateAfter7Days = m_currentExpiryDate.getText();
        String dateAfter14Days = m_nextExpiryDate.getText();

        m_callSellContract = createOptionContract("C", spotPrice + m_sellLegLengthFromSpotPrice.getDouble(), dateAfter7Days);
        m_putSellContract = createOptionContract("P", spotPrice - m_sellLegLengthFromSpotPrice.getDouble(), dateAfter7Days);

        m_callBuyContract = createOptionContract("C", spotPrice + m_buyLegLengthFromSellPrice.getDouble(), dateAfter14Days);
        m_putBuyContract = createOptionContract("P", spotPrice - m_buyLegLengthFromSellPrice.getDouble(), dateAfter14Days);
    }

    private Contract createOptionContract(String right, double strike, String date) {
        Contract contract = new Contract();
        contract.symbol("SPY");
        contract.secType("OPT");
        contract.exchange("SMART");
        contract.currency("USD");
        contract.lastTradeDateOrContractMonth(date);
        contract.strike(strike);
        contract.right(right);
        contract.multiplier("100");
        return contract;
    }

    // Populate the legs when contract details are received
    private void populateLegs(Contract contract, ContractType type) {
        // Create a new ComboLeg for the given contract
        ComboLeg leg = new ComboLeg();
        leg.conid(contract.conid());  // Set the contract ID
        leg.ratio(1);                  // Default ratio (1:1)
        leg.exchange("SMART");         // Set the exchange to SMART (IB's default)

        // Assign legs based on the contract type
        switch (type) {
            case CALL_BUY:
                leg.action("BUY");
                m_buyCallLeg = leg;  // Assign leg as buy call leg
                break;

            case CALL_SELL:
                leg.action("SELL");
                m_sellCallLeg = leg;  // Assign leg as sell call leg
                break;

            case PUT_BUY:
                leg.action("BUY");
                m_buyPutLeg = leg;    // Assign leg as buy put leg
                break;

            case PUT_SELL:
                leg.action("SELL");
                m_sellPutLeg = leg;   // Assign leg as sell put leg
                break;

            default:
                throw new IllegalArgumentException("Invalid contract type: " + type);
        }
    }

    public static double customRound(double value) {
        double fractionalPart = value - Math.floor(value);

        if (fractionalPart > 0.5) {
            return Math.ceil(value);  // Round up
        } else {
            return Math.floor(value); // Round down
        }
    }
}