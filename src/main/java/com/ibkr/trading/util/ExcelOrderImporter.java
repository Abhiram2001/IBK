package com.ibkr.trading.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Utility for importing pre-market close orders from Excel files.
 * Supports both .xlsx and .xls formats with comprehensive validation.
 * 
 * Expected Excel format:
 * Column 0: Symbol (e.g., SPY)
 * Column 1: Expiry Date (date or dd-MMM-yy format)
 * Column 2: Action (BUY or SELL)
 * Column 3: Option Type (CALL or PUT)
 * Column 4: Strike Price (numeric)
 * Column 5: Target Price (numeric)
 * Column 6: Alert Threshold (numeric)
 * Column 7: Quantity (numeric)
 * Column 8: Order Type (LMT or MKT)
 * 
 * @author IBK Trading System
 * @version 2.0
 */
public class ExcelOrderImporter {
    
    /**
     * Represents an order imported from Excel.
     */
    public static class ImportedOrder {
        public final String symbol;
        public final String expiry;
        public final String action;
        public final String optionType;
        public final double strike;
        public final double targetPrice;
        public final double alertThreshold;
        public final int quantity;
        public final String orderType;
        public final int rowNumber;
        
        public ImportedOrder(String symbol, String expiry, String action, String optionType,
                           double strike, double targetPrice, double alertThreshold,
                           int quantity, String orderType, int rowNumber) {
            this.symbol = symbol;
            this.expiry = expiry;
            this.action = action;
            this.optionType = optionType;
            this.strike = strike;
            this.targetPrice = targetPrice;
            this.alertThreshold = alertThreshold;
            this.quantity = quantity;
            this.orderType = orderType;
            this.rowNumber = rowNumber;
        }
    }
    
    /**
     * Result of an Excel import operation.
     */
    public static class ImportResult {
        public final List<ImportedOrder> orders;
        public final List<String> errors;
        public final boolean success;
        
        public ImportResult(List<ImportedOrder> orders, List<String> errors) {
            this.orders = orders;
            this.errors = errors;
            this.success = !orders.isEmpty();
        }
    }
    
    /**
     * Imports orders from an Excel file.
     * 
     * @param file the Excel file to import
     * @return ImportResult containing orders and any errors
     */
    public static ImportResult importFromExcel(File file) {
        List<ImportedOrder> orders = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        
        if (file == null || !file.exists()) {
            errors.add("File does not exist");
            return new ImportResult(orders, errors);
        }
        
        try (FileInputStream fis = new FileInputStream(file)) {
            Workbook workbook = createWorkbook(file, fis);
            if (workbook == null) {
                errors.add("Invalid file format. Please use .xlsx or .xls files");
                return new ImportResult(orders, errors);
            }
            
            Sheet sheet = workbook.getSheetAt(0);
            
            if (sheet.getPhysicalNumberOfRows() < 2) {
                errors.add("Excel file is empty or has no data rows (expecting header + data)");
                workbook.close();
                return new ImportResult(orders, errors);
            }
            
            // Process each row starting from row 1 (skipping header)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                try {
                    ImportedOrder order = parseRow(row, i);
                    if (order != null) {
                        orders.add(order);
                    }
                } catch (Exception e) {
                    errors.add("Row " + (i + 1) + ": " + e.getMessage());
                }
            }
            
            workbook.close();
            
        } catch (IOException e) {
            errors.add("Error reading Excel file: " + e.getMessage());
        }
        
        return new ImportResult(orders, errors);
    }
    
    private static Workbook createWorkbook(File file, FileInputStream fis) throws IOException {
        String fileName = file.getName().toLowerCase();
        
        if (fileName.endsWith(".xlsx")) {
            return new XSSFWorkbook(fis);
        } else if (fileName.endsWith(".xls")) {
            return new HSSFWorkbook(fis);
        }
        return null;
    }
    
    private static ImportedOrder parseRow(Row row, int rowIndex) throws Exception {
        String symbol = getCellValueAsString(row.getCell(0)).trim().toUpperCase();
        if (symbol.isEmpty()) return null; // Skip empty rows
        
        String expiry = parseDateCell(row.getCell(1));
        String action = getCellValueAsString(row.getCell(2)).trim().toUpperCase();
        String optionType = getCellValueAsString(row.getCell(3)).trim().toUpperCase();
        double strike = getCellValueAsDouble(row.getCell(4));
        double targetPrice = getCellValueAsDouble(row.getCell(5));
        double alertThreshold = getCellValueAsDouble(row.getCell(6));
        int quantity = (int) getCellValueAsDouble(row.getCell(7));
        String orderType = getCellValueAsString(row.getCell(8)).trim().toUpperCase();
        
        // Validate values
        if (!action.equals("BUY") && !action.equals("SELL")) {
            throw new IllegalArgumentException("Invalid action '" + action + "'. Must be BUY or SELL");
        }
        
        if (!optionType.equals("CALL") && !optionType.equals("PUT")) {
            throw new IllegalArgumentException("Invalid option type '" + optionType + "'. Must be CALL or PUT");
        }
        
        if (!orderType.equals("LMT") && !orderType.equals("MKT")) {
            throw new IllegalArgumentException("Invalid order type '" + orderType + "'. Must be LMT or MKT");
        }
        
        if (strike <= 0 || targetPrice <= 0 || alertThreshold <= 0 || quantity <= 0) {
            throw new IllegalArgumentException("All numeric values must be positive");
        }
        
        return new ImportedOrder(symbol, expiry, action, optionType, strike,
                                targetPrice, alertThreshold, quantity, orderType, rowIndex + 1);
    }
    
    private static String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    Date date = cell.getDateCellValue();
                    SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yy");
                    return sdf.format(date);
                }
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    return cell.getStringCellValue();
                }
            default:
                return "";
        }
    }
    
    private static double getCellValueAsDouble(Cell cell) {
        if (cell == null) return 0.0;
        
        switch (cell.getCellType()) {
            case NUMERIC:
                return cell.getNumericCellValue();
            case STRING:
                try {
                    String value = cell.getStringCellValue().trim();
                    // Remove currency symbols and commas
                    value = value.replaceAll("[$,]", "");
                    return Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    return 0.0;
                }
            case FORMULA:
                try {
                    return cell.getNumericCellValue();
                } catch (Exception e) {
                    return 0.0;
                }
            default:
                return 0.0;
        }
    }
    
    private static String parseDateCell(Cell cell) {
        if (cell == null) return "";
        
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            Date date = cell.getDateCellValue();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            return sdf.format(date);
        } else if (cell.getCellType() == CellType.STRING) {
            String dateStr = cell.getStringCellValue().trim();
            try {
                // Try parsing dd-MMM-yy format
                SimpleDateFormat inputFormat = new SimpleDateFormat("dd-MMM-yy");
                Date date = inputFormat.parse(dateStr);
                SimpleDateFormat outputFormat = new SimpleDateFormat("yyyyMMdd");
                return outputFormat.format(date);
            } catch (Exception e) {
                // If format fails, try to extract digits only (assuming yyyyMMdd)
                return dateStr.replaceAll("[^0-9]", "");
            }
        }
        return "";
    }
}
