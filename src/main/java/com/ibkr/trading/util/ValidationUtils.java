package com.ibkr.trading.util;

/**
 * Utility methods for validation and data processing.
 * 
 * @author IBK Trading System
 * @version 2.0
 */
public class ValidationUtils {
    
    public static double roundToNearestInteger(double value) {
        return Math.round(value);
    }

    public static double ensurePositive(double value) {
        return Math.max(0, value);
    }

    public static void requirePositive(double value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
    }

    public static void requireNonNull(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }
    
    /**
     * Validates that a stock symbol is not empty.
     * 
     * @param symbol the symbol to validate
     * @throws IllegalArgumentException if symbol is null or empty
     */
    public static void validateSymbol(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Symbol cannot be empty");
        }
    }
    
    /**
     * Parses a positive integer from a string.
     * 
     * @param value the string value to parse
     * @param fieldName the field name for error messages
     * @return the parsed integer
     * @throws IllegalArgumentException if value is invalid or not positive
     */
    public static int parsePositiveInt(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        try {
            int result = Integer.parseInt(value.trim());
            if (result <= 0) {
                throw new IllegalArgumentException(fieldName + " must be positive");
            }
            return result;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be a valid number");
        }
    }
    
    /**
     * Parses a positive double from a string.
     * 
     * @param value the string value to parse
     * @param fieldName the field name for error messages
     * @return the parsed double
     * @throws IllegalArgumentException if value is invalid or not positive
     */
    public static double parsePositiveDouble(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        try {
            double result = Double.parseDouble(value.trim());
            if (result <= 0) {
                throw new IllegalArgumentException(fieldName + " must be positive");
            }
            return result;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be a valid number");
        }
    }
}
