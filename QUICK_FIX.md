# Quick Fix for "Connection Reset" Errors

If you're seeing "Connection reset" errors when trying to connect to your ZKTeco device, follow these steps:

## The Problem

Your ZKTeco device is configured in **cloud/push mode** (device connects to server), but the application was trying to use **pull mode** (server connects to device). This causes connection conflicts.

## The Solution - 2 Options

### Option 1: Use Push Mode (Recommended - Already Working!)

The application now has a push mode server running on port 8086. Just configure your device:

#### Steps:

1. **Open firewall** on your server (if needed):
   ```bash
   sudo ufw allow 8086/tcp
   ```

2. **Configure your ZKTeco device**:
   - Access device menu (MENU button → enter admin password)
   - Go to: `Comm → Cloud Server` or `Network → Cloud`
   - Set:
     - **Server IP**: `192.168.1.109` (your server IP)
     - **Server Port**: `8086`
     - **Enable**: Yes
   - Save and reboot device

3. **That's it!** The application is already listening on port 8086.

#### Verify it's working:

Check application logs for:
```
INFO: ZKTeco push mode server started on port 8086
INFO: Accepted connection from device at 192.168.1.127
INFO: Received X attendance records from device
```

### Option 2: Use Pull Mode Only

If you prefer the old pull mode, disable cloud mode on the device:

1. Access device menu
2. Go to: `Comm → Cloud Server`
3. Set **Enable**: No
4. Save and reboot device

The application will connect to the device on port 4370.

## Need More Details?

- **Complete device configuration guide**: [PUSH_MODE_GUIDE.md](PUSH_MODE_GUIDE.md)
- **General troubleshooting**: [README.md](README.md#troubleshooting)

## Quick Check

Test if push mode server is running:
```bash
netstat -an | grep 8086
# Should show: tcp 0 0 0.0.0.0:8086 0.0.0.0:* LISTEN
```

Test connectivity from device network:
```bash
telnet 192.168.1.109 8086
# Should connect successfully
```

## Benefits of Push Mode

✅ Solves "Connection reset" errors  
✅ Real-time data sync (no delay)  
✅ Works with devices behind NAT  
✅ Lower network overhead  

## Still Having Issues?

1. Check firewall allows port 8086
2. Verify device can reach server IP
3. Check application logs for errors
4. See detailed troubleshooting in [PUSH_MODE_GUIDE.md](PUSH_MODE_GUIDE.md)
