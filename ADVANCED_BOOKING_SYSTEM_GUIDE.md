# Advanced Booking Management System - Implementation Guide

## 📦 Module Structure

```
src/main/java/com/quickserv/quickserv/
├── entity/
│   ├── Booking.java (ENHANCED)
│   │   ├── BookingStatus enum (8 states)
│   │   ├── Pricing fields (base, travel, addons, coupon, GST)
│   │   ├── Lifecycle timestamps (confirmed, assigned, started, completed, cancelled, rescheduled)
│   │   ├── Refund tracking (amount, reason, status)
│   │   └── Business logic methods:
│   │       ├── calculateRefundAmount() - Cancellation policy
│   │       ├── getRefundWithProviderNoShowCredit() - No-show handling
│   │       ├── canBeRescheduled() - State validation
│   │       └── canBeCancelled() - State validation
│   │
│   ├── Coupon.java (NEW)
│   ├── Addon.java (NEW)
│   └── NotificationEvent.java (NEW)
│
├── dto/booking/ (NEW)
│   ├── BookingCreateRequest.java
│   ├── BookingResponse.java
│   ├── PricingBreakdownDto.java
│   ├── ProviderMatchDto.java
│   └── RefundCalculationDto.java
│
├── repository/
│   ├── BookingRepository.java (ENHANCED with complex queries)
│   ├── CouponRepository.java (NEW)
│   └── AddonRepository.java (NEW)
│
├── service/booking/
│   ├── PricingService.java
│   │   ├── calculateTravelCharge(distance) - ₹5/km beyond 5km
│   │   ├── calculateGST(subtotal) - 18% GST
│   │   ├── calculateTotalAmount(booking)
│   │   ├── applyPercentageDiscount()
│   │   └── applyFixedDiscount()
│   │
│   ├── ProviderAssignmentService.java
│   │   ├── Scoring algorithm (location 40% + rating 40% + availability 20%)
│   │   ├── findBestProvider() - Returns highest scored provider
│   │   ├── findEligibleProviders() - Returns sorted list
│   │   └── Hard constraints:
│   │       ├── Distance <= 10km
│   │       ├── Rating >= 3.5
│   │       └── Available time slot
│   │
│   ├── BookingService.java (ENHANCED)
│   │   ├── createBooking() - Full validation + pricing
│   │   ├── confirmBooking() - State transition
│   │   ├── assignProvider() - Auto/manual assignment
│   │   ├── cancelBooking() - Refund calculation
│   │   ├── rescheduleBooking() - Conflict detection
│   │   └── completeBooking() - Finalization
│   │
│   ├── CancellationService.java (NEW)
│   │   ├── calculateRefund() - Based on policy
│   │   ├── processCancellation() - Update booking
│   │   └── handleProviderNoShow() - ₹100 credit + full refund
│   │
│   ├── NotificationService.java (NEW)
│   │   ├── sendBookingConfirmation() - Push + Email
│   │   ├── sendStatusUpdate() - Real-time via WebSocket
│   │   └── sendReminder() - 1 hour before service
│   │
│   └── BookingAnalyticsService.java (NEW)
│       ├── getBookingStats()
│       ├── getProviderMetrics()
│       └── getCustomerMetrics()
│
├── controller/
│   ├── BookingController.java (ENHANCED)
│   │   ├── POST /api/bookings/create - Create booking
│   │   ├── POST /api/bookings/{id}/confirm - Confirm booking
│   │   ├── POST /api/bookings/{id}/cancel - Cancel booking
│   │   ├── POST /api/bookings/{id}/reschedule - Reschedule
│   │   ├── GET /api/bookings/{id}/pricing - Price breakdown
│   │   └── GET /api/bookings/analytics - Dashboard data
│   │
│   └── AdminBookingController.java (NEW)
│       ├── GET /admin/bookings/all
│       ├── POST /admin/bookings/{id}/assign-provider (manual)
│       ├── POST /admin/bookings/{id}/resolve-dispute
│       └── GET /admin/bookings/analytics
│
└── exception/
    ├── BookingException.java
    ├── InsufficientFundsException.java
    └── ProviderNotAvailableException.java
```

