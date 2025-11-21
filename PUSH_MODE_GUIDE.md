# ZKTeco Push Mode Configuration Guide

This document explains how to configure ZKTeco devices to work in push/cloud mode with the biometric attendance system.

## Problem

If you're seeing "Connection reset" errors when the application tries to connect to your ZKTeco device, it's likely because:

1. The device is configured in **Push/Cloud mode** (device initiates connections)
2. The application was trying to use **Pull mode** (application initiates connections)
3. Both trying to connect simultaneously causes connection resets

## Solution

The application now supports **BOTH modes simultaneously**:

- **Pull Mode**: Application connects to device (port 4370) - traditional mode
- **Push Mode**: Device connects to application (port 8086) - cloud mode

## Configuration

### Application Configuration

The push mode server is enabled by default. Configuration in `application.properties`:

```properties
# ZKTeco Push Mode Server Configuration
zkteco.server.enabled=true
zkteco.server.port=8086
```

You can override these with environment variables:
```bash
export ZKTECO_SERVER_ENABLED=true
export ZKTECO_SERVER_PORT=8086
```

### Device Configuration

To configure your ZKTeco device in push/cloud mode:

#### Method 1: Device Menu (Recommended)

1. **Access device menu**:
   - Press MENU on the device
   - Enter admin password

2. **Navigate to Communication**:
   - Go to: `Comm -> Cloud Server` or `Network -> Cloud`

3. **Configure server settings**:
   - **Server IP**: `192.168.1.109` (your application server IP)
   - **Server Port**: `8086` (push mode port)
   - **Protocol**: `TCP` or `Standalone`
   - **Enable**: `Yes` or `On`

4. **Save and reboot device**

#### Method 2: Web Interface (if available)

1. **Access device web interface**:
   - Open browser: `http://192.168.1.127` (device IP)
   - Login with admin credentials

2. **Navigate to Cloud Settings**:
   - Go to: `System -> Communication -> Cloud`

3. **Configure**:
   - Server Address: `192.168.1.109:8086`
   - Enable Cloud: Yes
   - Save settings

#### Method 3: ZKAccess Software

1. Open ZKAccess software
2. Connect to device
3. Go to Device Settings → Communication
4. Configure cloud server settings
5. Upload to device

## How It Works

### Push Mode Flow

1. **Device initiates connection** to application server (port 8086)
2. **Device sends attendance data** when punches occur
3. **Application receives and processes** the data in real-time
4. **Device maintains connection** for continuous data push

### Hybrid Mode

The application supports both modes simultaneously:

1. **For devices in pull mode**: Application connects and pulls data every 60 seconds
2. **For devices in push mode**: Application waits for device to push data
3. **Automatic detection**: No manual configuration needed

## Firewall Configuration

Ensure your firewall allows:

### On Application Server

```bash
# Allow incoming connections on push mode port
sudo ufw allow 8086/tcp

# Or for firewalld:
sudo firewall-cmd --permanent --add-port=8086/tcp
sudo firewall-cmd --reload
```

### On Device Side

- Ensure outbound connections to port 8086 are allowed
- Check router/firewall between device and server

## Network Requirements

1. **Same Network**: Device and server should be on the same network
2. **Reachable**: Device must be able to reach server IP
3. **Port Access**: Port 8086 must be accessible from device

Test connectivity from device network:
```bash
# From a computer on the same network as device
telnet 192.168.1.109 8086
# Should connect successfully
```

## Verification

### Check Application Logs

When the application starts, you should see:

```
INFO: ZKTeco server listener started on port 8086
INFO: Devices can now push data to this server at 192.168.1.109:8086
```

When a device connects:

```
INFO: Accepted connection from device at 192.168.1.127
INFO: Device 192.168.1.127 connected with session ID 12345
INFO: Received 5 attendance records from device 192.168.1.127
INFO: Successfully processed 5 new pushed attendance records from device: iFace_701-1
```

### Troubleshooting

#### Device Not Connecting

**Check 1: Server is running**
```bash
# Check if port is listening
netstat -an | grep 8086
# Should show: tcp  0  0  0.0.0.0:8086  0.0.0.0:*  LISTEN
```

**Check 2: Firewall**
```bash
# Test from device network
telnet 192.168.1.109 8086
```

**Check 3: Device configuration**
- Verify server IP is correct
- Verify server port is 8086
- Ensure cloud/push mode is enabled
- Reboot device after changes

#### Connection Resets

If you still see "Connection reset" errors:

1. **Disable pull mode temporarily**: 
   - Set `zkteco.sync.enabled=false` in application.properties
   - Restart application
   - This prevents the application from trying to connect to device

2. **Verify device mode**:
   - Check device is configured for push mode only
   - Some devices support hybrid mode

3. **Check network**:
   - Ensure stable network connection
   - Check for network equipment between device and server
   - Verify MTU settings if using VPN

#### No Data Received

**Check 1: Employees registered**
- Employees must be registered on device
- Use the "Push Employee to Device" function

**Check 2: Test punch**
- Make a test punch on device
- Check application logs for received data

**Check 3: Device logs**
- Access device system logs
- Check for connection errors

## Port Reference

| Port | Purpose | Direction | Protocol |
|------|---------|-----------|----------|
| 8081 | Web UI | Incoming | HTTP |
| 4370 | Device Pull Mode | Outgoing | TCP |
| 8086 | Device Push Mode | Incoming | TCP |

## Advanced Configuration

### Custom Port

To use a different port for push mode:

```properties
# In application.properties
zkteco.server.port=9000
```

Configure this same port in your device cloud settings.

### Multiple Servers

If running multiple application instances:

1. Each instance needs a unique push mode port
2. Configure devices to connect to specific server ports
3. Use load balancer for high availability

### Security

Push mode communication is unencrypted (ZKTeco protocol limitation):

1. **Network Security**: Keep devices on secure internal network
2. **Firewall**: Restrict port 8086 to device network only
3. **VPN**: Use VPN for remote device connections

Example firewall rule (UFW):
```bash
# Allow only from device network
sudo ufw allow from 192.168.1.0/24 to any port 8086
```

## Benefits of Push Mode

1. **Real-time data**: Immediate attendance record sync
2. **Reduced network load**: Device initiates connection only when needed
3. **Better for NAT**: Works better with devices behind NAT/firewall
4. **Scalability**: Supports many devices without constant polling

## Migration from Pull Mode

If you were using pull mode and want to switch to push mode:

1. **Update device configuration** to push mode (see above)
2. **Keep application as-is** - it supports both modes
3. **Monitor logs** to confirm push mode is working
4. **Optionally disable pull mode** once confirmed:
   ```properties
   zkteco.sync.enabled=false
   ```

## Support

For issues:

1. Check application logs: `/logs` or console output
2. Verify device configuration using device menu
3. Test network connectivity between device and server
4. Ensure ports are open in firewall

Common issues resolved by push mode:
- ✅ "Connection reset" errors
- ✅ "Connection refused" errors  
- ✅ Devices behind NAT/firewall
- ✅ Intermittent connectivity
- ✅ Real-time data requirements
