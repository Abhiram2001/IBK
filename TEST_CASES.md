# Pre-Market Close Order Monitoring - Test Cases

## Test Environment Setup
- IB Gateway/TWS connected
- Valid paper trading account
- Sample Excel file with test trades
- Market hours or paper trading data available

---

## 1. Excel Import - Sheet Level

### TC-001: Import visible sheets only
**Steps:**
1. Create Excel with 3 sheets: "SPY Trades" (visible), "QQQ Trades" (visible), "TEST Trades" (hidden)
2. Add at least 1 active trade to each sheet
3. Import Excel file

**Expected:**
- Import summary shows 2 sheets imported
- "TEST Trades" in skipped sheets list
- 2 tabs created: "SPY Trades", "QQQ Trades"
- No "TEST Trades" tab

### TC-002: Import with all sheets hidden
**Steps:**
1. Create Excel with all sheets hidden
2. Import Excel file

**Expected:**
- Error message: "No active trades found in Excel file"
- No tabs created
- Skipped sheets list shows all sheet names

### TC-003: Unhide sheet and reimport
**Steps:**
1. Import Excel with 1 hidden sheet
2. Unhide the sheet in Excel
3. Click "Reimport Last File"

**Expected:**
- Previously hidden sheet now imported
- New tab created for unhidden sheet
- Existing tabs remain unchanged

---

## 2. Excel Import - Trade Level

### TC-004: Active column filtering (Y/N)
**Excel data:**
```
T01 | SPY 600 CALL | Active: Y
T02 | SPY 590 PUT  | Active: N
T03 | SPY 650 CALL | Active: (blank)
T04 | SPY 580 PUT  | Active: YES
```

**Expected:**
- T01: Imported ✓
- T02: Skipped (N)
- T03: Skipped (blank = inactive)
- T04: Imported ✓ (YES accepted)
- Total: 2 trades imported

### TC-005: Combo with mixed active legs
**Excel data:**
```
T05 | 590 PUT SELL | MAIN | Active: Y
T05 | 600 PUT BUY  |      | Active: N
```

**Expected:**
- Entire combo skipped (any inactive leg = skip all)
- Warning message about T05 having inactive legs
- T05 not visible in table

### TC-006: Combo with all active legs
**Excel data:**
```
T06 | 590 PUT SELL | MAIN | Rate: 1 | QTY: 2 | Active: Y
T06 | 600 PUT BUY  |      | Rate: 1 | QTY: 2 | Active: Y
```

**Expected:**
- Combo imported successfully
- Single row in table showing T06
- Symbols column shows "SPY/SPY" or similar
- Strike column shows "Combo"
- Rate column shows "Combo"
- QTY column shows "2"

### TC-006a: Rate × QTY validation - single leg
**Excel data:**
```
T07 | 600 CALL BUY | MAIN | Rate: 3 | QTY: 2 | Target: 5.00 | Active: Y
```

**Expected:**
- Trade imported successfully
- Rate column shows "3"
- QTY column shows "2"
- When order placed, TWS receives totalQuantity = 3×2 = **6 contracts**

### TC-006b: Rate × QTY validation - combo legs with net price
**Excel data:**
```
T08 | 675 PUT SELL  | MAIN | Rate: 1 | QTY: 4 | Target: 2.70 | Alert: 2.50 | Active: Y
T08 | 670 PUT BUY   |      | Rate: 2 | QTY: 4 | Active: Y
T08 | 700 CALL SELL |      | Rate: 2 | QTY: 4 | Active: Y
```

**Expected:**
- Combo imported with 3 legs
- QTY column shows "4" (whole-trade multiplier)
- Rate column shows "Combo"
- **Net price calculation:** (675P×1) - (670P×2) + (700C×2)
  - Example: (2.10×1) - (1.50×2) + (1.80×2) = 2.10 - 3.00 + 3.60 = **$2.70**
- When Market $ reaches $2.70 and alert fires:
  - Leg 1 ratio: 1, totalQuantity: 4 → 1×4 = 4 contracts
  - Leg 2 ratio: 2, totalQuantity: 4 → 2×4 = 8 contracts
  - Leg 3 ratio: 2, totalQuantity: 4 → 2×4 = 8 contracts