---

## 🏗️ Booking Lifecycle Flow

```
Customer creates booking
         ↓
PENDING (Awaiting confirmation)
         ↓
Customer confirms
         ↓
CONFIRMED (Awaiting provider assignment)
         ↓
[Auto-assign OR Manual admin assignment]
         ↓
ASSIGNED (Provider assigned)
         ↓
Provider starts work
         ↓
IN_PROGRESS
         ↓
Service completed
         ↓
COMPLETED ✓


Alternative flows:
PENDING → CANCELLED (within cancellation policy time)
  ├─ >24h before: Full refund
  ├─ 12-24h before: 50% refund
  └─ <12h before: No refund

ASSIGNED/CONFIRMED/PENDING → RESCHEDULED (new time)

ASSIGNED → IN_PROGRESS → COMPLETED → Provider no-show?
  └─ Full refund + ₹100 credit
```

---

## 💰 Pricing Calculation

```
Formula:
========
BASE_PRICE = Service catalog price
TRAVEL_CHARGE = Max(0, (distance - 5km) * ₹5/km)
ADDON_CHARGES = Sum of selected add-ons
COUPON_DISCOUNT = Applied coupon (percentage or fixed)

SUBTOTAL = BASE_PRICE + TRAVEL_CHARGE + ADDON_CHARGES - COUPON_DISCOUNT
GST (18%) = SUBTOTAL * 0.18
TOTAL = SUBTOTAL + GST

Example:
--------
Base Price:        ₹500
Travel Charge:     (8km - 5km) * ₹5 = ₹15
Add-ons:           ₹50 + ₹30 = ₹80
Coupon (10%):      -(₹500 + ₹15 + ₹80) * 0.10 = -₹59.50
Subtotal:          ₹500 + ₹15 + ₹80 - ₹59.50 = ₹535.50
GST (18%):         ₹535.50 * 0.18 = ₹96.39
TOTAL:             ₹535.50 + ₹96.39 = ₹631.89
```

---

## 🎯 Provider Scoring Algorithm

```
SCORING FORMULA:
================
Final Score = (Location Score × 0.40) + (Rating Score × 0.40) + (Availability Score × 0.20)

Location Score (0-100):
  = Max(0, (1 - distance/10km) × 100)
  = 100 if 0km away
  = 50 if 5km away
  = 0 if 10km+ away

Rating Score (0-100):
  = (Provider Rating / 5.0) × 100
  = 100 if 5.0 stars
  = 70 if 3.5 stars
  = 0 if no rating

Availability Score:
  = 100 if available
  = 0 if not available

HARD CONSTRAINTS (Must pass to be eligible):
=============================================
✓ Rating >= 3.5 stars
✓ Distance <= 10 km
✓ Available at requested time slot

EXAMPLE:
--------
Provider A: Distance 2km, Rating 4.8
  Location = (1 - 2/10) × 100 = 80
  Rating = (4.8/5) × 100 = 96
  Availability = 100
  Score = (80 × 0.40) + (96 × 0.40) + (100 × 0.20) = 32 + 38.4 + 20 = 90.4

Provider B: Distance 8km, Rating 3.8
  Location = (1 - 8/10) × 100 = 20
  Rating = (3.8/5) × 100 = 76
  Availability = 100
  Score = (20 × 0.40) + (76 × 0.40) + (100 × 0.20) = 8 + 30.4 + 20 = 58.4

Winner: Provider A with score 90.4
```

---

## 🔄 Cancellation Policy

```
Cancellation Window
===================
>24 hours before service:
  Refund: 100%
  Reason: Customer/System cancellation
  
12-24 hours before service:
  Refund: 50%
  Reason: Late cancellation charge applied
  
<12 hours before service:
  Refund: 0%
  Reason: No refund within 12 hours
  
Provider No-Show:
  Refund: 100% + ₹100 credit
  Action: Mark provider unavailable, credit customer account

Refund Flow:
  1. Calculate refund amount based on policy
  2. Mark booking as CANCELLED with refund_reason
  3. Process refund (depends on payment method)
  4. Update customer wallet/account
  5. Send cancellation notification
  6. Log refund transaction
```

