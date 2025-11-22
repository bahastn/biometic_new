# ZKTeco Device Connection Troubleshooting Guide

This guide helps you diagnose and fix connection issues with ZKTeco biometric devices.

## Quick Diagnosis

### Symptom 1: "Connection reset" error

**What it means:** The device is actively rejecting incoming connections.

**Most likely cause:** Device is configured in **PUSH mode** (cloud mode) and expects to connect TO a server, not accept connections FROM a server.

**Solution:**
1. **Option A - Configure device for push mode** (Recommended if device is already in push mode):
   - Access device menu (MENU button, enter admin password)
   - Navigate to: `Comm → Cloud Server` or `Network → Cloud`
   - Configure:
     - Server IP: Your application server IP (e.g., 192.168.1.109)
     - Server Port: 8086
     - Protocol: TCP
     - Enable: Yes
   - Save and reboot device
   - Wait 1-2 minutes for device to connect
   - Check application logs for "PUSH MODE CONNECTION ESTABLISHED"

2. **Option B - Switch device to pull mode**:
   - Access device menu
   - Navigate to: `Comm → Cloud Server` or `Network → Cloud`
   - Disable cloud/push mode
   - Save and reboot device
   - Try connecting again from application

### Symptom 2: "Device not reachable" error

**What it means:** Cannot establish ANY network connection to the device.

**Possible causes:**
1. Device is powered off
2. Device IP address is wrong
3. Device is on a different network
4. Firewall is blocking the connection
5. Network cable unplugged (for wired devices)

**Solution:**
```bash
# Test 1: Ping the device
ping 192.168.1.127
# Should get replies if device is on network

# Test 2: Check if port is accessible
telnet 192.168.1.127 4370
# Or use: nc -zv 192.168.1.127 4370

# Test 3: Check your network configuration
ip addr show  # Linux
ipconfig      # Windows
```

If ping succeeds but telnet fails:
- Device might be in push mode (see Symptom 1)
- Firewall on device or network might be blocking port 4370

### Symptom 3: Connection succeeds but no data syncs

**What it means:** Connection established but no attendance records appear.

**Possible causes:**
1. No attendance records on device
2. Employees not registered on device
3. Clock/date mismatch between device and server
4. Records already synced and cleared

**Solution:**
1. Make a test punch on the device
2. Check if employee exists in both device and application
3. Verify device date/time matches server
4. Check application logs for sync activity

### Symptom 4: "Push mode server failed to start"

**What it means:** Application cannot start the push mode listener on port 8086.

**Possible causes:**
1. Port 8086 already in use by another application
2. No permission to bind to port
3. Firewall blocking the port

**Solution:**
```bash
# Check what's using port 8086
netstat -an | grep 8086      # Linux/Mac
netstat -an | findstr 8086   # Windows

# Check if port is open
sudo ufw allow 8086/tcp      # Ubuntu/Debian
firewall-cmd --add-port=8086/tcp --permanent && firewall-cmd --reload  # CentOS/RHEL

# If another application is using it, either:
# 1. Stop that application, or
# 2. Change push mode port in application.properties:
#    zkteco.server.port=9000
```

## Connection Modes Explained

### Pull Mode (Application → Device)
- **How it works:** Application actively connects to device (port 4370)
- **Best for:** Simple setups, single network
- **Pros:** Easy to configure, works with older devices
- **Cons:** Devices must accept incoming connections, issues with NAT/firewalls

### Push Mode (Device → Application)
- **How it works:** Device connects to application server (port 8086)
- **Best for:** Complex networks, devices behind NAT, real-time updates
- **Pros:** Works through NAT, real-time data, better for many devices
- **Cons:** Requires device configuration, not all models support it

### Hybrid Mode (Both)
- **How it works:** Application supports both modes simultaneously
- **Best for:** Mixed environments, migration scenarios
- **Pros:** Maximum compatibility, automatic fallback
- **Cons:** Requires both ports to be accessible

## Step-by-Step Connection Setup

### For Pull Mode

1. **Verify network connectivity:**
   ```bash
   ping 192.168.1.127
   telnet 192.168.1.127 4370
   ```

2. **Add device in application:**
   - Navigate to: Devices → Add New Device
   - Enter: Name, IP (192.168.1.127), Port (4370)
   - Click: Save

3. **Connect to device:**
   - Click: Connect button
   - Look for: "PULL MODE CONNECTION SUCCESSFUL"

4. **If connection fails:**
   - Check device is in pull mode (cloud/push disabled)
   - Verify IP and port are correct
   - Check firewall allows outbound port 4370

### For Push Mode

1. **Verify push server is running:**
   - Check application logs for: "ZKTeco PUSH MODE SERVER STARTED"
   - Verify listening on port 8086:
     ```bash
     netstat -an | grep 8086
     # Should show: tcp 0 0 0.0.0.0:8086 0.0.0.0:* LISTEN
     ```

