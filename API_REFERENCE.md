# Booking Module API Reference

## Base URL
```
http://localhost:8080/api/bookings
```

## Authentication
All endpoints require an active user session. User must be logged in.

## Response Format

### Success Response (2xx)
```json
{
    "success": true,
    "message": "Operation successful",
    "data": { /* response data */ }
}
```

### Error Response (4xx, 5xx)
```json
{
    "success": false,
    "message": "Error description"
}
```

---

## Customer Endpoints

### 1. Create Booking
**Endpoint**: `POST /api/bookings/create`

**Required Headers**: 
- Content-Type: application/json

**Request Body**:
```json
{
    "serviceId": 1,
    "bookingDateTime": "2026-03-25T14:00:00",
    "paymentMethod": "UPI",
    "customerNotes": "Optional notes",
    "addonIds": [1, 2],
    "couponCode": "SAVE10",
    "serviceCharge": 0.00
}
```

**Query Parameters**: None

**Success Response (201)**:
```json
{
    "success": true,
    "message": "Booking created successfully",
    "data": {
        "id": 1,
        "customerId": 5,
        "customerName": "John Doe",
        "customerEmail": "john@example.com",
        "providerId": 2,
        "providerName": "Electrician Pro",
        "serviceId": 1,
        "serviceName": "Electrical Wiring",
        "bookingDateTime": "2026-03-25T14:00:00",
        "status": "PENDING",
        "totalAmount": 500.00,
        "paymentMethod": "UPI",
        "createdAt": "2026-03-21T10:00:00"
    }
}
```

**Error Cases**:
- 400: Invalid service ID, past date, payment method required
- 404: Service not found
- 401: User not logged in

---

### 2. Get My Bookings
**Endpoint**: `GET /api/bookings/my-bookings`

**Query Parameters**: None

**Success Response (200)**:
```json
{
    "success": true,
    "message": "Bookings retrieved successfully",
    "data": [
        { /* booking object */ },
        { /* booking object */ }
    ]
}
```

---

### 3. Get Bookings by Status
**Endpoint**: `GET /api/bookings/my-bookings/status/{status}`

**Path Parameters**:
- `status`: PENDING, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED, REJECTED, RESCHEDULED

**Success Response (200)**:
```json
{
    "success": true,
    "message": "Bookings retrieved successfully",
    "data": [
        { /* booking objects with matching status */ }
    ]
}
```

**Error Cases**:
- 400: Invalid status value

---

### 4. Get Upcoming Bookings
**Endpoint**: `GET /api/bookings/upcoming`

**Query Parameters**: None

**Success Response (200)**:
```json
{
    "success": true,
    "message": "Upcoming bookings retrieved successfully",
    "data": [
        { /* future booking objects */ }
    ]
}
```

---

### 5. Get Booking Details
**Endpoint**: `GET /api/bookings/{bookingId}`

**Path Parameters**:
- `bookingId`: ID of the booking (long)

**Success Response (200)**:
```json
{
    "success": true,
    "message": "Booking details retrieved successfully",
    "data": { /* complete booking object */ }
}
```

**Error Cases**:
- 404: Booking not found
- 403: You don't have permission to view this booking

---

### 6. Cancel Booking
**Endpoint**: `POST /api/bookings/{bookingId}/cancel`

**Path Parameters**:
- `bookingId`: ID of the booking to cancel

**Request Body**: None

**Success Response (200)**:
```json
{
    "success": true,
    "message": "Booking cancelled successfully",
    "data": { /* updated booking object with status CANCELLED */ }
}
```

**Error Cases**:
- 400: Cannot cancel completed booking, already cancelled
- 404: Booking not found
- 403: Unauthorized

---

### 7. Reschedule Booking
**Endpoint**: `POST /api/bookings/{bookingId}/reschedule`

**Path Parameters**:
- `bookingId`: ID of the booking to reschedule

**Query Parameters**:
- `newDateTime`: New booking date/time (ISO 8601 format, e.g., 2026-03-26T10:00:00)

**Request Body**: None

**Success Response (200)**:
```json
{
    "success": true,
    "message": "Booking rescheduled successfully",
    "data": { /* updated booking object with new datetime */ }
}
```

**Error Cases**:
- 400: New date must be in future, provider unavailable at new time, cannot reschedule completed/cancelled
- 404: Booking not found
- 403: Unauthorized

---

## Provider Endpoints

### 1. Get All Provider Bookings
**Endpoint**: `GET /api/bookings/provider/all`

**Authorization**: Must have PROVIDER role

**Query Parameters**: None

**Success Response (200)**:
```json
{
    "success": true,
    "message": "Provider bookings retrieved successfully",
    "data": [
        { /* booking objects */ }
    ]
}
```

---

### 2. Get Upcoming Bookings
**Endpoint**: `GET /api/bookings/provider/upcoming`

**Authorization**: Must have PROVIDER role

**Success Response (200)**:
```json
{
    "success": true,
    "message": "Upcoming bookings retrieved successfully",
    "data": [
        { /* future booking objects */ }
    ]
}
```

---

### 3. Get Today's Bookings
**Endpoint**: `GET /api/bookings/provider/today`

**Authorization**: Must have PROVIDER role

**Success Response (200)**:
```json
{
    "success": true,
    "message": "Today's bookings retrieved successfully",
    "data": [
        { /* today's booking objects */ }
    ]
}
```

---

### 4. Update Booking Status
**Endpoint**: `POST /api/bookings/{bookingId}/update-status`

**Path Parameters**:
- `bookingId`: ID of the booking

**Authorization**: Must have PROVIDER role

