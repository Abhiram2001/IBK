package apidemo.stategies;

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

public class ExcelOrderImporter {
    
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
    
    public static ImportResult importFromExcel(File file) {
        List<ImportedOrder> orders = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        
        try (FileInputStream fis = new FileInputStream(file)) {
            Workbook workbook;
            String fileName = file.getName().toLowerCase();
            
            if (fileName.endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(fis);
            } else if (fileName.endsWith(".xls")) {
                workbook = new HSSFWorkbook(fis);
            } else {
                errors.add("Invalid file format. Please use .xlsx or .xls files");
                return new ImportResult(orders, errors);
            }
            
            Sheet sheet = workbook.getSheetAt(0);
            
            if (sheet.getPhysicalNumberOfRows() < 2) {
                errors.add("Excel file is empty or has no data rows");
                workbook.close();
                return new ImportResult(orders, errors);
            }
            
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                try {
                    String symbol = getCellValueAsString(row.getCell(0)).trim().toUpperCase();
                    if (symbol.isEmpty()) continue;
                    
                    String expiry = parseDateCell(row.getCell(1));
                    String action = getCellValueAsString(row.getCell(2)).trim().toUpperCase();
                    String optionType = getCellValueAsString(row.getCell(3)).trim().toUpperCase();
                    double strike = getCellValueAsDouble(row.getCell(4));
                    double targetPrice = getCellValueAsDouble(row.getCell(5));
                    double alertThreshold = getCellValueAsDouble(row.getCell(6));
                    int quantity = (int) getCellValueAsDouble(row.getCell(7));
                    String orderType = getCellValueAsString(row.getCell(8)).trim().toUpperCase();
                    
                    if (!action.equals("BUY") && !action.equals("SELL")) {
                        errors.add("Row " + (i + 1) + ": Invalid action '" + action + "'. Must be BUY or SELL");
                        continue;
                    }
                    
                    if (!optionType.equals("CALL") && !optionType.equals("PUT")) {
                        errors.add("Row " + (i + 1) + ": Invalid option type '" + optionType + "'. Must be CALL or PUT");
                        continue;
                    }
                    
                    if (!orderType.equals("LMT") && !orderType.equals("MKT")) {
                        errors.add("Row " + (i + 1) + ": Invalid order type '" + orderType + "'. Must be LMT or MKT");
                        continue;
                    }
                    
                    if (strike <= 0 || targetPrice <= 0 || alertThreshold <= 0 || quantity <= 0) {
                        errors.add("Row " + (i + 1) + ": All numeric values must be positive");
                        continue;
                    }
                    
                    orders.add(new ImportedOrder(symbol, expiry, action, optionType, strike,
                                                targetPrice, alertThreshold, quantity, orderType, i + 1));
                    
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
                return String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
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
                    return Double.parseDouble(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
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
                SimpleDateFormat inputFormat = new SimpleDateFormat("dd-MMM-yy");
                Date date = inputFormat.parse(dateStr);
                SimpleDateFormat outputFormat = new SimpleDateFormat("yyyyMMdd");
                return outputFormat.format(date);
            } catch (Exception e) {
                return dateStr.replaceAll("[^0-9]", "");
            }
        }
        return "";
    }
}
