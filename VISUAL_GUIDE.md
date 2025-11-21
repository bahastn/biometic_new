# Visual Guide - ZKTeco Attendance System UI

## Application Screenshots

### 1. Dashboard (Home Page)
**URL**: http://192.168.1.109:8081/

**Features Visible**:
- 4 Statistics Cards (gradient backgrounds):
  - Total Devices (purple gradient)
  - Total Employees (green gradient)
  - Today's Punches (pink gradient)
  - System Status (blue gradient)
- Quick Action Buttons:
  - Add Device
  - Add Employee
  - Manual Sync
  - View All Attendance
- Today's Attendance Table (latest 10 records)
- Sidebar Navigation:
  - Dashboard (active)
  - Devices
  - Employees
  - Attendance
  - Manual Sync

---

### 2. Device Management
**URL**: http://192.168.1.109:8081/devices

**Features Visible**:
- "Add New Device" button (top right)
- Devices Table showing:
  - Device Name
  - IP Address (e.g., 192.168.1.127)
  - Port (e.g., 4370)
  - Location
  - Status Badge (Active/Inactive - green/gray)
  - Connection Badge (Connected/Disconnected - green/red with WiFi icon)
  - Last Sync Time
  - Action Buttons:
    - Connect/Disconnect (green/yellow)
    - Edit (blue)
    - Delete (red)

---

### 3. Add/Edit Device Form
**URL**: http://192.168.1.109:8081/devices/new

**Form Fields**:
- Device Name (text input)
- IP Address (text input with validation pattern)
  - Format help: "xxx.xxx.xxx.xxx (each number 0-255)"
- Port (number input, 1-65535)
- Location (text input, optional)
- Serial Number (text input, optional)
- Active checkbox

**Buttons**:
- Cancel (gray) - returns to device list
- Save Device (blue)

**Info Box**:
- Configuration help section
- Tips about network requirements
- Default port information

---

### 4. Employee Management
**URL**: http://192.168.1.109:8081/employees

**Features Visible**:
- "Add New Employee" button (top right)
- Employees Table showing:
  - Employee ID (e.g., EMP001)
  - Name
  - Department
  - Designation
  - Card Number
  - Status Badge (Active/Inactive)
  - Action Buttons:
    - Edit (blue)
    - Delete (red)

---

### 5. Add/Edit Employee Form
**URL**: http://192.168.1.109:8081/employees/new

**Form Fields**:
- Employee ID (text input, readonly after creation)
- Full Name (text input, required)
- Department (text input, optional)
- Designation (text input, optional)
- Card Number (text input, optional)
- Active checkbox

**Buttons**:
- Cancel (gray) - returns to employee list
- Save Employee (green)

**Info Box**:
- Employee ID uniqueness requirement
- Active employee sync information
- Manual sync instructions

---

### 6. Manual Sync Page
**URL**: http://192.168.1.109:8081/sync

**Three Main Sections**:

**Section 1: Push Employee to Device** (left card, blue header)
- Employee dropdown selector
- Device dropdown selector
- "Push Employee to Device" button (blue, full width)

**Section 2: Push All Employees to Device** (right card, green header)
- Device dropdown selector
- Info alert showing number of employees to sync
- "Push All Employees to Device" button (green, full width)

**Section 3: Pull Attendance from Device** (bottom card, yellow header)
- Device dropdown selector
- Warning alert about automatic sync
- "Pull Attendance from Device" button (yellow, full width)

**Info Box**:
- Explanation of each sync operation
- Automatic sync information

---

### 7. Attendance Logs
**URL**: http://192.168.1.109:8081/attendance

**Features Visible**:
- Attendance Records Table showing:
  - Row number
  - Employee ID
  - Employee Name
  - Department
  - Punch Time (YYYY-MM-DD HH:mm:ss)
  - Type Badge:
    - IN (green with arrow icon)
    - OUT (red with arrow icon)
  - Verify Mode Badge:
    - Fingerprint (blue with fingerprint icon)
    - Card (yellow with card icon)
    - Password (info blue with key icon)
  - Device Name
  - Synced At timestamp

**Info Box**:
- Explanation of fields
- Real-time sync information

---

## UI Design Elements

### Color Scheme
- **Primary**: Blue (#0d6efd)
- **Success**: Green (#38ef7d)
- **Warning**: Pink/Yellow (#f5576c)
- **Info**: Cyan (#00f2fe)
- **Sidebar**: Dark blue gradient (#1a237e to #283593)

### Typography
- Font Family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif
- Clean, modern, professional look

### Components
- **Cards**: White background, rounded corners (12px), subtle shadow
- **Badges**: Rounded (6px), colored backgrounds
- **Buttons**: Rounded (8px), various colors based on action
- **Sidebar**: Fixed, dark blue gradient, white text
- **Tables**: Hover effects, striped optional, responsive

### Icons
- Bootstrap Icons library
- Used throughout for visual clarity
- Examples:
  - 🔌 WiFi for connection status
  - 👤 Person for employees
  - 🖥️ Network for devices
  - 🕐 Clock for attendance
  - 🔄 Refresh for sync

### Responsive Design
- Mobile-friendly
- Bootstrap grid system
- Sidebar collapses on small screens
- Tables scroll horizontally on mobile

---

## User Experience Flow

### Adding a Device
1. Navigate to Devices → Click "Add New Device"
2. Fill in form (Name: "Main Entrance", IP: 192.168.1.127, Port: 4370)
3. Click "Save Device"
4. Redirected to device list with success message
5. Click "Connect" button to test connection
6. Connection status updates to "Connected"

### Adding an Employee
1. Navigate to Employees → Click "Add New Employee"
2. Fill in form (ID: EMP001, Name: John Doe, Department: IT)
3. Click "Save Employee"
4. Redirected to employee list with success message

### Syncing Employee to Device
1. Navigate to Manual Sync
2. Select employee from dropdown
3. Select device from dropdown
4. Click "Push Employee to Device"
5. Success message shows sync completed

### Viewing Attendance
1. Navigate to Attendance
2. View all punch records in table
3. See real-time updates as sync runs every 60 seconds
4. Filter by date/employee (if implemented)

---

## Flash Messages
- **Success** (green): "Device added successfully: Main Entrance"
- **Error** (red): "Failed to connect to device"
- **Info** (blue): System status messages
- Auto-dismiss with close button

---

This visual guide shows what users will see and interact with in the ZKTeco Attendance System. The interface is intuitive, professional, and provides all necessary functionality for managing biometric devices and attendance records.
