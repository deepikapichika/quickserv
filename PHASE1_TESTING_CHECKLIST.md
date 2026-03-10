# Phase 1 Testing Checklist: Booking System

## Prerequisites
- [ ] Root module compiles cleanly (`mvnw clean compile` returns success)
- [ ] MySQL database is running and connected
- [ ] Application can start without errors
- [ ] User has registered as CUSTOMER and PROVIDER

---

## Customer Booking Flow Tests

### Test 1: Browse and Book Service
**Objective**: Customer can find a service and create a booking

Steps:
1. [ ] Login as CUSTOMER user
2. [ ] Navigate to `/browse` (Customer Dashboard → Browse Services)
3. [ ] Click on a service card or search for a service
4. [ ] Click "Book Now" or similar CTA button
5. [ ] Verify page loads with service details
6. [ ] Select a booking date/time (must be in future)
7. [ ] Add optional notes in the notes field
8. [ ] Click "Confirm Booking"
9. [ ] Verify success message appears
10. [ ] Verify redirect to `/bookings/customer`

**Expected Results**:
- [ ] Booking form displays service price, provider name, and description
- [ ] Date/time input is present and functional
- [ ] Submit creates booking in database
- [ ] No errors in browser console

---

### Test 2: Customer View Bookings
**Objective**: Customer can see their bookings and their status

Steps:
1. [ ] Navigate to `/bookings/customer`
2. [ ] Verify list of customer's bookings appears
3. [ ] Check that each booking card shows:
   - [ ] Service title
   - [ ] Provider name
   - [ ] Booking date & time
   - [ ] Amount/price
   - [ ] Status badge (PENDING, CONFIRMED, etc.)
4. [ ] If booking is PENDING, verify "Cancel" button is present
5. [ ] Click "View Details" for a booking
6. [ ] Verify booking detail page loads all information

**Expected Results**:
- [ ] All bookings are listed in reverse creation order
- [ ] Status badges are color-coded appropriately
- [ ] Customer cannot see other customers' bookings
- [ ] Details page is read-only with all information visible

---

### Test 3: Cancel Booking
**Objective**: Customer can cancel a PENDING booking

Steps:
1. [ ] Navigate to `/bookings/customer`
2. [ ] Find a booking with PENDING status
3. [ ] Click "Cancel" button on that booking
4. [ ] Confirm the cancel action in the popup dialog
5. [ ] Verify status changes to CANCELLED
6. [ ] Verify "Cancel" button is no longer shown for that booking

**Expected Results**:
- [ ] Status updates immediately on page (refresh if needed)
- [ ] Cannot cancel COMPLETED or other non-cancellable statuses
- [ ] Booking remains visible in history (not deleted)

---

## Provider Booking Management Tests

### Test 4: Provider View Bookings
**Objective**: Provider can see all bookings they received

Steps:
1. [ ] Login as PROVIDER user
2. [ ] Navigate to `/bookings/provider`
3. [ ] Verify dashboard loads with stats cards showing:
   - [ ] Total Bookings count
   - [ ] Pending Bookings count
   - [ ] Today's Bookings count
   - [ ] Upcoming Bookings count
4. [ ] Verify booking list appears below stats
5. [ ] Each booking card should show:
   - [ ] Customer name and email
   - [ ] Service title
   - [ ] Booking date & time
   - [ ] Amount
   - [ ] Status badge

**Expected Results**:
- [ ] Stats cards show correct numbers (count matches list)
- [ ] Only bookings for this provider are shown
- [ ] Bookings are ordered by creation date (newest first)
- [ ] No errors on page load

---

### Test 5: Update Booking Status
**Objective**: Provider can update booking status and add notes

Steps:
1. [ ] Navigate to `/bookings/provider`
2. [ ] Find a PENDING booking
3. [ ] On that booking card, select status "CONFIRMED" from dropdown
4. [ ] Optionally add notes in text area
5. [ ] Click "Update Status" button
6. [ ] Verify status changes to CONFIRMED
7. [ ] Verify notes are saved if added
8. [ ] Repeat for other status transitions

**Expected Results**:
- [ ] Status updates immediately (refresh page to confirm persistence)
- [ ] Notes are saved with the status update
- [ ] Provider cannot update another provider's bookings
- [ ] Status changes appear in customer's view

---

### Test 6: Status Transition Flow
**Objective**: Verify provider can transition through booking lifecycle

Steps:
1. [ ] Start with a PENDING booking
2. [ ] Change to CONFIRMED
3. [ ] Verify change, then change to IN_PROGRESS
4. [ ] Verify change, then change to COMPLETED
5. [ ] Verify booking appears as COMPLETED

**Expected Results**:
- [ ] All transitions work correctly
- [ ] Cannot go backwards in status
- [ ] Cannot perform invalid transitions
- [ ] Each status correctly reflected in customer view

---

## Data Integrity Tests

### Test 7: Database Table Creation
**Objective**: Verify `bookings` table is created with correct schema

Steps:
1. [ ] Open MySQL CLI or GUI (e.g., MySQL Workbench)
2. [ ] Connect to `quickserv` database
3. [ ] Run: `SHOW TABLES;`
4. [ ] Verify `bookings` table exists
5. [ ] Run: `DESCRIBE bookings;`
6. [ ] Verify columns exist:
   - [ ] `id` (INT, PRIMARY KEY, AUTO_INCREMENT)
   - [ ] `customer_id` (INT, FOREIGN KEY → users)
   - [ ] `provider_id` (INT, FOREIGN KEY → users)
   - [ ] `service_id` (INT, FOREIGN KEY → services)
   - [ ] `booking_date_time` (DATETIME, NOT NULL)
   - [ ] `customer_notes` (VARCHAR(1000))
   - [ ] `provider_notes` (VARCHAR(1000))
   - [ ] `status` (VARCHAR, DEFAULT 'PENDING')
   - [ ] `total_amount` (DECIMAL)
   - [ ] `created_at` (DATETIME)
   - [ ] `updated_at` (DATETIME)

