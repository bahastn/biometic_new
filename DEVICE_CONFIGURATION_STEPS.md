# ZKTeco Device Configuration Steps for Push Mode

## Problem Summary
Your ZKTeco device (iFace_701-1 at `192.168.1.127:4370`) is currently configured in **push mode** and rejecting incoming connections from the application. This is why you see "Connection reset" errors.

## What Changed
The application now displays the **actual server IP address** you need to configure on your device, instead of showing a placeholder `<your-server-ip>`.

When you start your application, you'll see:
```
╔════════════════════════════════════════════════════════════════╗
║ ZKTeco PUSH MODE SERVER STARTED                                ║
╠════════════════════════════════════════════════════════════════╣
║ Listening Port: 8086                                           ║
║ Status: Ready to accept device connections                    ║
║                                                                ║
║ NEXT STEPS:                                                    ║
║ 1. Configure your ZKTeco device cloud server settings:        ║
║    - Server IP: 192.168.1.109                                  ║
║    - Server Port: 8086                                         ║
║ 2. Enable cloud/push mode on the device                       ║
║ 3. Reboot device to establish connection                      ║
║                                                                ║
║ See PUSH_MODE_GUIDE.md for detailed instructions              ║
╚════════════════════════════════════════════════════════════════╝
```

## Quick Fix Steps

### 1. On Your ZKTeco Device
Access the device menu (you'll need admin credentials):

1. **Press MENU** on the device
2. **Enter admin password**
3. Navigate to **Comm → Cloud Server** (or **Network → Cloud**)
4. Configure:
   - **Server IP**: `192.168.1.109` (shown in your startup logs)
   - **Server Port**: `8086`
   - **Enable**: `Yes`/`On`
5. **Save settings**
6. **Reboot the device**

### 2. Verify Network Connectivity
Ensure the device can reach your server:
```bash
# From a computer on the same network as the device
telnet 192.168.1.109 8086
# Should connect successfully
```

### 3. Check Firewall
If you have a firewall, ensure port 8086 is open:
```bash
# On Ubuntu/Debian
sudo ufw allow 8086/tcp

# Or for firewalld (CentOS/RHEL)
sudo firewall-cmd --permanent --add-port=8086/tcp
sudo firewall-cmd --reload
```

### 4. Monitor Application Logs
After configuring the device and rebooting it, watch your application logs for:
```
INFO: Accepted connection from device at 192.168.1.127
INFO: Device 192.168.1.127 connected with session ID xxxxx
INFO: Received X attendance records from device 192.168.1.127
```

## What to Expect

### Successful Connection
When the device successfully connects in push mode, you'll see:
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

### Data Synchronization
Once connected, attendance records will automatically sync when:
- An employee punches in/out on the device
- The device pushes the data to your application
- The application processes and stores the attendance log

## Troubleshooting

### Device Still Not Connecting?

**Check 1: Verify server IP in application.properties**
```properties
server.address=192.168.1.109
```
Make sure this matches your server's actual IP address.

**Check 2: Verify both ports**
- Web UI: `http://192.168.1.109:8081`
- Push server: Port `8086` (for device connections)

**Check 3: Network reachability**
From device network:
```bash
ping 192.168.1.109
telnet 192.168.1.109 8086
```

**Check 4: Application logs**
Look for:
```
INFO: ZKTeco PUSH MODE SERVER STARTED
INFO: Listening Port: 8086
```

If you see:
```
ERROR: PUSH MODE SERVER FAILED TO START
ERROR: Port 8086 may already be in use
```
Then port 8086 is already in use by another application.

## Alternative: Disable Push Mode

If you prefer to use **pull mode** instead (application connects to device):
1. On device: Disable cloud/push mode
2. In application.properties: Keep `zkteco.sync.enabled=true`
3. Restart both device and application

## For More Help
See `PUSH_MODE_GUIDE.md` for detailed configuration instructions and troubleshooting.
