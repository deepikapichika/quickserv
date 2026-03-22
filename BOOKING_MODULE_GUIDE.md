# Booking Management Module - Implementation Guide

## Overview
This Booking Management Module seamlessly integrates with your existing QuickServe project to provide comprehensive booking functionality for customers and providers.

## What's New

### 1. Enhanced Booking Entity
**File**: `Booking.java`

**New Fields Added**:
- `PaymentMethod paymentMethod` - Enum for payment methods (CARD, UPI, WALLET, CASH)
- `String addonIds` - Comma-separated IDs of selected add-ons
- `String couponCode` - Applied coupon/promo code
- `BigDecimal discountAmount` - Discount applied through coupon

**New Status**: `RESCHEDULED` added to BookingStatus enum

### 2. New Payment Method Enum
**File**: `PaymentMethod.java`

Supports four payment methods:
- CARD (Credit/Debit Card)
- UPI (Unified Payments Interface)
- WALLET (Digital Wallet)
- CASH (Cash on Service)

### 3. Enhanced BookingService
**File**: `BookingService.java`

**New Methods Added**:

#### Customer-Centric Methods:
- `createBooking(User customer, BookingCreateRequest request)` - Create booking with full validation
- `getCustomerBookings(User customer)` - Get all bookings for a customer
- `getCustomerBookingsByStatus(User customer, BookingStatus status)` - Filter by status
- `getCustomerUpcomingBookings(User customer)` - Get future bookings only
- `cancelBooking(Long bookingId, User customer)` - Allow customer to cancel
- `rescheduleBooking(Long bookingId, LocalDateTime newDateTime, User customer)` - Reschedule with conflict checking

#### Provider Methods:
- `updateBookingStatus(Long bookingId, BookingStatus status, User provider)` - Update status
- `updateBookingStatus(Long bookingId, BookingStatus status, String providerNotes, User provider)` - Update with notes
- `addProviderNotes(Long bookingId, String notes, User provider)` - Add provider notes

**Key Features**:
- Automatic conflict detection (provider availability checking)
- Transaction management with `@Transactional`
- Proper exception handling with custom exceptions
- Total amount calculation (extensible for addons and coupons)
- Backward compatible with legacy method signature

### 4. Enhanced BookingRepository
**File**: `BookingRepository.java`

**New Query Methods**:
- `findByCustomerAndStatusOrderByBookingDateTimeDesc()` - Get customer bookings by status
- `findUpcomingBookingsForCustomer()` - Future bookings for customer
- `findByCouponCode()` - Find bookings by coupon code

### 5. BookingController
**File**: `BookingController.java`

Comprehensive REST API endpoints:

#### Customer Endpoints:
```
POST   /api/bookings/create              - Create new booking
GET    /api/bookings/my-bookings         - Get all customer bookings
GET    /api/bookings/my-bookings/status/{status} - Filter by status
GET    /api/bookings/upcoming            - Get upcoming bookings
GET    /api/bookings/{bookingId}         - Get booking details
POST   /api/bookings/{bookingId}/cancel  - Cancel booking
POST   /api/bookings/{bookingId}/reschedule?newDateTime=... - Reschedule
```

#### Provider Endpoints:
```
GET    /api/bookings/provider/all        - Get all provider bookings
GET    /api/bookings/provider/upcoming   - Get upcoming bookings
GET    /api/bookings/provider/today      - Get today's bookings
POST   /api/bookings/{bookingId}/update-status - Update booking status
POST   /api/bookings/{bookingId}/add-notes - Add provider notes
```

#### Admin Endpoints:
```
GET    /api/bookings/admin/all           - Get all platform bookings
```

### 6. DTOs

#### BookingCreateRequest.java
Request DTO for creating bookings with validation:
- serviceId (required)
- bookingDateTime (required, must be future)
- paymentMethod (required: CARD, UPI, WALLET, CASH)
- customerNotes (optional, max 1000 chars)
- addonIds (optional list)
- couponCode (optional, alphanumeric pattern)
- serviceCharge (optional)

#### BookingResponse.java
Rich response DTO with complete booking details:
- Customer and provider information
- Service details (name, price, description)
- Booking status and payment info
- Total amount, discount, coupon details
- Timestamps

#### BookingStatusUpdateRequest.java
Provider request to update booking status:
- status (required: PENDING, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED, REJECTED, RESCHEDULED)
- providerNotes (optional)
- actualAmount (optional, for billing adjustments)

#### AddonDto.java
DTO for service add-ons/extras:
- id, name, description
- price (required, non-negative)
- serviceId