**Expected Results**:
- [ ] Table exists with all required columns
- [ ] Foreign keys are correctly defined
- [ ] Data types are appropriate
- [ ] Constraints are in place

---

### Test 8: Sample Data Check
**Objective**: Verify booking data is correctly stored and retrieved

Steps:
1. [ ] Create a booking through the UI (Test 1)
2. [ ] In MySQL, run: `SELECT * FROM bookings;`
3. [ ] Verify new booking record exists
4. [ ] Check all fields are populated correctly:
   - [ ] customer_id matches logged-in customer
   - [ ] provider_id matches service provider
   - [ ] service_id matches selected service
   - [ ] booking_date_time is what customer selected
   - [ ] status is 'PENDING'
   - [ ] total_amount matches service price
   - [ ] created_at and updated_at are current time

**Expected Results**:
- [ ] Data is correctly persisted
- [ ] No NULL values in required fields
- [ ] Relationships are consistent
- [ ] Amounts are accurate

---

## Authorization & Security Tests

### Test 9: Customer Cannot Access Provider Routes
**Objective**: Verify customer cannot access provider booking routes

Steps:
1. [ ] Login as CUSTOMER
2. [ ] Try to navigate to `/bookings/provider` directly
3. [ ] Verify user is redirected to login or dashboard
4. [ ] Verify error message if applicable

**Expected Results**:
- [ ] Access is denied
- [ ] User is not allowed to see other provider's bookings

---

### Test 10: Provider Cannot Cancel Customer Bookings
**Objective**: Verify authorization on cancel operation

Steps:
1. [ ] Get booking ID of a customer's booking
2. [ ] Login as a different PROVIDER (or use browser dev tools)
3. [ ] Try to POST to `/bookings/cancel/{bookingId}`
4. [ ] Verify authorization error occurs

**Expected Results**:
- [ ] Request is rejected
- [ ] Provider gets "Unauthorized" error

---

## UI/UX Tests

### Test 11: Responsive Design
**Objective**: Verify pages work on mobile and desktop

Steps:
1. [ ] Test booking form on desktop (1920px width)
2. [ ] Test booking form on tablet (768px width)
3. [ ] Test booking form on mobile (375px width)
4. [ ] Verify all elements are readable and clickable

**Expected Results**:
- [ ] Layout adapts to screen size
- [ ] Form inputs are usable on all devices
- [ ] Tables/lists are readable with overflow on small screens

---

## Error Handling Tests

### Test 12: Booking Conflict Prevention
**Objective**: Verify system prevents double-booking

Steps:
1. [ ] Create a booking for a provider at a specific time
2. [ ] Create another booking for the same provider at same time
3. [ ] Verify error message: "Provider is not available at this time"

**Expected Results**:
- [ ] Second booking is rejected
- [ ] Error message is clear
- [ ] Original booking remains unchanged

---

### Test 13: Invalid Input Handling
**Objective**: Verify form validation

Steps:
1. [ ] Navigate to booking form
2. [ ] Try to submit without selecting a date
3. [ ] Verify error occurs (browser validation)
4. [ ] Try to select a date in the past
5. [ ] Verify error occurs

**Expected Results**:
- [ ] Form validation prevents invalid submissions
- [ ] Error messages are helpful

---

## Performance Tests

### Test 14: Large Booking List
**Objective**: Verify performance with many bookings

Steps:
1. [ ] Create 20+ bookings for a provider
2. [ ] Load `/bookings/provider`
3. [ ] Verify page loads in reasonable time (< 3 seconds)
4. [ ] Verify all bookings are listed
5. [ ] Try to update a booking status

**Expected Results**:
- [ ] No performance degradation
- [ ] Page remains responsive
- [ ] Queries are optimized

---

## Test Results Summary

| Test # | Test Name | Status | Notes |
|--------|-----------|--------|-------|
| 1 | Browse and Book Service | ⬜ | |
| 2 | Customer View Bookings | ⬜ | |
| 3 | Cancel Booking | ⬜ | |
| 4 | Provider View Bookings | ⬜ | |
| 5 | Update Booking Status | ⬜ | |
| 6 | Status Transition Flow | ⬜ | |
| 7 | Database Table Creation | ⬜ | |
| 8 | Sample Data Check | ⬜ | |
| 9 | Customer Cannot Access Provider Routes | ⬜ | |
| 10 | Provider Cannot Cancel Customer Bookings | ⬜ | |
| 11 | Responsive Design | ⬜ | |
| 12 | Booking Conflict Prevention | ⬜ | |
| 13 | Invalid Input Handling | ⬜ | |
| 14 | Large Booking List | ⬜ | |

✅ = Passed | ⚠️ = Warning/Issue | ❌ = Failed | ⬜ = Not Tested

---

## Notes & Issues Found

```
[Add any issues, bugs, or observations here during testing]
```

---

**Test Date**: ________________
**Tester Name**: ________________
**Status**: Ready for Phase 2? [ ] YES [ ] NO

If NO, please describe blockers:
```
[Describe any blocking issues here]
```

