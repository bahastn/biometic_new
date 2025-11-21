# Quick Start Guide - Real Device Integration

## What Was Fixed

Previously, the application showed devices as "Connected" but **didn't actually communicate with them**. Now it performs **REAL device communication**.

## What Changed

### Before (Placeholder Code)
```java
// OLD: Just returned true without connecting
boolean connected = true; // PLACEHOLDER
```

### After (Real Implementation)
```java
// NEW: Actual TCP socket connection
socket = new Socket(ipAddress, port);
byte[] cmd = createCommand(CMD_CONNECT, new byte[0]);
out.write(cmd);
// ... actual protocol implementation
```

## How to Verify It Works

### 1. Start Application
```bash
cd /home/runner/work/biometic_new/biometic_new
./mvnw spring-boot:run
```

### 2. Access Web Interface
Open browser: `http://192.168.1.109:8081`

### 3. Add Your Device
- Click "Devices" → "Add New Device"
- Enter:
  - Device Name: Main Door
  - IP Address: 192.168.1.127
  - Port: 4370
  - Location: Building A
- Click "Save"

### 4. Test Real Connection
- Click "Connect" button next to your device
- **Watch the logs** - you should see:
  ```
  Connecting to ZKTeco device at 192.168.1.127:4370
  Successfully connected to device. Session ID: [number]
  ```
- **Connection badge** will turn green: "Connected"

### 5. Add Employee
- Click "Employees" → "Add New Employee"
- Enter:
  - Employee ID: 1 (must be number)
  - Name: John Doe
  - Card Number: 12345 (optional)
- Click "Save"

### 6. Push to Device (Real Operation)
- Click "Manual Sync"
- Select your device and employee
- Click "Push Employee to Device"
- **This actually sends data to the physical device!**

### 7. Test Attendance Sync (Real-time)
- **Punch on the physical device** (fingerprint/card)
- Wait 60 seconds for automatic sync, OR
- Click "Pull Attendance from Device"
- Check "Attendance Logs" - you'll see the **actual punch data**!

## What to Expect

### Successful Connection
```
✅ Device status shows "Connected" (green badge)
✅ Last sync time updates
✅ Application logs show connection details
✅ You can push employees to device
✅ Attendance records sync from device
```

### Connection Fails
```
❌ Device status shows "Disconnected" (red badge)
❌ Logs show timeout or connection errors
❌ Possible causes:
   - Device not powered on
   - Wrong IP address
   - Network firewall blocking port 4370
   - Device in sleep mode
```

## Troubleshooting

### Can't Connect?
1. **Ping the device**: `ping 192.168.1.127`
2. **Check device IP** on device menu
3. **Verify port** (should be 4370)
4. **Check firewall** on server:
   ```bash
   sudo ufw allow 4370/tcp
   ```

### No Attendance Records?
1. **Push employees first** (device needs employee data)
2. **Punch on device** to create records
3. **Check employee IDs match** (must be numeric: 1, 2, 3...)
4. **Wait for sync** or manually trigger it

### Employee Push Fails?
1. **Employee ID must be numeric** (use "1" not "EMP001")
2. **Device must be connected** (green badge)
3. **Name too long?** (max 27 characters)

## Key Features Now Working

✅ **Real TCP/IP Connection** - Actually connects to device on port 4370  
✅ **Live Attendance Sync** - Pulls real punch records every 60 seconds  
✅ **Employee Management** - Pushes employee data to physical device  
✅ **Connection Status** - Shows real connection state, not simulated  
✅ **Duplicate Prevention** - Won't import same record twice  
✅ **Error Handling** - Proper timeouts and error messages  

## Next Steps

1. **Test with your device** at 192.168.1.127
2. **Add multiple devices** if you have them
3. **Set up employees** and push to devices
4. **Monitor attendance logs** for real-time data
5. **Check logs** for any issues

## Need Help?

- **Integration Guide**: See `INTEGRATION_GUIDE.md` for detailed testing
- **Application Logs**: Check console output for connection details
- **Database**: Records are saved to PostgreSQL `simcaf` database

## Important Notes

- Employee IDs **must be numeric** (1, 2, 3... not EMP001)
- Default sync interval is **60 seconds** (configurable)
- Connection uses **port 4370** (ZKTeco standard)
- Device must support **network communication** (not USB-only models)
- First time: **push employees to device** before expecting attendance records

## Verification Checklist

- [ ] Application starts successfully
- [ ] Can access web interface at http://192.168.1.109:8081
- [ ] Can add a device
- [ ] Device shows "Connected" after clicking Connect
- [ ] Logs show "Successfully connected to device"
- [ ] Can add an employee
- [ ] Can push employee to device
- [ ] Can see "Successfully pushed employee" in logs
- [ ] After punching on device, attendance appears in logs
- [ ] Automatic sync runs every 60 seconds

**When all items are checked, the integration is working correctly!**