#### CouponDto.java
DTO for coupon/promo code management:
- code (required, uppercase alphanumeric)
- description
- discountType (PERCENTAGE or FIXED)
- discountValue
- maxDiscount (for percentage)
- minOrderAmount
- validFrom, validUntil
- usageLimit, usedCount
- isActive

## Integration Points

### Database Migrations
If using migrations, you may need to add these columns to the `bookings` table:

```sql
ALTER TABLE bookings ADD COLUMN payment_method VARCHAR(20);
ALTER TABLE bookings ADD COLUMN addon_ids VARCHAR(500);
ALTER TABLE bookings ADD COLUMN coupon_code VARCHAR(50);
ALTER TABLE bookings ADD COLUMN discount_amount DECIMAL(10, 2) DEFAULT 0;
```

### Security & Authorization
All endpoints check user session:
- Customers can only access their own bookings
- Providers can only update/view their own bookings
- Admins have full access
- Role-based validation on all endpoints

### Error Handling
Uses custom exceptions:
- `BusinessValidationException` - Validation failures (400 Bad Request)
- `ResourceNotFoundException` - Resource not found (404 Not Found)
- Centralized error responses with consistent JSON format

### Conflict Detection
Automatic detection when:
- Provider is already booked at selected time
- Prevents double-booking with 1-hour duration assumption
- Can be extended to check actual service duration

## Usage Examples

### 1. Create a Booking (Customer)

```javascript
// Frontend JavaScript
const bookingData = {
    serviceId: 123,
    bookingDateTime: "2026-03-25T14:00:00",
    paymentMethod: "UPI",
    customerNotes: "Please arrive on time",
    addonIds: [1, 2],
    couponCode: "SAVE10"
};

fetch('/api/bookings/create', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(bookingData)
})
.then(res => res.json())
.then(data => {
    if (data.success) {
        console.log("Booking created:", data.data);
    } else {
        alert(data.message);
    }
});
```

### 2. Get Customer Bookings

```javascript
fetch('/api/bookings/my-bookings')
    .then(res => res.json())
    .then(data => console.log(data.data));
```

### 3. Update Booking Status (Provider)

```javascript
const statusUpdate = {
    status: "CONFIRMED",
    providerNotes: "Confirmed for March 25, 2026 at 2:00 PM"
};

fetch('/api/bookings/123/update-status', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(statusUpdate)
})
.then(res => res.json())
.then(data => console.log(data.data));
```

### 4. Reschedule Booking

```javascript
fetch('/api/bookings/123/reschedule?newDateTime=2026-03-26T10:00:00', {
    method: 'POST'
})
.then(res => res.json())
.then(data => console.log(data.data));
```

## Future Enhancements (TODOs)

1. **Addon Price Calculation**
   - Fetch actual addon prices from database
   - Add addon prices to total amount calculation

2. **Coupon Validation & Application**
   - Validate coupon code exists and is active
   - Check minimum order amount requirement
   - Apply percentage or fixed discounts
   - Track coupon usage count

3. **Payment Integration**
   - Razorpay/Stripe integration for CARD and UPI
   - Wallet payment processing
   - Payment confirmation webhooks

4. **Notifications**
   - Email confirmation after booking
   - SMS reminders before service
   - Provider notification on new booking
   - Status change notifications

5. **Reviews & Ratings**
   - Allow customers to rate services after completion
   - Provider rating calculation
   - Review management

6. **Analytics**
   - Booking success rate metrics
   - Provider utilization analytics
   - Revenue reporting

## Testing

The module has been tested for:
- ✅ Compilation success
- ✅ Entity relationships
- ✅ Service layer validation
- ✅ Transaction management
- ✅ Authorization checks
- ✅ Error handling

To run tests:
```bash
mvn test
```

## Backward Compatibility

The module maintains backward compatibility:
- Legacy `createBooking(User, ServiceListing, LocalDateTime, String)` method preserved
- Existing booking endpoints continue to work
- New PaymentMethod enum defaults appropriately
- All new fields are nullable in database

## File Summary

| File | Purpose |
|------|---------|
| Booking.java | Enhanced entity with payment & addon support |
| PaymentMethod.java | Payment method enum |
| BookingService.java | Business logic with customer/provider methods |
| BookingRepository.java | Enhanced JPA queries |
| BookingController.java | REST API endpoints |
| BookingCreateRequest.java | Booking creation DTO |
| BookingResponse.java | Booking response DTO |
| BookingStatusUpdateRequest.java | Status update DTO |
| AddonDto.java | Service addon DTO |
| CouponDto.java | Coupon/promo DTO |
| ServiceService.java | Enhanced with getServiceByIdAsOptional |

## Support & Questions

For integration questions or issues, refer to:
1. BookingController.java for API documentation
2. BookingService.java for business logic
3. DTOs for request/response validation rules
4. Exception classes for error handling

