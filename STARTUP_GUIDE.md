# QuickServ Application Startup Guide

## ⚠️ CRITICAL: Before Running the Application

### Step 1: Start MySQL Server

**Option A: Using XAMPP (Easiest)**
1. Open XAMPP Control Panel
2. Click "Start" next to MySQL
3. Wait for "Running" status

**Option B: Using Command Prompt**
```bash
# For MySQL installed via installer:
net start mysql80

# Or check status:
netstat -ano | findstr :3306
```

**Option C: Using MySQL Workbench**
- Open MySQL Workbench
- Click the "play" button to start the server

**Verify MySQL is running:**
```bash
mysql -u root -p -e "SELECT 1"
# When prompted, enter password: root
```

### Step 2: Create Database

```sql
-- Run this in MySQL command line or phpMyAdmin:
CREATE DATABASE IF NOT EXISTS quickserv;
USE quickserv;

-- Create tables (if not auto-created):
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    role ENUM('CUSTOMER', 'PROVIDER', 'ADMIN') DEFAULT 'CUSTOMER',
    location VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    icon_url VARCHAR(500)
);

-- Insert sample data
INSERT INTO categories (name, description, icon_url) VALUES
('Electrician', 'Electrical services and repairs', '⚡'),
('Plumber', 'Plumbing services and repairs', '🔧'),
('AC Repair', 'Air conditioning services', '❄️'),
('Beautician', 'Beauty and grooming services', '💄'),
('Tutor', 'Educational tutoring services', '📚');
```

### Step 3: Run the Application

```bash
# Navigate to project directory
cd C:\Users\MOHAN\OneDrive\Desktop\project-service\quickserv

# Run the application
mvn spring-boot:run
```

### Step 4: Access the Application

Wait for message: **"Started QuickservApplication in X.XXX seconds"**

Then open in browser:
- **Homepage**: http://localhost:8080
- **Login**: http://localhost:8080/login
- **Register**: http://localhost:8080/register

## 🔧 Troubleshooting

### Error: "Failed to start bean 'webServerStartStop'"

**Cause**: Usually MySQL is not running or database doesn't exist

**Fix**:
1. Ensure MySQL is running: `netstat -ano | findstr :3306`
2. Create database: `CREATE DATABASE quickserv;`
3. Restart the application

### Error: "Access denied for user 'root'@'localhost'"

**Fix**: Check MySQL password in application.properties
- Default: `root`
- If different, update `spring.datasource.password=your_password`

### Error: "Connection timeout"

**Cause**: MySQL is not accessible

**Fix**:
1. Stop any other applications using port 3306
2. Restart MySQL service
3. Increase timeout: Already configured to 10 seconds

### Error: "Port 8080 already in use"

**Fix**: 
```bash
# Kill process using port 8080
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Or change port in application.properties:
server.port=8081
```

## ✅ Success Checklist

- [ ] MySQL is running on port 3306
- [ ] Database 'quickserv' is created
- [ ] Application starts without errors
- [ ] Console shows "Tomcat initialized with port 8080"
- [ ] Homepage loads at http://localhost:8080

## 📝 Test Data

Login with:
- **Email**: john@example.com
- **Password**: password