### TC-006c: Rate validation - zero or negative
**Excel data:**
```
T09 | 600 CALL BUY | MAIN | Rate: 0 | QTY: 2 | Active: Y
T10 | 590 PUT SELL | MAIN | Rate: -1 | QTY: 1 | Active: Y
```

**Expected:**
- T09: Import error "Rate must be positive"
- T10: Import error "Rate must be positive"
- Neither trade imported

---

## 3. Single Leg Monitoring

### TC-007: Start monitoring - single leg
**Steps:**
1. Import single leg trade: T01 | 600 CALL | Target: 5.00 | Alert: +4.50
2. Select trade, click "Start Monitoring"

**Expected:**
- Status changes to "Monitoring"
- Market $ column starts updating with live price
- Price updates every 10 seconds

### TC-008: Alert triggers above threshold (positive)
**Setup:** T01 | 600 CALL | Target: 5.00 | Alert: +4.50

**Steps:**
1. Start monitoring
2. Wait for market price to reach 4.50 or above

**Expected:**
- Alert dialog appears when price ≥ 4.50
- Status changes to "Alerted"
- Order automatically placed at limit 5.00
- Alert only fires once (no re-fire)

### TC-009: Alert triggers below threshold (negative)
**Setup:** T02 | 590 PUT | Target: 3.00 | Alert: -3.50

**Steps:**
1. Start monitoring
2. Wait for market price to reach 3.50 or below

**Expected:**
- Alert dialog appears when price ≤ 3.50
- Status changes to "Alerted"
- Order automatically placed at limit 3.00
- Alert only fires once

### TC-010: No alert when threshold is zero
**Setup:** T03 | 580 PUT | Target: 2.00 | Alert: 0.00

**Steps:**
1. Start monitoring
2. Wait for price updates

**Expected:**
- Price updates normally
- No alert ever fires (regardless of price)
- Status remains "Monitoring"

### TC-011: Stop monitoring
**Steps:**
1. Start monitoring a trade
2. Select trade, click "Stop Monitoring"

**Expected:**
- Status changes back to "Ready"
- Market price stops updating
- No alert can fire

---

## 4. Combo Order Monitoring (Net Price)

### TC-012: Net price calculation - 2 legs (equal rates)
**Setup:**
```
T06 | 590 PUT SELL | MAIN | Rate: 1 | QTY: 3 | Target: 2.00 | Alert: +1.80
T06 | 600 PUT BUY  |      | Rate: 1 | QTY: 3 |
```

**Steps:**
1. Start monitoring
2. Observe Market $ column

**Expected:**
- Market $ = (590 PUT price × 1) - (600 PUT price × 1)
- Example: (2.10 × 1) - (0.80 × 1) = **1.30**
- Display shows net credit/debit per combo unit

### TC-013: Net price calculation - 4 legs (Iron Condor with unequal rates)
**Setup:**
```
T07 | 590 PUT  SELL | MAIN | Rate: 1 | QTY: 4 | Target: 2.00 | Alert: +1.80
T07 | 600 PUT  BUY  |      | Rate: 2 | QTY: 4 |
T07 | 650 CALL BUY  |      | Rate: 1 | QTY: 4 |
T07 | 660 CALL SELL |      | Rate: 2 | QTY: 4 |
```

**Steps:**
1. Start monitoring
2. Observe Market $ column

**Expected:**
- Market $ = (590P×1 + 660C×2) - (600P×2 + 650C×1)
- Example: (2.10×1 + 0.25×2) - (0.80×2 + 0.50×1) = (2.10 + 0.50) - (1.60 + 0.50) = **0.50**
- Net reflects actual ratio structure (1:2:1:2)

### TC-014: Alert on net combo price
**Setup:** T06 bull put spread | Rate: 1,1 | QTY: 3 | Target: 2.00 | Alert: +1.80

**Steps:**
1. Start monitoring (net price = 1.30)
2. Wait for net price to reach 1.80 or above

**Expected:**
- Alert fires when **net price ≥ 1.80** (not individual leg)
- BAG order placed: ratio=1,1 totalQty=3 limit=2.00
- All legs submitted as single combo order

### TC-015: Partial leg prices (no premature alert)
**Steps:**
1. Start monitoring 4-leg combo
2. Observe Market $ while only 2 legs have prices