**Request Body**:
```json
{
    "status": "CONFIRMED",
    "providerNotes": "Confirmed and will arrive at 2 PM",
    "actualAmount": null
}
```

**Valid Status Values**:
- PENDING
- CONFIRMED
- IN_PROGRESS
- COMPLETED
- CANCELLED
- REJECTED
- RESCHEDULED

**Success Response (200)**:
```json
{
    "success": true,
    "message": "Booking status updated successfully",
    "data": { /* updated booking object */ }
}
```

**Error Cases**:
- 400: Invalid status format
- 404: Booking not found
- 403: Unauthorized (not the booking provider)

---

### 5. Add Notes to Booking
**Endpoint**: `POST /api/bookings/{bookingId}/add-notes`

**Path Parameters**:
- `bookingId`: ID of the booking

**Query Parameters**:
- `notes`: Notes text (required, URL-encoded)

**Request Body**: None

**Success Response (200)**:
```json
{
    "success": true,
    "message": "Notes added successfully",
    "data": { /* updated booking object */ }
}
```

**Error Cases**:
- 404: Booking not found
- 403: Unauthorized

---

## Admin Endpoints

### 1. Get All Bookings
**Endpoint**: `GET /api/bookings/admin/all`

**Authorization**: Must have ADMIN role

**Query Parameters**: None

**Success Response (200)**:
```json
{
    "success": true,
    "message": "All bookings retrieved successfully",
    "data": [
        { /* all booking objects from platform */ }
    ]
}
```

---

## Booking Object Structure

```json
{
    "id": 1,
    "customerId": 5,
    "customerName": "John Doe",
    "customerEmail": "john@example.com",
    "providerId": 2,
    "providerName": "Electrician Pro",
    "providerEmail": "provider@example.com",
    "serviceId": 1,
    "serviceName": "Electrical Wiring",
    "serviceDescription": "Professional electrical wiring installation",
    "servicePrice": 500.00,
    "priceUnit": "per visit",
    "bookingDateTime": "2026-03-25T14:00:00",
    "customerNotes": "Please arrive on time",
    "providerNotes": "Confirmed for 2 PM",
    "status": "CONFIRMED",
    "totalAmount": 500.00,
    "discountAmount": 0.00,
    "paymentMethod": "UPI",
    "addons": ["Free Consultation", "Warranty"],
    "couponCode": null,
    "createdAt": "2026-03-21T10:00:00",
    "updatedAt": "2026-03-21T10:15:00"
}
```

---

## Payment Methods

Valid values for `paymentMethod`:
- `CARD` - Credit/Debit Card
- `UPI` - Unified Payments Interface
- `WALLET` - Digital Wallet
- `CASH` - Cash on Service

---

## Status Lifecycle

```
PENDING → CONFIRMED → IN_PROGRESS → COMPLETED
                   ↘ REJECTED
                   ↘ CANCELLED
                   ↘ RESCHEDULED
```

---

## Error Codes

| HTTP Code | Message | Cause |
|-----------|---------|-------|
| 201 | Booking created successfully | Booking created |
| 200 | Operation successful | All GET/POST successful |
| 400 | Bad Request | Validation error, invalid status, past date, etc. |
| 401 | Unauthorized | User not logged in |
| 403 | Forbidden | Insufficient permissions |
| 404 | Not Found | Resource doesn't exist |
| 500 | Internal Server Error | Unexpected error |

---

## Rate Limiting
No rate limiting implemented (future enhancement)

## Pagination
No pagination implemented (future enhancement)

## Filtering & Sorting
- Status filtering: Use `/my-bookings/status/{status}`
- Time filtering: Use `/upcoming` for future bookings
- Advanced filtering: Can be added in future versions

---

## Example cURL Commands

### Create Booking
```bash
curl -X POST http://localhost:8080/api/bookings/create \
  -H "Content-Type: application/json" \
  -d '{
    "serviceId": 1,
    "bookingDateTime": "2026-03-25T14:00:00",
    "paymentMethod": "UPI",
    "customerNotes": "Please arrive on time"
  }' \
  --cookie "JSESSIONID=your_session_id"
```

### Get My Bookings
```bash
curl -X GET http://localhost:8080/api/bookings/my-bookings \
  --cookie "JSESSIONID=your_session_id"
```

### Update Booking Status
```bash
curl -X POST http://localhost:8080/api/bookings/1/update-status \
  -H "Content-Type: application/json" \
  -d '{
    "status": "CONFIRMED",
    "providerNotes": "All set!"
  }' \
  --cookie "JSESSIONID=your_session_id"
```

### Reschedule Booking
```bash
curl -X POST "http://localhost:8080/api/bookings/1/reschedule?newDateTime=2026-03-26T10:00:00" \
  --cookie "JSESSIONID=your_session_id"
```

---

## Testing

### Using Postman
1. Import this API reference
2. Set environment variable: `baseUrl` = http://localhost:8080
3. Set `sessionId` = your actual JSESSIONID
4. Add Cookie header: `JSESSIONID={{sessionId}}`

### Using cURL
Always include `--cookie "JSESSIONID=your_session_id"` with requests

### Using JavaScript Fetch
```javascript
const options = {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include', // Include cookies
    body: JSON.stringify(data)
};
fetch('http://localhost:8080/api/bookings/create', options)
    .then(res => res.json())
    .then(data => console.log(data));
```

---

## Changelog

### Version 1.0 (Initial Release)
- Booking creation with validation
- Customer booking management
- Provider booking management
- Admin overview
- Payment method support
- Conflict detection
- Status tracking
- Notes and comments

