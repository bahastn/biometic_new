# Deployment Guide

## Quick Start

### Step 1: Database Setup

1. Install PostgreSQL (if not already installed):
```bash
sudo apt update
sudo apt install postgresql postgresql-contrib
```

2. Create database and user:
```bash
sudo -u postgres psql
```

Then in the PostgreSQL console:
```sql
CREATE DATABASE simcaf;
CREATE USER simcaf WITH PASSWORD 'simcaf@2025';
GRANT ALL PRIVILEGES ON DATABASE simcaf TO simcaf;
\q
```

### Step 2: Configure Network

Ensure your server has the IP address configured:
```bash
# Check current IP
ip addr show

# If needed, configure static IP 192.168.1.109
# Edit /etc/netplan/00-installer-config.yaml or your network config
```

### Step 3: Build and Run

1. Build the application:
```bash
cd /path/to/biometic_new
./mvnw clean package -DskipTests
```

2. Run the application:
```bash
java -jar target/zkteco-sync-0.0.1-SNAPSHOT.jar
```

Or use Maven directly:
```bash
./mvnw spring-boot:run
```

### Step 4: Access the Application

Open your browser and navigate to:
```
http://192.168.1.109:8081/
```

### Step 5: Configure Devices

1. Click on "Devices" in the sidebar
2. Click "Add New Device"
3. Fill in the device details:
   - Device Name: e.g., "Main Entrance"
   - IP Address: 192.168.1.127 (or your device's IP)
   - Port: 4370 (default ZKTeco port)
   - Location: e.g., "Ground Floor"
4. Click "Save Device"
5. Click the "Connect" button to test the connection

### Step 6: Add Employees

1. Click on "Employees" in the sidebar
2. Click "Add New Employee"
3. Fill in employee details:
   - Employee ID: e.g., "EMP001"
   - Full Name: e.g., "John Doe"
   - Department: e.g., "IT"
   - Designation: e.g., "Developer"
   - Card Number: (optional) RFID card number
4. Click "Save Employee"

### Step 7: Sync Employees to Device

1. Go to "Manual Sync" page
2. Select an employee and a device
3. Click "Push Employee to Device"

Or push all employees at once:
1. Select a device
2. Click "Push All Employees to Device"

## Running as a Service (Linux)

Create a systemd service file `/etc/systemd/system/zkteco-sync.service`:

```ini
[Unit]
Description=ZKTeco Biometric Attendance System
After=network.target postgresql.service

[Service]
Type=simple
User=your-user
WorkingDirectory=/path/to/biometic_new
ExecStart=/usr/bin/java -jar /path/to/biometic_new/target/zkteco-sync-0.0.1-SNAPSHOT.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

Enable and start the service:
```bash
sudo systemctl daemon-reload
sudo systemctl enable zkteco-sync
sudo systemctl start zkteco-sync
sudo systemctl status zkteco-sync
```

View logs:
```bash
sudo journalctl -u zkteco-sync -f
```

## Firewall Configuration

If using UFW:
```bash
sudo ufw allow 8081/tcp
sudo ufw reload
```

If using firewalld:
```bash
sudo firewall-cmd --permanent --add-port=8081/tcp
sudo firewall-cmd --reload
```

## Production Recommendations

1. **Use a reverse proxy (nginx/Apache)**:
   - Add SSL/TLS for HTTPS
   - Set up proper domain name
   - Configure load balancing if needed

2. **Database**:
   - Use a strong password (not the default)
   - Configure regular backups
   - Enable PostgreSQL authentication

3. **Application**:
   - Configure JVM heap size for production
   - Set up application monitoring
   - Configure logging levels appropriately

4. **Security**:
   - Add authentication and authorization
   - Use environment variables for sensitive data
   - Keep the system and dependencies updated

## Environment Variables

You can override configuration using environment variables:

```bash
export SERVER_ADDRESS=192.168.1.109
export SERVER_PORT=8081
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/simcaf
export SPRING_DATASOURCE_USERNAME=simcaf
export SPRING_DATASOURCE_PASSWORD=your_secure_password
export ZKTECO_SYNC_ENABLED=true
export ZKTECO_SYNC_INTERVAL=60000

java -jar target/zkteco-sync-0.0.1-SNAPSHOT.jar
```

## Troubleshooting

### Port Already in Use
```bash
# Find process using port 8081
sudo lsof -i :8081
# Kill the process if needed
sudo kill -9 <PID>
```

### Database Connection Error
```bash
# Check PostgreSQL status
sudo systemctl status postgresql
# Check PostgreSQL logs
sudo tail -f /var/log/postgresql/postgresql-*.log
```

### Device Connection Error
```bash
# Test network connectivity
ping 192.168.1.127
# Check if device port is accessible
telnet 192.168.1.127 4370
```

## Support

For issues and questions, check the application logs:
- Console output when running with Maven
- `/var/log/zkteco-sync.log` if configured
- `journalctl -u zkteco-sync` if running as service