2. **Add device in application:**
   - Navigate to: Devices → Add New Device
   - Enter: Name, IP (192.168.1.127), Port (4370)
   - Click: Save
   - Note: IP is device's IP, not server IP

3. **Configure device for push mode:**
   - Access device menu
   - Go to: Comm → Cloud Server
   - Configure:
     - Server IP: <Your server IP, e.g., 192.168.1.109>
     - Server Port: 8086
     - Enable: Yes
   - Save and reboot device

4. **Verify connection:**
   - Wait 1-2 minutes after device reboot
   - Check logs for: "PUSH MODE CONNECTION ESTABLISHED"
   - Device IP should match your device

5. **If device doesn't connect:**
   - Verify device can reach server:
     ```bash
     # From a computer on same network as device:
     telnet 192.168.1.109 8086
     ```
   - Check server IP is correct in device config
   - Verify port 8086 is open in server firewall
   - Ensure network allows device→server communication

## Network Troubleshooting

### Check Server Firewall

Ubuntu/Debian:
```bash
sudo ufw status
sudo ufw allow 8086/tcp
sudo ufw allow 4370/tcp  # If using pull mode
```

CentOS/RHEL:
```bash
sudo firewall-cmd --list-all
sudo firewall-cmd --add-port=8086/tcp --permanent
sudo firewall-cmd --add-port=4370/tcp --permanent
sudo firewall-cmd --reload
```

### Check Network Routing

If device and server are on different networks:
```bash
# Check routing table
ip route show      # Linux
route print        # Windows

# Test connection from device network
# Use a computer on same network as device:
ping <server-ip>
telnet <server-ip> 8086
```

### Check for NAT/Proxy Issues

If devices are behind NAT:
- **Pull mode:** May not work if device has private IP
- **Push mode:** Should work (device initiates outbound connection)
- **Solution:** Use push mode or configure port forwarding

## Common Error Messages

### "Connection refused"
- **Cause:** Nothing is listening on that port
- **Solution:** 
  - For pull mode: Device may be off or in push mode
  - For push mode: Server not running or wrong port

### "Connection reset"
- **Cause:** Device/server actively rejected connection
- **Solution:**
  - Device in push mode → Configure for push or disable it
  - Firewall reset connection → Check firewall rules

### "Connection timeout"
- **Cause:** Network packet didn't reach destination
- **Solution:**
  - Check network connectivity (ping)
  - Verify IP address is correct
  - Check firewall allows traffic

### "Device not reachable"
- **Cause:** Cannot establish socket connection
- **Solution:**
  - Verify device is powered on
  - Check IP address is correct
  - Ensure device is on same network or routable

## Advanced Diagnostics

### Enable Debug Logging

In `application.properties`:
```properties
# Enable debug logging for ZKTeco components
logging.level.com.egfs.bio_new.sdk=DEBUG
logging.level.com.egfs.bio_new.service=DEBUG
```

### Monitor Network Traffic

Using tcpdump (Linux):
```bash
# Monitor traffic to/from device
sudo tcpdump -i any host 192.168.1.127

# Monitor specific port
sudo tcpdump -i any port 4370 or port 8086
```

Using Wireshark:
1. Start capture on network interface
2. Filter: `ip.addr == 192.168.1.127`
3. Look for TCP handshake (SYN, SYN-ACK, ACK)
4. Check for RST (reset) packets

### Test with Multiple Devices

If one device works but another doesn't:
- Compare device firmware versions
- Check device settings are identical
- Verify both devices on same network
- Try different device models separately

## Getting Help

If you've tried everything above and still have issues:

1. **Collect information:**
   - Application logs (last 100 lines)
   - Device model and firmware version
   - Network topology diagram
   - Exact error messages
   - Output of: `ping`, `telnet`, `netstat` commands

2. **Check documentation:**
   - `README.md` - General setup
   - `PUSH_MODE_GUIDE.md` - Push mode details
   - `CONNECTION_IMPROVEMENTS.md` - Technical details

3. **Create issue:**
   - Include all information from step 1
   - Describe what you've already tried
   - Specify device model and your network setup

## Quick Reference

### Ports Used
| Port | Purpose | Direction | Required For |
|------|---------|-----------|--------------|
| 8081 | Web UI | Incoming | Application access |
| 4370 | Device Pull | Outgoing | Pull mode sync |
| 8086 | Device Push | Incoming | Push mode sync |

### Configuration Properties
```properties
# Server
server.address=192.168.1.109
server.port=8081

# Device defaults
zkteco.default.device.ip=192.168.1.127
zkteco.default.device.port=4370

# Push mode server
zkteco.server.enabled=true
zkteco.server.port=8086

# Pull mode sync
zkteco.sync.enabled=true
zkteco.sync.interval=60000
```

### Quick Commands
```bash
# Test device reachability
ping 192.168.1.127

# Test pull mode port
telnet 192.168.1.127 4370

# Test push mode port
telnet 192.168.1.109 8086

# Check application logs
tail -f logs/application.log

# Check listening ports
netstat -an | grep -E "8081|8086|4370"
```
