# ZKTeco Device Connection Modes

## Overview

The biometric attendance system supports three connection modes for ZKTeco devices:

1. **AUTO** (Default) - Automatically detects the device mode
2. **PULL** - Application connects to device (traditional mode)
3. **PUSH** - Device connects to application (cloud mode)

## Connection Modes

### AUTO Mode (Recommended)

This is the default mode when a device is first added to the system.

**How it works:**
1. Application attempts to connect to device in pull mode
2. If connection succeeds → Mode is saved as **PULL**
3. If connection fails (connection reset) → Mode is saved as **PUSH**
4. Future connections skip the failed mode attempt

**Advantages:**
- No manual configuration required
- Automatically adapts to device configuration
- Reduces error messages after initial detection

**When to use:**
- When adding new devices
- When unsure of device configuration
- Default for most use cases

### PULL Mode

The application initiates connections to the device.

**How it works:**
1. Application connects to device on port 4370
2. Application requests data from device
3. Connection is maintained for the duration of data transfer

**Advantages:**
- Direct control over when data is synced
- Works with older ZKTeco devices
- No device configuration needed (uses default settings)

**Requirements:**
- Device must accept incoming connections
- Device must be configured in "pull" or "standalone" mode
- Network must allow outgoing connections from server to device

**When to use:**
- For devices behind the same firewall as server
- When you need precise control over sync timing
- When device doesn't support push mode

**Configuration on Device:**
- No special configuration needed
- Ensure device is not in cloud/push mode

### PUSH Mode

The device initiates connections to the application.

**How it works:**
1. Device connects to application on port 8086
2. Device pushes attendance data when punches occur
3. Real-time data synchronization

**Advantages:**
- Real-time attendance data sync
- Works better with devices behind NAT/firewall
- Reduces network traffic (data sent only when needed)
- Better for large deployments with many devices

**Requirements:**
- Application server must be reachable from device
- Port 8086 must be open on server firewall
- Device must be configured with server IP and port

**When to use:**
- For devices on different networks
- When real-time sync is required
- When devices are behind NAT/firewall
- When managing many devices

**Configuration on Device:**
See [PUSH_MODE_GUIDE.md](PUSH_MODE_GUIDE.md) for detailed instructions.

## Setting Connection Mode

### Via Database (Current Method)

Since the web UI may not yet have the connection mode field, you can set it directly in the database:

```sql
-- Note: The database column is 'connection_mode' (snake_case)
-- Valid values are: 'AUTO', 'PULL', 'PUSH' (case-sensitive)

-- Set device to AUTO mode (default)
UPDATE devices SET connection_mode = 'AUTO' WHERE id = 1;

-- Set device to PULL mode
UPDATE devices SET connection_mode = 'PULL' WHERE id = 1;

-- Set device to PUSH mode
UPDATE devices SET connection_mode = 'PUSH' WHERE id = 1;
```

### Via Web Interface (If Implemented)

If the web interface has been updated with the connection mode field:

1. Go to Devices page
2. Click "Add Device" or edit existing device
3. Set "Connection Mode" field:
   - `AUTO` - Let system detect (default)
   - `PULL` - Force pull mode
   - `PUSH` - Force push mode only
4. Save device

### Via API (If Available)

```json
POST /api/devices
{
  "deviceName": "iFace_701-1",
  "ipAddress": "192.168.1.127",
  "port": 4370,
  "connectionMode": "AUTO"
}
```

## Troubleshooting

### Connection Errors with AUTO Mode

**Symptom:** Seeing "Connection reset" messages in logs

**Solution:** 
- This is expected during initial connection
- System will auto-detect and save the correct mode
- Error messages will disappear after first successful sync

### Device Not Syncing in PUSH Mode

**Check:**
1. Device is configured with correct server IP
2. Device is configured with server port 8086
3. Firewall allows incoming connections on port 8086
4. Device can reach server (check network connectivity)

**Verify:**
```bash
# On server, check if port is listening
netstat -an | grep 8086

# From device network, test connectivity
telnet <server-ip> 8086
```