**Expected:**
- Market $ stays 0.00 until **all legs** have prices
- No alert fires on incomplete data
- Once all legs priced, net calculates and displays

---

## 5. Directional Alerts

### TC-016: Edit alert threshold (positive to negative)
**Steps:**
1. Import trade with Alert: +5.00
2. Double-click Alert $ cell, change to -3.00
3. Press Enter
4. Start monitoring

**Expected:**
- Cell accepts negative value
- Display shows "-3.00"
- Alert now triggers when price ≤ 3.00 (below)

### TC-017: Edit alert threshold to zero (disable)
**Steps:**
1. Trade has Alert: +4.50
2. Edit to 0
3. Start monitoring

**Expected:**
- Alert $ shows "0.00"
- No alert ever fires
- Price monitoring continues normally

### TC-018: Multiple trades with different directions
**Setup:**
```
T01 | Alert: +5.00  (trigger above)
T02 | Alert: -3.50  (trigger below)
T03 | Alert: 0      (disabled)
```

**Steps:**
1. Start monitoring all 3
2. Wait for prices to move

**Expected:**
- T01 alerts when ≥ 5.00
- T02 alerts when ≤ 3.50
- T03 never alerts
- All work independently and correctly

---

## 6. Pagination

### TC-019: Navigate pages
**Steps:**
1. Import 35 trades into one sheet
2. Observe pagination controls

**Expected:**
- Page 1/3 displayed (15 trades per page)
- "Next ▶" button enabled
- "◀ Prev" button disabled on page 1
- Click Next → page 2 shows trades 16-30
- Click Next → page 3 shows trades 31-35
- "Next ▶" disabled on last page

### TC-020: Monitor trade on page 2
**Steps:**
1. Import 20 trades
2. Navigate to page 2
3. Start monitoring a trade on page 2
4. Navigate back to page 1

**Expected:**
- Trade on page 2 status shows "Monitoring"
- Market data subscriptions cancelled for page 1
- Navigate back to page 2 → prices resume updating
- Alert still fires even when on different page

### TC-021: Remove trade adjusts pagination
**Steps:**
1. Import 16 trades (2 pages)
2. Go to page 2 (1 trade visible)
3. Remove that trade

**Expected:**
- Current page adjusts to page 1 (last valid page)
- Trade count shows 15 trades
- Pagination shows "Page 1/1"

---

## 7. Reimport Logic

### TC-022: Reimport with duplicate trades
**Steps:**
1. Import Excel with trades T01, T02, T03
2. Click "Reimport Last File" without changing Excel

**Expected:**
- Summary: "0 new trades added (3 duplicates skipped)"
- No duplicate rows in table
- Existing trades unchanged

### TC-023: Reimport with new trades
**Steps:**
1. Import Excel with T01, T02
2. Edit Excel, add T03, T04 (keep T01, T02)
3. Reimport

**Expected:**
- Summary: "2 new trades added"
- Table now shows T01, T02, T03, T04
- T01, T02 unchanged (same position, status preserved)
- New trades added to last page

### TC-024: Reimport after removing trades
**Steps:**
1. Import T01, T02, T03
2. Remove T02 from table
3. Reimport same Excel

**Expected:**
- T02 re-imported as "new" trade
- Table shows T01, T03, T02
- T02 status is "Ready" (fresh import)

---

## 8. Order Placement

### TC-025: Manual order placement - single leg
**Steps:**
1. Import T01 | 600 CALL BUY | Target: 5.00
2. Select trade, click "Place Order"

**Expected:**
- Order appears in TWS with:
  - Symbol: SPY
  - SecType: OPT
  - Strike: 600
  - Action: BUY
  - Limit: 5.00
  - TIF: GTC
- Status changes to "Placed"

### TC-026: Manual order placement - combo
**Steps:**
1. Import 2-leg combo T06 | Target: 2.00
2. Select, click "Place Order"

**Expected:**
- BAG order appears in TWS with:
  - SecType: BAG
  - ComboLegs: 590 PUT SELL, 600 PUT BUY
  - Limit: 2.00 (net combo price)
  - All legs grouped as one order
- Status changes to "Placed"

### TC-027: Auto order on alert
**Steps:**
1. Start monitoring T01 | Alert: +4.50 | Target: 5.00
2. Wait for alert to fire