---

## 📨 Notification System

```
Events & Channels:
==================

1. Booking Created
   → Email: Booking confirmation
   → Push: "Your booking is confirmed"
   → SMS: Booking ID and details

2. Provider Assigned
   → Email: Provider details and map
   → Push: "Provider assigned: John Plumber"
   → SMS: Provider phone and ETA

3. Service Started
   → Push: "Service in progress"
   → SMS: "Provider on the way"

4. Service Completed
   → Push: "Service completed"
   → Email: Invoice and receipt
   → SMS: "Thank you for booking"

5. Reminder
   → SMS: "Service in 1 hour"
   → Email: Reminder with provider details
   → Push: Service reminder

6. Cancellation
   → Email: Cancellation and refund details
   → SMS: Refund confirmation
   → Push: "Booking cancelled - refund processed"

7. Status Update (Real-time via WebSocket)
   → PENDING → CONFIRMED → ASSIGNED → IN_PROGRESS → COMPLETED
```

---

## 🔒 Rate Limiting & Concurrency Control

```
Rate Limiting:
==============
- Max 5 bookings per customer per minute
- Sliding window counter
- Prevents abuse/spam

Concurrency Control:
====================
Optimistic Locking:
  - @Version field on Booking entity
  - Prevents lost updates
  - Throws OptimisticLockException on conflict

Pessimistic Locking (for time slots):
  - Lock provider availability for requested time
  - SELECT...FOR UPDATE in database
  - Prevents double-booking

Database Indexes:
=================
- idx_customer_id: Fast customer booking lookup
- idx_provider_id: Fast provider booking lookup
- idx_status: Filter by booking status
- idx_booking_date: Range queries by date
```

---

## 🗄️ Database Schema (New Columns)

```sql
ALTER TABLE bookings ADD COLUMN base_price DECIMAL(10,2);
ALTER TABLE bookings ADD COLUMN travel_charge DECIMAL(10,2);
ALTER TABLE bookings ADD COLUMN addon_charges DECIMAL(10,2);
ALTER TABLE bookings ADD COLUMN coupon_discount DECIMAL(10,2);
ALTER TABLE bookings ADD COLUMN gst_amount DECIMAL(10,2);
ALTER TABLE bookings ADD COLUMN distance_km DECIMAL(8,2);
ALTER TABLE bookings ADD COLUMN confirmed_at TIMESTAMP;
ALTER TABLE bookings ADD COLUMN assigned_at TIMESTAMP;
ALTER TABLE bookings ADD COLUMN started_at TIMESTAMP;
ALTER TABLE bookings ADD COLUMN completed_at TIMESTAMP;
ALTER TABLE bookings ADD COLUMN cancelled_at TIMESTAMP;
ALTER TABLE bookings ADD COLUMN rescheduled_at TIMESTAMP;
ALTER TABLE bookings ADD COLUMN cancellation_notes VARCHAR(1000);
ALTER TABLE bookings ADD COLUMN refund_amount DECIMAL(10,2);
ALTER TABLE bookings ADD COLUMN is_refunded BOOLEAN DEFAULT FALSE;
ALTER TABLE bookings ADD COLUMN refund_reason VARCHAR(500);
ALTER TABLE bookings ADD COLUMN assigned_provider_score DECIMAL(5,2);
ALTER TABLE bookings ADD COLUMN version BIGINT DEFAULT 0;

CREATE INDEX idx_customer_id ON bookings(customer_id);
CREATE INDEX idx_provider_id ON bookings(provider_id);
CREATE INDEX idx_status ON bookings(status);
CREATE INDEX idx_booking_date ON bookings(booking_date_time);
```

---

## 🧪 Testing Scenarios

