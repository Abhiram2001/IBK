# Excel Trade Import Template (Multi-Sheet with Active Filtering)

## Overview

One Excel file can contain **multiple sheets**, each with multiple trades.
Only **visible sheets** and **active trades** are imported.

## Sheet-Level Control

| Sheet State | Behavior |
|-------------|----------|
| Visible     | **Imported** — normal visible sheet |
| Hidden      | **Skipped** — right-click sheet tab → Hide |
| Very Hidden | **Skipped** — hidden via VBA |

Use Excel's built-in Hide/Unhide (right-click sheet tab) to toggle sheets on/off.

## Column Format (per sheet)

Each sheet must have headers in row 1:

| Column | Header | Format | Example | Description |
|--------|--------|--------|---------|-------------|
| A | Trade ID | Text | T01 | Unique key for grouping legs |
| B | Account | Text | DU12345 | Trading account |
| C | Symbol | Text | SPY | Underlying symbol |
| D | Expiry | Date/Text | 15-Jan-26 | Option expiry date |
| E | Action | Text | CALL BUY | Combined option type and action |
| F | Role | Text | MAIN | MAIN for pricing leg, blank for others |
| G | Strike | Number | 600 | Strike price |
| **H** | **Rate** | **Number** | **1** | **Per-leg multiplier** |
| **I** | **QTY** | **Number** | **2** | **Whole-trade multiplier** |
| J | Target | Number | 1.50 | Target price (MAIN leg only) |
| K | Alert | Number | 0.10 | Alert threshold (MAIN leg only) |
| **L** | **Active** | **Text** | **Y** | **Y = import, blank/N = skip (default: inactive)** |

**Rate × QTY = Final leg quantity:** Each leg's Rate is multiplied by the trade's QTY to get the final contract count sent to TWS.

### Alert $ Direction (signed value)

| Alert $ Value | Meaning | Triggers When |
|---------------|---------|---------------|
| `+5.00`       | Above   | Market price ≥ 5.00 |
| `-3.50`       | Below   | Market price ≤ 3.50 |
| `0` or blank  | No alert | Never triggers |

## Active Column Rules

- **`Y` or `YES`** → trade is imported
- **Blank, `N`, or anything else** → trade is **skipped** (default = inactive)
- For **combo trades** (same Trade ID, multiple legs): if **any** leg is not `Y`, the **entire trade** is skipped

## Trade Types

### Single Orders
- Unique Trade ID per trade
- Role = `MAIN`, Active = `Y`

### Combo Orders
- Same Trade ID for all legs
- One leg marked `MAIN` for pricing
- All legs must have Active = `Y`

## Sample Data

### Sheet: "SPY Trades" (visible sheet)
```
Trade ID | Account | Symbol | Expiry    | Action    | Role | Strike | Rate | QTY | Target | Alert  | Active
---------|---------|--------|-----------|-----------|------|--------|------|-----|--------|--------|-------
T01      | DU12345 | SPY    | 15-Jan-26 | CALL BUY  | MAIN | 600    | 1    | 2   | 5.00   | -4.50  | Y
T02      | DU12345 | SPY    | 20-Feb-26 | PUT SELL  | MAIN | 500    | 2    | 1   | 3.00   | +3.50  | N
T03      | DU12345 | SPY    | 15-Jan-26 | CALL BUY  | MAIN | 600    | 1    | 3   | 2.50   | +2.80  | Y
T03      | DU12345 | SPY    | 15-Jan-26 | PUT BUY   |      | 600    | 2    | 3   |        |        | Y
```

- **T01**: Imported, Rate=1 × QTY=2 = **2 contracts**, alert when price ≤ $4.50
- **T02**: Skipped (Active=N)
- **T03**: Combo, QTY=3 for entire trade:
  - CALL BUY: Rate=1 × 3 = **3 contracts**
  - PUT BUY: Rate=2 × 3 = **6 contracts**
  - Alert when net ≥ $2.80

### Sheet: "QQQ Trades" (hidden sheet)
This entire sheet is skipped regardless of row-level Active values.
To hide: right-click sheet tab → Hide. To unhide: right-click any tab → Unhide.

## Reimport Behavior

When reimporting the same or updated Excel file:
- **New sheets** → new tab is created
- **Existing sheets** → only trades with **new Trade IDs** are added
- **Duplicate Trade IDs** → silently skipped (already in the panel)
- Use the **"Reimport Last File"** button for quick refresh after editing Excel

## Validation Rules

- **Trade ID**: Required, groups multiple legs (unique key per sheet)
- **Symbol**: Required for all legs
- **Expiry**: Required, consistent within combo
- **Action**: Format "CALL BUY", "PUT SELL", "BUY CALL", or "SELL PUT"
- **Role**: `MAIN` for pricing leg, blank for others
- **Strike**: Must be positive number
- **Rate**: Must be positive integer (per-leg multiplier)
- **QTY**: Must be positive integer (whole-trade multiplier)
- **Target**: Required positive value for MAIN role legs
- **Alert**: Signed value — positive (above), negative (below), 0 (no alert)
- **Active**: `Y`/`YES` to import; blank or anything else = skip

## How to Use

1. **Create Excel File**: Multiple sheets, each with the column format above
2. **Mark Active**: Set Active=Y on trades you want to import; hide sheets to skip them entirely
3. **Import**: Click "Import from Excel" → Select your file
4. **Review**: Import summary shows per-sheet counts, skipped sheets, and warnings
5. **Per-Sheet Tabs**: Each active sheet becomes its own tab with independent controls
6. **Monitor/Trade**: Use per-tab buttons to select, monitor, place orders, and remove trades
7. **Reimport**: Edit Excel → click "Reimport Last File" → only new Trade IDs are added

## Features

- ✅ **Multi-Sheet Support**: Each active sheet becomes its own tab
- ✅ **Active Filtering**: Sheet-level (hide/unhide) and row-level (Active column) control
- ✅ **Default Inactive**: Blank Active column = skipped (opt-in, not opt-out)
- ✅ **Directional Alerts**: Positive alert = above, negative = below, zero = disabled
- ✅ **Smart Reimport**: Only new Trade IDs are added, duplicates skipped
- ✅ **Single & Combo Orders**: Handles both simple and multi-leg strategies
- ✅ **Trade ID Grouping**: Multiple legs automatically grouped by Trade ID
- ✅ **Per-Sheet Management**: Independent monitoring and order placement per tab
- ✅ **Account Support**: Ready for multi-account trading
- ✅ **Real-time Monitoring**: Live price tracking with threshold alerts
