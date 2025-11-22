# QUICK FIX: Device Connection Reset Issue

## Your Situation
- Device: iFace_701-1 at 192.168.1.127:4370
- Error: "Connection reset"
- Status: Not connected

## Root Cause
Your device is configured in **PUSH mode** (cloud mode) and is rejecting incoming connections. The application is trying to connect in **PULL mode**, causing the connection reset.

## Solution: Configure Device for Push Mode

### Step 1: Access Device Menu
1. Press **MENU** button on the device
2. Enter **admin password**

### Step 2: Navigate to Cloud Settings
Go to: **Comm → Cloud Server** (or **Network → Cloud**)

### Step 3: Configure Server Settings
Set the following:
- **Server IP**: 192.168.1.109 (your application server IP)
- **Server Port**: 8086
- **Protocol**: TCP
- **Enable**: Yes

### Step 4: Save and Reboot
1. Save the settings
2. Reboot the device
3. Wait 1-2 minutes

### Step 5: Verify Connection
Check application logs for:
```
╔════════════════════════════════════════════════════════════════╗
║ PUSH MODE CONNECTION ESTABLISHED                               ║
╠════════════════════════════════════════════════════════════════╣
║ Device IP: 192.168.1.127                                       ║
```

## Alternative: Switch to Pull Mode

If you prefer pull mode instead:

### Step 1: Access Device Menu
Same as above

### Step 2: Disable Push Mode
1. Go to: **Comm → Cloud Server**
2. Set **Enable** to **No**
3. Save and reboot

### Step 3: Try Connection Again
1. In the application, go to **Devices**
2. Click **Connect** for your device
3. Should now connect successfully

## Verification Commands

### Check if Push Server is Running
```bash
netstat -an | grep 8086
# Should show: tcp 0 0 0.0.0.0:8086 0.0.0.0:* LISTEN
```

### Test Network Connectivity
```bash
# From a computer on the same network as the device:
ping 192.168.1.127
telnet 192.168.1.109 8086
```

### Check Firewall
```bash
# Ubuntu/Debian
sudo ufw allow 8086/tcp

# CentOS/RHEL
sudo firewall-cmd --add-port=8086/tcp --permanent
sudo firewall-cmd --reload
```

## Expected Behavior After Fix

### When Device Connects (Push Mode)
Application logs will show:
```
╔════════════════════════════════════════════════════════════════╗
║ PUSH MODE CONNECTION ESTABLISHED                               ║
╠════════════════════════════════════════════════════════════════╣
║ Device IP: 192.168.1.127                                       ║
║ Session ID: 12345                                              ║
║ Mode: Push (Device initiated connection)                      ║
║ Status: Ready to receive attendance data                      ║
╚════════════════════════════════════════════════════════════════╝
```

### When Attendance Data Arrives
```
INFO: Received 5 attendance records from device 192.168.1.127
INFO: Successfully processed 5 new pushed attendance records from device: iFace_701-1
```

## Still Not Working?

See the comprehensive [TROUBLESHOOTING.md](TROUBLESHOOTING.md) guide for:
- Detailed diagnostic procedures
- Network troubleshooting
- Advanced configuration
- Common error solutions

## Summary

**What you need to do:**
1. Configure device cloud server to: 192.168.1.109:8086
2. Enable cloud/push mode
3. Reboot device
4. Wait 1-2 minutes
5. Check logs for "PUSH MODE CONNECTION ESTABLISHED"

**The application is already configured correctly** - it supports both pull and push modes. You just need to configure the device to connect to the server.