```
1. Booking Creation:
   - Validate all required fields
   - Calculate pricing correctly
   - Assign provider automatically
   
2. Cancellation:
   - >24h: Full refund
   - 12-24h: 50% refund
   - <12h: No refund
   - Provider no-show: Full + ₹100
   
3. Rescheduling:
   - Check provider availability
   - Recalculate distance/pricing
   - Update all timestamps
   
4. Provider Assignment:
   - Find providers within 10km
   - Filter by rating >= 3.5
   - Score and sort
   - Pick highest scorer
   
5. Notifications:
   - Verify all channels work
   - Check email/SMS content
   - Test WebSocket real-time updates
```

---

## ⚡ Performance Optimization

```
Caching Strategy:
=================
- Cache provider list by category (5 min)
- Cache service pricing (1 hour)
- Cache coupon validations (10 min)
- Cache booking analytics (15 min)

Query Optimization:
===================
- Use indexes for fast lookups
- Batch load related entities
- Avoid N+1 query problems
- Use database pagination for large result sets

Async Operations:
=================
- Send notifications asynchronously
- Process refunds in background
- Generate reports in batch
- Update analytics non-blocking
```

---

## 📊 Admin Analytics Dashboard

```
Metrics to Track:
=================
1. Total Bookings: Count by status
2. Revenue: Sum of all completed bookings
3. Cancellation Rate: Cancelled / Total
4. Average Rating: Provider satisfaction
5. Top Providers: By bookings/revenue
6. Peak Hours: When most bookings occur
7. Customer Retention: Repeat customers
8. Average Response Time: Provider assignment speed
9. Refunds Issued: Refund analysis
10. Service Completion Rate: Completed / Assigned

Charts & Reports:
=================
- Daily/Weekly/Monthly bookings
- Revenue trends
- Provider performance
- Customer demographics
- Service category popularity
- Peak demand times
- Customer satisfaction trends
- Refund analysis
```

---

## 🔐 Security Considerations

```
Access Control:
===============
- Customers can only see their own bookings
- Providers can only see assigned bookings
- Admins can see all bookings
- Role-based authorization on all endpoints

Data Protection:
================
- Encrypt sensitive payment info
- Hash payment tokens
- Audit log all refund transactions
- Mask customer phone numbers in logs
- Secure WebSocket connections (WSS)
- CSRF protection on all forms

Validation:
===========
- Server-side validation (not just client)
- Validate user ownership
- Validate booking state transitions
- Validate coupon codes
- Validate distance calculations
```

---

## 📝 Implementation Checklist

- [ ] Enhance Booking entity with new fields
- [ ] Create PricingService
- [ ] Create ProviderAssignmentService  
- [ ] Create CancellationService
- [ ] Create NotificationService
- [ ] Create BookingService (enhance)
- [ ] Create DTOs for requests/responses
- [ ] Create API endpoints (BookingController)
- [ ] Create admin endpoints
- [ ] Implement database indexes
- [ ] Write unit tests
- [ ] Write integration tests
- [ ] Setup WebSocket for real-time updates
- [ ] Implement rate limiting
- [ ] Setup email/SMS providers
- [ ] Add caching layer
- [ ] Create admin dashboard
- [ ] Documentation
- [ ] Deploy to staging
- [ ] Performance testing
- [ ] Security testing

---

## 🚀 Deployment Strategy

```
Phase 1 (Week 1-2):
- Core booking with PENDING/CONFIRMED states
- Basic pricing (base price only)
- Simple provider assignment (first available)

Phase 2 (Week 3-4):
- Advanced pricing (travel charges, GST, coupons)
- Intelligent provider scoring
- Cancellation policy implementation

Phase 3 (Week 5-6):
- Real-time WebSocket notifications
- Email/SMS integrations
- Admin analytics dashboard

Phase 4 (Week 7-8):
- Performance optimization
- Security audit
- Load testing
- Production deployment
```

This architecture provides a scalable, maintainable booking system that handles all specified requirements while following Spring Boot best practices.

