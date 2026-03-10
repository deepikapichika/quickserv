# 🚀 Quick Start Guide: Testing Phase 1 Booking System

## Prerequisites (5 minutes)

### 1. Stop Any Running Instance
If you have the application running, stop it:
```bash
# Press Ctrl+C in the terminal
```

### 2. Verify MySQL is Running
```bash
# Check MySQL status
mysql -u root -p

# If connected, verify database
USE quickserv;
SHOW TABLES;

# Exit MySQL
EXIT;
```

### 3. Clean Build (Optional but Recommended)
```bash
cd "C:\Users\MOHAN\OneDrive\Desktop\project-service\quickserv"
.\mvnw.cmd clean
```

---

## Start the Application (2 minutes)

### From Command Line
```bash
cd "C:\Users\MOHAN\OneDrive\Desktop\project-service\quickserv"
.\mvnw.cmd spring-boot:run
```

### From IntelliJ IDEA
1. Open `quickserv` project (the ROOT folder)
2. Find `QuickservApplication.java` in `src/main/java/com/quickserv/quickserv/`
3. Right-click → Run 'QuickservApplication'
4. Wait for: `Started QuickservApplication in X seconds`

### Application Ready When You See
```
Started QuickservApplication in 8.234 seconds (JVM running for 8.876)
```

Visit: **http://localhost:8080**

---

## Setup Test Accounts (5 minutes)

### Create CUSTOMER Account
1. Navigate to **http://localhost:8080/register**
2. Fill in:
   - **Name**: John Customer
   - **Email**: customer@test.com
   - **Password**: TestPass@123
   - **Phone**: 9876543210
   - **Role**: CUSTOMER
   - **Location**: Bangalore
3. Click Register
4. **Login with this account** (you'll use it for Test 1-3)

### Create PROVIDER Account
1. Navigate to **http://localhost:8080/register**
2. Fill in:
   - **Name**: Sarah Provider
   - **Email**: provider@test.com
   - **Password**: TestPass@123
   - **Phone**: 9876543211
   - **Role**: PROVIDER
   - **Location**: Bangalore
   - **Service Type**: Salon and Beauty
3. Click Register
4. **Don't logout yet** (you'll use this for Test 4-6)

---

## Quick Test Flow (15 minutes)

### Test 1: Create a Booking (Customer)
```
1. Login as customer@test.com / TestPass@123
2. Click on "Browse Services" from dashboard
3. Find any service (e.g., "Hair Cut")
4. Click on the service card or "Book Now"
5. Select a date/time in the future
6. Add optional notes
7. Click "Confirm Booking"
✅ Expected: Redirected to /bookings/customer with booking listed
```

### Test 2: View Provider Bookings (Provider)
```
1. Logout (top right corner)
2. Login as provider@test.com / TestPass@123
3. Navigate to /bookings/provider
4. Should see dashboard stats:
   - Total Bookings: 1 (or more)
   - Pending: 1 (or more)
   - Today's Bookings: (if booked for today)
   - Upcoming: (if booked for future)
5. Should see the booking from Test 1
✅ Expected: Booking appears in provider's list
```

### Test 3: Update Booking Status (Provider)
```
1. Still logged in as provider
2. Find the booking from Test 1
3. Select status "CONFIRMED" from dropdown
4. Add notes like "Confirmed, see you soon!"
5. Click "Update Status"
6. Verify status changes to CONFIRMED
✅ Expected: Booking status updates immediately
```

### Test 4: Verify in Customer View
```
1. Logout
2. Login as customer@test.com
3. Go to /bookings/customer
4. Find your booking
5. Verify:
   - Status shows "CONFIRMED"
   - Provider notes visible
✅ Expected: Customer sees updated status and notes
```

### Test 5: Verify Database
```
1. Open MySQL CLI or MySQL Workbench
2. Run: USE quickserv;
3. Run: SELECT * FROM bookings;
4. Verify:
   - 1 row exists
   - customer_id = customer's user ID
   - provider_id = provider's user ID
   - status = 'CONFIRMED'
✅ Expected: Data correctly stored in database
```

