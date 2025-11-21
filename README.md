# ZKTeco Biometric Attendance System

A Spring Boot application for managing ZKTeco biometric devices, employee data, and attendance logs with real-time synchronization.

## Features

- **Device Management**: Add, configure, and manage multiple ZKTeco biometric devices
- **Employee Management**: Manage employee information and sync with devices
- **Real-time Attendance Sync**: Automatic synchronization of attendance data every 60 seconds
- **Manual Sync**: Push employee data to devices and pull attendance records on demand
- **Web-based UI**: Beautiful, responsive interface built with Bootstrap 5 and Thymeleaf
- **Dashboard**: View statistics and today's attendance at a glance

## Technology Stack

- **Backend**: Java 17, Spring Boot 3.3.0
- **Frontend**: HTML, CSS, Bootstrap 5, Thymeleaf
- **Database**: PostgreSQL (Production), H2 (Testing)
- **Build Tool**: Maven
- **Device SDK**: ZKTeco SDK (ZKFingerReader.jar)

## Prerequisites

- Java 17 or higher
- PostgreSQL 12 or higher
- Maven 3.6 or higher
- ZKTeco biometric device(s) on the same network

## Configuration

The application is configured to run on:
- **Server IP**: 192.168.1.109
- **Server Port**: 8081
- **Default Device IP**: 192.168.1.127
- **Default Device Port**: 4370

### Database Setup

1. Create a PostgreSQL database:
```sql
CREATE DATABASE simcaf;
CREATE USER simcaf WITH PASSWORD 'simcaf@2025';
GRANT ALL PRIVILEGES ON DATABASE simcaf TO simcaf;
```

2. Update database credentials in `src/main/resources/application.properties` if needed:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/simcaf
spring.datasource.username=simcaf
spring.datasource.password=simcaf@2025
```

### Server Configuration

To change the server IP and port, edit `src/main/resources/application.properties`:
```properties
server.address=192.168.1.109
server.port=8081
```

### ZKTeco Device Configuration

Default device settings in `src/main/resources/application.properties`:
```properties
zkteco.default.device.ip=192.168.1.127
zkteco.default.device.port=4370
zkteco.sync.enabled=true
zkteco.sync.interval=60000  # Sync interval in milliseconds (60 seconds)
```

## Building and Running

### Using Maven Wrapper (Recommended)

1. **Build the application**:
```bash
./mvnw clean package
```

2. **Run the application**:
```bash
./mvnw spring-boot:run
```

Or run the JAR file:
```bash
java -jar target/zkteco-sync-0.0.1-SNAPSHOT.jar
```

### Using Maven

1. **Build the application**:
```bash
mvn clean package
```

2. **Run the application**:
```bash
mvn spring-boot:run
```

## Accessing the Application

Once the application is running, open your web browser and navigate to:

```
http://192.168.1.109:8081/
```

## Application Features

### Dashboard
- View total devices and employees
- See today's attendance count
- Quick access to all major functions
- View recent attendance records

### Device Management
- Add new ZKTeco devices with IP, port, and location
- Edit device configurations
- Connect/disconnect devices
- View connection status and last sync time
- Delete devices

### Employee Management
- Add new employees with ID, name, department, and designation
- Edit employee information
- Manage employee active status
- Delete employees
- Associate card numbers with employees

### Manual Sync
- **Push Employee to Device**: Sync a single employee to a specific device
- **Push All Employees**: Sync all active employees to a device
- **Pull Attendance**: Manually retrieve attendance records from a device

### Attendance Logs
- View all attendance records
- See employee details, punch time, and verification method
- Track which device recorded each punch
- Monitor sync timestamps

## Real-time Synchronization

The application automatically syncs attendance data from all active devices every 60 seconds using a custom TCP/IP implementation of the ZKTeco protocol. 

### How It Works:
1. **Automatic Sync**: The scheduler connects to each active device and retrieves new attendance records
2. **Connection Pooling**: Device connections are cached for efficiency
3. **Duplicate Prevention**: Records are checked against existing data to prevent duplicates
4. **Employee Matching**: Attendance records are matched to employees by employee ID
5. **Data Parsing**: Raw device data is parsed into structured attendance logs

### Configuration:
- Enable/disable automatic sync in `application.properties` with `zkteco.sync.enabled`
- Adjust the sync interval with `zkteco.sync.interval` (default: 60000ms = 60 seconds)
- Monitor sync status and last sync time on the dashboard

### Protocol Details:
The application uses direct TCP/IP socket communication with ZKTeco devices on port 4370 (default). It implements:
- Device connection handshake with session management
- Command/response packet structure with checksums
- Attendance data retrieval and parsing
- Employee data push to devices

## Network Requirements

- The application server must be on the same network as the ZKTeco devices
- Ensure firewalls allow communication on the configured ports
- Default ZKTeco device port is 4370 (configurable per device)

## Testing

Run the test suite:
```bash
./mvnw test
```

The tests use an in-memory H2 database and don't require a running PostgreSQL instance.

## Troubleshooting

### Database Connection Issues
- Verify PostgreSQL is running: `sudo systemctl status postgresql`
- Check database credentials in `application.properties`
- Ensure the database and user exist

### Device Connection Issues
- Verify the device is powered on and connected to the network
- Check the IP address and port are correct (default port: 4370)
- Ensure firewall rules allow TCP communication on the device port
- Test connectivity: `ping <device-ip>`
- Check device logs - the application now uses real TCP/IP communication
- Ensure the device firmware supports network communication (not just USB)
- Verify the device is not in sleep mode or standby

### Application Won't Start
- Check if port 8081 is already in use
- Verify Java 17 or higher is installed: `java -version`
- Check application logs for detailed error messages

## Project Structure

```
src/main/java/com/egfs/bio_new/
├── controller/         # Web controllers (Device, Employee, Sync, Home)
├── entity/            # JPA entities (Device, Employee, AttendanceLog)
├── repository/        # Spring Data JPA repositories
├── service/           # Business logic services
├── sdk/               # ZKTeco device SDK implementation
│   ├── ZKTecoDevice.java      # TCP/IP device communication
│   └── AttendanceRecord.java  # Data transfer object
└── BioNewApplication.java  # Main application class

src/main/resources/
├── templates/         # Thymeleaf HTML templates
│   ├── fragments/    # Shared layout fragments
│   ├── devices/      # Device management pages
│   ├── employees/    # Employee management pages
│   ├── attendance/   # Attendance listing page
│   └── sync/         # Manual sync page
└── application.properties  # Application configuration

lib/
└── ZKFingerReader.jar  # Legacy fingerprint SDK (not used for network devices)
```

## API Endpoints

### Web Pages
- `GET /` - Dashboard
- `GET /devices` - Device list
- `GET /devices/new` - Add device form
- `GET /devices/edit/{id}` - Edit device form
- `GET /employees` - Employee list
- `GET /employees/new` - Add employee form
- `GET /employees/edit/{id}` - Edit employee form
- `GET /sync` - Manual sync page
- `GET /attendance` - Attendance logs

### Actions
- `POST /devices/save` - Create device
- `POST /devices/update/{id}` - Update device
- `GET /devices/delete/{id}` - Delete device
- `GET /devices/connect/{id}` - Connect to device
- `GET /devices/disconnect/{id}` - Disconnect from device
- `POST /sync/employee-to-device` - Push employee to device
- `POST /sync/all-employees-to-device` - Push all employees to device
- `POST /sync/attendance-from-device` - Pull attendance from device

## License

This project is proprietary software.

## Support

For issues and questions, please contact the development team.