See [PUSH_MODE_GUIDE.md](PUSH_MODE_GUIDE.md) for detailed troubleshooting.

### Device Not Syncing in PULL Mode

**Check:**
1. Device is powered on and connected to network
2. Device IP address is correct in system
3. Device is reachable from server
4. Device is not in cloud/push mode

**Verify:**
```bash
# Test connectivity
ping <device-ip>

# Test port access
telnet <device-ip> 4370
```

### Changing Connection Mode

**From AUTO to PULL:**
1. Edit device
2. Set connection mode to `PULL`
3. Disable push mode on device (if enabled)
4. Save and reconnect

**From AUTO to PUSH:**
1. Edit device
2. Set connection mode to `PUSH`
3. Configure device to push to server (see PUSH_MODE_GUIDE.md)
4. Save device
5. Wait for device to connect

**From PULL to PUSH:**
1. Edit device  
2. Set connection mode to `PUSH`
3. Configure device for push mode
4. Save and disconnect device
5. Device will connect in push mode

**From PUSH to PULL:**
1. Disable push mode on device
2. Edit device in system
3. Set connection mode to `PULL`
4. Save and reconnect

## Log Messages

### AUTO Mode - First Connection

```
INFO: Attempting to connect to device: iFace_701-1 at 192.168.1.127:4370 (Mode: AUTO)
INFO: Device iFace_701-1 registered for push mode on port 8086 (fallback)
INFO: Connection reset from 192.168.1.127:4370 - device appears to be in push mode
INFO: Auto-detected device mode as PUSH for device: iFace_701-1
INFO: ╔════════════════════════════════════════════════════════════════╗
INFO: ║ PUSH MODE DETECTED                                             ║
...
```

### PULL Mode - Connected

```
INFO: Attempting to connect to device: iFace_701-1 at 192.168.1.127:4370 (Mode: PULL)
INFO: Successfully connected to device at 192.168.1.127:4370. Session ID: 12345
INFO: ╔════════════════════════════════════════════════════════════════╗
INFO: ║ PULL MODE CONNECTION SUCCESSFUL                                ║
...
```

### PUSH Mode - Configured

```
INFO: Attempting to connect to device: iFace_701-1 at 192.168.1.127:4370 (Mode: PUSH)
INFO: ╔════════════════════════════════════════════════════════════════╗
INFO: ║ PUSH MODE CONFIGURED                                           ║
INFO: ║ Device is registered to receive push data on port 8086        ║
...
```

## Best Practices

1. **Use AUTO mode for new devices** - Let the system detect the correct mode
2. **Use PUSH mode for remote devices** - Better for devices on different networks
3. **Use PULL mode for local devices** - When all devices are on the same network
4. **Document device configurations** - Keep track of which devices use which mode
5. **Monitor logs during setup** - Verify mode detection is working correctly

## Migration from Previous Versions

If you're upgrading from a version without connection mode support:

1. All existing devices will default to AUTO mode
2. First connection attempt will auto-detect the mode
3. Mode will be saved for future connections
4. No manual configuration needed

## FAQ

**Q: What happens if I set wrong connection mode?**  
A: The connection will fail. Change the mode back to AUTO or the correct mode.

**Q: Can I use both PULL and PUSH for the same device?**  
A: No, each device can only use one mode at a time. Use AUTO to let the system choose.

**Q: Which mode is faster?**  
A: PUSH mode provides real-time sync. PULL mode syncs on demand or by schedule.

**Q: Do I need to restart the application when changing modes?**  
A: No, just save the device configuration and reconnect.

**Q: What if AUTO mode detects the wrong mode?**  
A: Manually set the correct mode and ensure device is configured accordingly.

## Related Documentation

- [PUSH_MODE_GUIDE.md](PUSH_MODE_GUIDE.md) - Detailed push mode configuration
- [QUICK_START.md](QUICK_START.md) - Getting started guide
- [TROUBLESHOOTING.md](TROUBLESHOOTING.md) - Common issues and solutions