---

## Common Issues & Solutions

### Issue: "Application failed to start"
**Solution**:
```bash
# Check MySQL connection
# Verify database user/password in application.properties
# Restart MySQL service
```

### Issue: "Cannot find service to book"
**Solution**:
1. Services are seeded on first startup
2. If not visible, check MySQL:
   ```sql
   SELECT * FROM services LIMIT 5;
   ```
3. If empty, restart application and wait for seed data

### Issue: "Booking form shows but submit fails"
**Solution**:
1. Check browser console (F12) for errors
2. Check application logs for exceptions
3. Verify:
   - Date/time is in future
   - Service exists in database
   - Provider has availability

### Issue: "Cannot see provider bookings"
**Solution**:
1. Verify you're logged in as PROVIDER
2. Role should show as "PROVIDER" in database
3. Check that at least 1 booking exists for this provider

---

## Quick Database Checks

### Check if tables exist:
```sql
SHOW TABLES;
-- Should include: users, services, categories, bookings
```

### Check bookings table structure:
```sql
DESCRIBE bookings;
-- Should show all columns as described in PHASE1_IMPLEMENTATION_REPORT.md
```

### View all bookings:
```sql
SELECT * FROM bookings;
```

### View specific booking details:
```sql
SELECT b.*, u_customer.name as customer_name, u_provider.name as provider_name, s.title as service_title
FROM bookings b
JOIN users u_customer ON b.customer_id = u_customer.id
JOIN users u_provider ON b.provider_id = u_provider.id
JOIN services s ON b.service_id = s.id;
```

---

## Key URLs for Testing

| Feature | URL |
|---------|-----|
| Home | http://localhost:8080 |
| Register | http://localhost:8080/register |
| Login | http://localhost:8080/login |
| Browse Services | http://localhost:8080/browse |
| Service Detail | http://localhost:8080/service/{id} |
| Booking Form | http://localhost:8080/bookings/new/{serviceId} |
| Customer Bookings | http://localhost:8080/bookings/customer |
| Provider Bookings | http://localhost:8080/bookings/provider |
| Booking Detail | http://localhost:8080/bookings/{id} |

---

## Expected Test Duration

| Activity | Time |
|----------|------|
| Prerequisites Setup | 5 min |
| Start Application | 2 min |
| Create Test Accounts | 5 min |
| Run Quick Test Flow | 15 min |
| Database Verification | 5 min |
| **Total** | **~30 minutes** |

---

## Success Criteria

You'll know Phase 1 is working when:

✅ Customer can create a booking
✅ Provider can see the booking
✅ Provider can update booking status
✅ Customer sees updated status
✅ Data persists in MySQL database
✅ No errors in application logs

---

## After Testing

### If All Tests Pass ✅
1. Document your test results in `PHASE1_TESTING_CHECKLIST.md`
2. Confirm all 14 test scenarios pass
3. You're ready for **Phase 2 (Reviews System)**

### If Issues Found ❌
1. Check error logs in application console
2. Review `PHASE1_IMPLEMENTATION_REPORT.md` for technical details
3. Verify database connectivity
4. Check user roles and permissions
5. Try clean build: `mvnw clean compile`

---

## Useful Debug Commands

### View application logs
```bash
# In terminal where app is running, look for any ERROR or WARN messages
```

### Enable detailed logging
Add to `src/main/resources/application.properties`:
```properties
logging.level.com.quickserv.quickserv=DEBUG
logging.level.org.springframework.web=DEBUG
```

### Check if port 8080 is in use
```bash
# Windows
netstat -ano | findstr :8080

# Kill process if needed
taskkill /PID {PID} /F
```

---

## Need Help?

1. **Check Compilation**: `mvnw -DskipTests compile`
2. **Check MySQL**: `mysql -u root -p < [check connection]`
3. **Read Docs**: `PHASE1_IMPLEMENTATION_REPORT.md` has full technical details
4. **Review Code**: Check BookingController.java for route logic

---

**Ready to Start?** → Run `mvnw spring-boot:run` and visit http://localhost:8080

