# QuickServ - Deployment & Testing Guide

## For BNY Mellon Code Divas Judges

This guide explains how to build, deploy, and test QuickServ locally.

---

## System Requirements
- **Java**: JDK 17 or higher
- **Database**: MySQL 8.0 or higher (running locally)
- **Build Tool**: Maven 3.6+ (included as wrapper)
- **Disk Space**: ~500MB
- **RAM**: 2GB minimum

---

## Step 1: Prepare Database

### Option A: Auto-Creation (Recommended)
The app creates tables automatically on first run. Ensure MySQL is running:

```bash
mysql -u root -p
```

```sql
CREATE DATABASE IF NOT EXISTS quickserv;
EXIT;
```

### Option B: Manual Setup
Import the database schema:

```bash
mysql -u root -p quickserv < database-setup.sql
```

---

## Step 2: Configure Application

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/quickserv
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD
spring.jpa.hibernate.ddl-auto=update
```

---

## Step 3: Build the Application

### Windows (PowerShell)
```powershell
cd C:\Users\MOHAN\OneDrive\Desktop\project-service\quickserv
.\mvnw.cmd clean package -DskipTests
```

### Windows (Command Prompt)
```cmd
mvnw.cmd clean package -DskipTests
```

### Linux/Mac
```bash
./mvnw clean package -DskipTests
```

**Expected Output:**
```
[INFO] BUILD SUCCESS
[INFO] Total time: XX.XXs
```

---

## Step 4: Run the Application

### Using Maven
```bash
.\mvnw.cmd spring-boot:run
```

### Using JAR File (after build)
```bash
java -jar target/quickserv-0.0.1-SNAPSHOT.jar
```

### Expected Startup Output
```
[INFO] Started QuickservApplication in XX.XXX seconds
[INFO] Tomcat started on port(s): 8080
```

---

## Step 5: Access the Application

**URL**: `http://localhost:8080`

You should see:
- QuickServ home page with login/register options
- Navigation menu
- Service browsing capability

---

## Testing the Core Flow

### 1. Register as Customer
1. Click **Register**
2. Fill in: Name, Email, Password, Location
3. Select **Role: CUSTOMER**
4. Click **Register**
5. ✅ Should redirect to login

### 2. Register as Provider
1. Click **Register**
2. Fill in: Name, Email, Password, Location
3. Select **Role: PROVIDER**
4. Select **Service Type** (e.g., Cleaning, Plumbing)
5. Click **Register**
6. ✅ Should redirect to login

### 3. Login & Browse
1. Login with a customer account
2. Click **Browse Services** or **Dashboard**
3. ✅ Should see available services
4. Click on a service to view details
5. ✅ Should see provider name, price, description

### 4. Provider Dashboard
1. Login with a provider account
2. Go to **Provider Dashboard**
3. Click **Add Service**
4. Fill: Title, Description, Price, Category
5. Click **Save**
6. ✅ Service appears in provider's service list

### 5. Admin Dashboard
1. Login with admin account (or ask for credentials)
2. Access `/admin/dashboard`
3. ✅ Should show system statistics

---

## Features Demonstrated

✅ **Authentication**
- Registration with password validation
- Secure login
- Role-based access (Customer/Provider/Admin)
- Session management

✅ **Service Management**
- Browse available services
- View service details
- Add new services (provider)
- Edit/delete services (provider)

✅ **User Experience**
- Clean, modern UI with dark theme
- Responsive design (mobile-friendly)
- Password visibility toggle
- Error handling with user messages

✅ **Database Integration**
- MySQL with proper relationships
- Auto table creation via JPA
- User role management
- Service categorization

---

## Stopping the Application

Press `CTRL + C` in the terminal where the app is running.

---

## Troubleshooting

### Issue: MySQL Connection Failed
**Solution**: Ensure MySQL is running and connection details are correct in `application.properties`

### Issue: Port 8080 Already in Use
**Solution**: Change port in `application.properties`:
```properties
server.port=8081
```

### Issue: Build Fails with Java Version Error
**Solution**: Ensure Java 17+ is installed:
```bash
java -version
```

### Issue: Database Tables Not Created
**Solution**: Manually run `database-setup.sql` or restart app with `ddl-auto=create`

---

## Project Structure

```
quickserv/
├── src/main/java/
│   └── com/quickserv/quickserv/
│       ├── config/         → Security config
│       ├── controller/     → API endpoints
│       ├── entity/         → Database models
│       ├── repository/     → Data access
│       └── service/        → Business logic
├── src/main/resources/
│   ├── templates/          → HTML pages
│   ├── static/css/         → Styling
│   └── application.properties
└── pom.xml                 → Maven config
```

---

## Support

For issues during testing, check:
1. MySQL is running and reachable
2. Java 17+ is installed
3. Port 8080 is not blocked
4. Application logs for specific error messages

---

**Last Updated**: March 8, 2026  
**For**: BNY Mellon Code Divas Competition

