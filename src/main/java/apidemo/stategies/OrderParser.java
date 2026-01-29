package apidemo.stategies;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OrderParser {
    
    public static class ParsedOrder {
        public final LocalDate expiryDate;
        public final String action;
        public final String symbol;
        public final String optionType;
        public final double strikePrice;
        public final boolean isValid;
        public final String errorMessage;
        
        private ParsedOrder(LocalDate expiryDate, String action, String symbol, 
                           String optionType, double strikePrice, boolean isValid, String errorMessage) {
            this.expiryDate = expiryDate;
            this.action = action;
            this.symbol = symbol;
            this.optionType = optionType;
            this.strikePrice = strikePrice;
            this.isValid = isValid;
            this.errorMessage = errorMessage;
        }
        
        public static ParsedOrder valid(LocalDate expiryDate, String action, String symbol, 
                                       String optionType, double strikePrice) {
            return new ParsedOrder(expiryDate, action, symbol, optionType, strikePrice, true, null);
        }
        
        public static ParsedOrder invalid(String errorMessage) {
            return new ParsedOrder(null, null, null, null, 0.0, false, errorMessage);
        }
        
        @Override
        public String toString() {
            if (!isValid) return "Invalid: " + errorMessage;
            return String.format("%s %s %s %s %.2f (Expiry: %s)", 
                action, symbol, optionType, strikePrice, expiryDate);
        }
    }
    
    public static ParsedOrder parseOrder(String input) {
        if (input == null || input.trim().isEmpty()) {
            return ParsedOrder.invalid("Empty input");
        }
        
        input = input.trim().toLowerCase();
        
        Pattern pattern = Pattern.compile(
            "(\\d{1,2})\\s+(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)\\s+(\\d{2,4})\\s+" +
            "(buy|sell)\\s+" +
            "(\\w+)\\s+" +
            "(call|put)\\s+" +
            "(\\d+(?:\\.\\d+)?)",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = pattern.matcher(input);
        
        if (!matcher.find()) {
            return ParsedOrder.invalid("Invalid format. Expected: 'DD MMM YY BUY/SELL SYMBOL CALL/PUT STRIKE'");
        }
        
        try {
            int day = Integer.parseInt(matcher.group(1));
            String monthStr = matcher.group(2);
            String yearStr = matcher.group(3);
            String action = matcher.group(4).toUpperCase();
            String symbol = matcher.group(5).toUpperCase();
            String optionType = matcher.group(6).toUpperCase();
            double strikePrice = Double.parseDouble(matcher.group(7));
            
            int year = Integer.parseInt(yearStr);
            if (year < 100) {
                year += 2000;
            }
            
            String dateStr = String.format("%d %s %d", day, monthStr, year);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
            LocalDate expiryDate = LocalDate.parse(dateStr, formatter);
            
            if (expiryDate.isBefore(LocalDate.now())) {
                return ParsedOrder.invalid("Expiry date is in the past");
            }
            
            return ParsedOrder.valid(expiryDate, action, symbol, optionType, strikePrice);
            
        } catch (DateTimeParseException e) {
            return ParsedOrder.invalid("Invalid date: " + e.getMessage());
        } catch (NumberFormatException e) {
            return ParsedOrder.invalid("Invalid number format: " + e.getMessage());
        } catch (Exception e) {
            return ParsedOrder.invalid("Parse error: " + e.getMessage());
        }
    }
}