**Expected:**
- Alert dialog appears
- Order automatically placed at 5.00
- User doesn't need to click "Place Order"
- Order visible in TWS immediately

---

## 9. Remove Trades

### TC-028: Remove single trade
**Steps:**
1. Import 5 trades
2. Select T03, click "Remove Selected"

**Expected:**
- Confirmation dialog appears
- T03 removed from table
- Trade count: 4 trades
- Market data subscription cancelled for T03

### TC-029: Remove monitored trade
**Steps:**
1. Start monitoring T01
2. Select T01, click "Remove Selected"

**Expected:**
- Monitoring stopped automatically
- Trade removed from table
- No memory leaks or orphaned subscriptions

### TC-030: Remove multiple trades
**Steps:**
1. Import 10 trades
2. Select T02, T04, T07 (checkbox column)
3. Click "Remove Selected"

**Expected:**
- Confirmation shows "3 trades"
- All 3 removed
- Table adjusts (no gaps)
- Remaining trades: 7

---

## 10. Table Editing

### TC-031: Edit target price
**Steps:**
1. Import T01 | Target: 5.00
2. Double-click Target $ cell
3. Change to 6.50, press Enter

**Expected:**
- Cell accepts value
- Display shows "6.50"
- Next order placed uses 6.50 as limit

### TC-032: Edit alert threshold with sign
**Steps:**
1. Import T01 | Alert: +5.00
2. Edit to -3.50

**Expected:**
- Cell accepts negative value
- Alert semantics change to "below 3.50"
- Next monitoring session uses new threshold

### TC-033: Invalid edit (non-numeric)
**Steps:**
1. Double-click Target $ cell
2. Type "ABC", press Enter

