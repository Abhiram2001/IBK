# Excel Order Import Template

## Excel File Format

Create an Excel file (.xlsx or .xls) with the following columns in the first row as headers:

| Column | Header Name | Format | Example |
|--------|-------------|--------|---------|
| A | Symbol | Text | SPY |
| B | Expiry | Date (dd-MMM-yy) or yyyyMMdd | 15-Jan-26 or 20260115 |
| C | Action | Text (BUY/SELL) | BUY |
| D | Type | Text (CALL/PUT) | CALL |
| E | Strike | Number | 600 |
| F | Target $ | Number | 1.50 |
| G | Alert $ | Number | 0.10 |
| H | Qty | Number | 1 |
| I | Order Type | Text (LMT/MKT) | LMT |

## Sample Data Rows

```
Symbol | Expiry      | Action | Type | Strike | Target $ | Alert $ | Qty | Order Type
-------|-------------|--------|------|--------|----------|---------|-----|------------
SPY    | 15-Jan-26   | BUY    | CALL | 600    | 1.50     | 0.10    | 1   | LMT
SPY    | 15-Jan-26   | SELL   | PUT  | 580    | 2.00     | 0.15    | 2   | MKT
QQQ    | 20-Feb-26   | BUY    | CALL | 500    | 3.00     | 0.20    | 5   | LMT
```

## Notes

- The first row MUST contain headers (will be skipped during import)
- All numeric values must be positive
- Action must be either BUY or SELL
- Type must be either CALL or PUT
- Order Type must be either LMT (Limit) or MKT (Market)
- Empty rows will be skipped
- Expiry date can be formatted as Excel date or text in dd-MMM-yy format
- The system will validate each row and show errors for invalid entries

## How to Use

1. Create your Excel file following the format above
2. Open the Pre-Market Close Order Panel
3. Click "Import from Excel" button
4. Select your Excel file
5. Review any error messages for invalid rows
6. Valid orders will be added to the monitoring list
7. Click "Start Monitoring All" to begin monitoring the imported orders
