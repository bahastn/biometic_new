# ZKTeco Device Integration - Implementation Guide

## Overview
This document explains the changes made to enable real device communication with ZKTeco biometric attendance devices.

## Problem
The original implementation used placeholder code that simulated device connections without actual network communication. The `ZKFingerReader.jar` SDK is designed for USB fingerprint scanners, not network-based attendance terminals.

## Solution
Implemented a custom ZKTeco protocol handler that communicates with devices over TCP/IP using the ZKTeco proprietary protocol.

## Key Components

### 1. ZKTecoDevice Class (`src/main/java/com/egfs/bio_new/sdk/ZKTecoDevice.java`)
A custom implementation of the ZKTeco network protocol that:
- Establishes TCP socket connections to devices on port 4370
- Implements the ZKTeco command/response protocol with proper checksums
- Handles session management and reply sequencing
- Provides methods for:
  - `connect()` - Establish connection to device
  - `disconnect()` - Close connection
  - `getAttendanceRecords()` - Retrieve punch records from device
  - `setUser()` - Push employee information to device
  - `disableDevice()` / `enableDevice()` - Control device state during operations

### 2. AttendanceRecord Class (`src/main/java/com/egfs/bio_new/sdk/AttendanceRecord.java`)
Data transfer object for parsing attendance records from devices with fields:
- `userId` - Employee ID
- `punchTime` - Timestamp of the punch
- `verifyType` - Method used (0=Password, 1=Fingerprint, 2=Card, 3=Face)
- `inOutState` - Type of punch (0=Check-in, 1=Check-out, etc.)

### 3. Updated ZKTecoService (`src/main/java/com/egfs/bio_new/service/ZKTecoService.java`)
Enhanced service layer that:
- Maintains a thread-safe cache of device connections (ConcurrentHashMap)
- Connects to real devices instead of simulating connections
- Syncs attendance data with duplicate prevention
- Pushes employee data to devices with proper validation
- Includes comprehensive error handling and logging

### 4. Database Updates
- Changed `AttendanceLog.verifyMode` from Integer to String for better readability
- Added `existsByEmployeeAndPunchTimeAndDevice()` method to prevent duplicate records

## Protocol Details

### Connection Flow
1. Open TCP socket to device IP on port 4370
2. Send CONNECT command (1000) with proper packet structure
3. Receive session ID from device
4. Use session ID for all subsequent commands

### Packet Structure
```
[Start Marker (2 bytes)] [Device ID (2 bytes)] [Session ID (2 bytes)] 
[Reply Number (2 bytes)] [Command ID (2 bytes)] [Checksum (2 bytes)]
[Data (variable length)]
```

### Data Encoding
- All multi-byte values use LITTLE_ENDIAN byte order
- Checksums calculated over entire packet except checksum field itself
- Employee IDs must be numeric (converted to int)
- Names limited to 27 characters (28 bytes with null terminator)

## How to Test

### Prerequisites
1. Ensure ZKTeco device is powered on and connected to network
2. Verify device IP (default: 192.168.1.127) and port (default: 4370)
3. Test network connectivity: `ping 192.168.1.127`
4. Ensure firewall allows TCP traffic on port 4370

### Testing Steps

#### 1. Start the Application
```bash
./mvnw spring-boot:run
```

#### 2. Add a Device
- Navigate to http://192.168.1.109:8081/devices/new
- Enter device details:
  - Name: Main Entrance
  - IP Address: 192.168.1.127
  - Port: 4370
  - Location: Building A
- Click "Save Device"

#### 3. Test Connection
- On the devices list page, click "Connect" button
- Check logs for connection success:
  ```
  Successfully connected to device. Session ID: [number]
  ```
- Device status should show as "Connected"

#### 4. Add Employees
- Navigate to http://192.168.1.109:8081/employees/new
- Add employee with:
  - Employee ID: 1 (must be numeric)
  - Name: John Doe
  - Card Number: 12345 (optional, numeric)
- Click "Save Employee"

#### 5. Push Employee to Device
- Navigate to http://192.168.1.109:8081/sync
- Select device and employee
- Click "Push Employee to Device"
- Check logs for success message

#### 6. Test Attendance Sync
- Punch on the physical device (fingerprint/card/password)
- Wait for automatic sync (runs every 60 seconds) OR
- Navigate to sync page and click "Pull Attendance from Device"
- Check attendance logs page for new records

#### 7. Monitor Real-time Sync
- Check application logs for scheduled sync:
  ```
  Starting scheduled attendance sync...
  Syncing attendance from device: Main Entrance
  Retrieved X attendance records from device
  Successfully synced Y new attendance records
  ```

### Troubleshooting

#### Connection Fails
- **Symptom**: "Failed to connect" or timeout errors
- **Solutions**:
  - Verify device IP: `ping 192.168.1.127`
  - Check device port (should be 4370)
  - Ensure device is not in sleep mode
  - Check firewall rules on both server and device
  - Verify device supports network communication (not USB-only model)

#### No Attendance Records
- **Symptom**: Sync completes but no records appear
- **Solutions**:
  - Verify employees are registered on device (push employees first)
  - Check employee IDs match between application and device
  - Ensure attendance records exist on device
  - Check logs for parsing errors

#### Employee Push Fails
- **Symptom**: "Failed to set user" errors
- **Solutions**:
  - Ensure employee ID is numeric (e.g., "1", "100", not "EMP001")
  - Verify device has available user slots
  - Check device is not in admin-only mode
  - Try pushing with shorter name (max 27 characters)

## Performance Considerations

### Connection Pooling
Connections are cached in memory to avoid reconnecting for each operation. The cache uses ConcurrentHashMap for thread-safe access.

### Sync Interval
Default sync interval is 60 seconds. Adjust via `zkteco.sync.interval` property:
```properties
zkteco.sync.interval=30000  # 30 seconds
```

### Duplicate Prevention
Attendance records are checked against existing data using employee, punch time, and device. This prevents the same record from being imported multiple times.

## Security

### CodeQL Analysis
✅ No security vulnerabilities detected

### Network Security
- Device communication is unencrypted (ZKTeco protocol limitation)
- Ensure devices are on a secure internal network
- Use firewall rules to restrict access to device ports
- Consider VPN for remote device access

## Monitoring

### Application Logs
Monitor logs for:
- Connection status: `Successfully connected to device`
- Sync activity: `Syncing attendance from device`
- Errors: `Error connecting to device`, `Error syncing attendance`

### Database Queries
Check recent attendance records:
```sql
SELECT e.name, a.punch_time, a.punch_type, a.verify_mode, d.device_name
FROM attendance_logs a
JOIN employees e ON a.employee_id = e.id
JOIN devices d ON a.device_id = d.id
ORDER BY a.punch_time DESC
LIMIT 10;
```

## Next Steps

### Production Deployment
1. Configure production database credentials
2. Set up proper network infrastructure
3. Configure firewalls for device communication
4. Set up monitoring and alerting
5. Create backup strategy for attendance data

### Enhanced Features (Future)
- Multiple device simultaneous sync
- Fingerprint template management
- Advanced attendance reports
- Mobile app integration
- Real-time push notifications

## References

### ZKTeco Protocol
- Protocol implementation based on reverse-engineered ZKTeco communication
- Tested with ZKTeco attendance devices (e.g., K40, F18, etc.)
- Compatible with most ZKTeco network-enabled devices on port 4370

### Support
For issues related to:
- Device connectivity: Check device manual for network settings
- Protocol implementation: Review ZKTecoDevice.java source code
- Application errors: Check application logs and Spring Boot configuration