**Expected:**
- Cell border turns red
- Edit rejected (can't commit)
- Must enter valid number or press Escape

---

## 11. Market Price Updates

### TC-034: Price update frequency
**Steps:**
1. Start monitoring a trade
2. Observe Market $ updates

**Expected:**
- Prices update every **10 seconds** (not 2 seconds)
- Timer visible in code: `Timer(10000, ...)`

### TC-035: No updates when not monitoring
**Steps:**
1. Import trades but don't start monitoring
2. Observe Market $ column

**Expected:**
- Market $ stays at 0.00
- No market data subscriptions created
- No unnecessary API calls

---

## 12. Status Display

### TC-036: Status progression
**Steps:**
1. Import trade (status: Ready)
2. Start monitoring (status: Monitoring)
3. Wait for alert (status: Alerted)
4. Order placed (status: Placed)

**Expected:**
- Status column reflects each state correctly
- Color coding (if any) updates
- Status persists after page navigation

### TC-037: Error status
**Steps:**
1. Import trade with invalid contract
2. Try to start monitoring

**Expected:**
- Status: "Error: ..."
- Error message descriptive
- Trade remains in table (not removed)

---

## 13. Multi-Sheet Management

### TC-038: Independent sheet operations
**Steps:**
1. Import Excel with 3 sheets
2. Start monitoring trades in Sheet 1
3. Switch to Sheet 2 tab
4. Remove trades in Sheet 2

**Expected:**
- Sheet 1 monitoring continues independently
- Sheet 2 removes don't affect Sheet 1
- Each tab maintains own state

### TC-039: Same Trade ID in different sheets
**Setup:**
- Sheet "SPY Trades": T01
- Sheet "QQQ Trades": T01

**Expected:**
- Both T01 trades coexist (different sheets = different namespace)
- No collision or conflict
- Each can be monitored independently

---

## 14. Edge Cases

### TC-040: Zero target price
**Steps:**
1. Import trade with Target: 0.00
2. Try to place order

**Expected:**
- Validation error (Target must be > 0)
- Order not placed
- User prompted to fix

### TC-041: Very large combo (10+ legs)
**Steps:**
1. Import combo with 10 legs
2. Start monitoring

**Expected:**
- All legs subscribed
- Net price calculates correctly
- No performance degradation
- Alert works on net price

### TC-042: Rapid page navigation
**Steps:**
1. Import 50 trades (4 pages)
2. Rapidly click Next/Prev buttons

**Expected:**
- No crashes or errors
- Market data subscriptions cleaned up properly
- UI remains responsive

---

## 15. Session Persistence

### TC-043: Reimport preserves monitoring
**Steps:**
1. Import trades, start monitoring T01
2. Reimport same file

**Expected:**
- T01 still in "Monitoring" state (not reset)
- Market data subscription continues
- Status preserved across reimport

### TC-044: Remove and reimport same trade
**Steps:**
1. Import T01, start monitoring
2. Remove T01
3. Reimport Excel with T01

**Expected:**
- T01 re-imported as fresh trade
- Status: "Ready" (not "Monitoring")
- Previous monitoring session closed

---

## Expected Error Scenarios

### E-001: Invalid Excel file
- File not found → error dialog
- Corrupted Excel → error dialog
- Wrong file type → error dialog

### E-002: Invalid contract data
- Bad symbol → error during monitoring start
- Bad expiry format → warning during import
- Bad strike → validation error

### E-003: Connection issues
- TWS disconnected → error when starting monitoring
- Lost connection during monitoring → graceful handling
- Reconnect → resume monitoring

---

## Performance Benchmarks

### P-001: Import large file
- 500 trades across 5 sheets
- Import time: < 5 seconds
- UI remains responsive

### P-002: Many simultaneous monitors
- 20 trades monitoring simultaneously
- Market data updates smooth
- No UI lag

### P-003: Page navigation
- 100 trades, 7 pages
- Page switch time: < 200ms
- Market data cleanup efficient

---

## Regression Tests (After Changes)

Run these after any code changes:

1. TC-001 (hidden sheets)
2. TC-008 (positive alert)
3. TC-009 (negative alert)
4. TC-012 (combo net price)
5. TC-014 (combo alert)
6. TC-019 (pagination)
7. TC-023 (reimport)
8. TC-026 (combo order placement)

---

## Test Data Templates

### Single Leg Test Data (with Rate)
```
Trade ID | Account | Symbol | Expiry    | Action   | Role | Strike | Rate | QTY | Target | Alert | Active
---------|---------|--------|-----------|----------|------|--------|------|-----|--------|-------|-------
T01      | DU12345 | SPY    | 20250320  | CALL BUY | MAIN | 600    | 1    | 2   | 5.00   | 4.50  | Y
T02      | DU12345 | SPY    | 20250320  | PUT BUY  | MAIN | 590    | 2    | 1   | 3.00   | -3.50 | Y
T03      | DU12345 | SPY    | 20250320  | CALL BUY | MAIN | 650    | 1    | 1   | 2.50   | 0     | Y
```

**Order quantities:**
- T01: 1×2 = 2 contracts
- T02: 2×1 = 2 contracts
- T03: 1×1 = 1 contract

### Combo Test Data (with Rate)
```
Trade ID | Account | Symbol | Expiry    | Action    | Role | Strike | Rate | QTY | Target | Alert | Active
---------|---------|--------|-----------|-----------|------|--------|------|-----|--------|-------|-------
T06      | DU12345 | SPY    | 20250320  | PUT SELL  | MAIN | 590    | 1    | 3   | 2.00   | 1.80  | Y
T06      | DU12345 | SPY    | 20250320  | PUT BUY   |      | 600    | 1    | 3   |        |       | Y
T07      | DU12345 | SPY    | 20250320  | PUT SELL  | MAIN | 590    | 1    | 4   | 2.00   | 1.80  | Y
T07      | DU12345 | SPY    | 20250320  | PUT BUY   |      | 600    | 2    | 4   |        |       | Y
T07      | DU12345 | SPY    | 20250320  | CALL BUY  |      | 650    | 1    | 4   |        |       | Y
T07      | DU12345 | SPY    | 20250320  | CALL SELL |      | 660    | 2    | 4   |        |       | Y
```

**Order quantities:**
- T06 (2-leg): QTY=3
  - 590 PUT SELL: 1×3 = 3 contracts
  - 600 PUT BUY: 1×3 = 3 contracts
- T07 (4-leg Iron Condor): QTY=4
  - 590 PUT SELL: 1×4 = 4 contracts
  - 600 PUT BUY: 2×4 = 8 contracts
  - 650 CALL BUY: 1×4 = 4 contracts
  - 660 CALL SELL: 2×4 = 8 contracts
