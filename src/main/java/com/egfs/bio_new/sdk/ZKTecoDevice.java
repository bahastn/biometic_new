package com.egfs.bio_new.sdk;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * ZKTeco device communication implementation
 * Implements the ZKTeco proprietary protocol for network-based attendance devices
 */
@Slf4j
public class ZKTecoDevice {
    
    private String ipAddress;
    private int port;
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private int sessionId = 0;
    private int replyNumber = 0;
    
    // Protocol constants
    private static final int CMD_CONNECT = 1000;
    private static final int CMD_EXIT = 1001;
    private static final int CMD_ENABLE_DEVICE = 1002;
    private static final int CMD_DISABLE_DEVICE = 1003;
    private static final int CMD_GET_ATTENDANCE = 1004;
    private static final int CMD_CLEAR_ATTENDANCE = 1005;
    private static final int CMD_USER_WKM = 1008;
    private static final int CMD_SET_USER = 1009;
    private static final int CMD_GET_USER = 1011;
    private static final int CMD_DELETE_USER = 1012;
    private static final int CMD_CLEAR_ADMIN = 1014;
    private static final int CMD_GET_FREE_SIZES = 1050;
    private static final int CMD_VERSION = 1100;
    private static final int CMD_PREPARE_DATA = 1500;
    private static final int CMD_DATA = 1501;
    
    private static final int USHRT_MAX = 65535;
    
    public ZKTecoDevice(String ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
    }
    
    /**
     * Connect to the device
     */
    public boolean connect() {
        return connectWithRetry(3, 2000);
    }
    
    /**
     * Connect to the device with retry logic
     * 
     * @param maxRetries Maximum number of connection attempts
     * @param retryDelayMs Delay between retries in milliseconds
     * @return true if connected successfully, false otherwise
     */
    private boolean connectWithRetry(int maxRetries, long retryDelayMs) {
        int attempt = 0;
        
        while (attempt < maxRetries) {
            attempt++;
            
            try {
                log.info("Connecting to ZKTeco device at {}:{} (attempt {}/{})", 
                        ipAddress, port, attempt, maxRetries);
                
                // Clean up any existing connection
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // Ignore
                    }
                }
                
                // Create new socket connection
                socket = new Socket();
                socket.connect(new java.net.InetSocketAddress(ipAddress, port), 5000);
                socket.setSoTimeout(10000); // 10 second timeout for read operations
                socket.setKeepAlive(true);
                socket.setTcpNoDelay(true);
                
                out = new DataOutputStream(socket.getOutputStream());
                in = new DataInputStream(socket.getInputStream());
                
                // Send connect command
                byte[] cmd = createCommand(CMD_CONNECT, new byte[0]);
                out.write(cmd);
                out.flush();
                
                log.debug("Connect command sent, waiting for reply...");
                
                // Read response
                byte[] reply = readReply();
                if (reply != null && reply.length >= 6) {
                    sessionId = ByteBuffer.wrap(reply, 4, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
                    log.info("Successfully connected to device at {}:{}. Session ID: {}", 
                            ipAddress, port, sessionId);
                    return true;
                } else {
                    log.warn("No valid response from device (attempt {}/{})", attempt, maxRetries);
                }
                
            } catch (SocketTimeoutException e) {
                log.warn("Connection timeout to {}:{} (attempt {}/{}): {}", 
                        ipAddress, port, attempt, maxRetries, e.getMessage());
            } catch (java.net.ConnectException e) {
                log.warn("Connection refused to {}:{} (attempt {}/{}): {}", 
                        ipAddress, port, attempt, maxRetries, e.getMessage());
            } catch (IOException e) {
                log.warn("Error connecting to device at {}:{} (attempt {}/{}): {}", 
                        ipAddress, port, attempt, maxRetries, e.getMessage());
            }
            
            // Disconnect and clean up before retry
            disconnect();
            
            // Wait before retrying (except on last attempt)
            if (attempt < maxRetries) {
                try {
                    log.debug("Waiting {}ms before retry...", retryDelayMs);
                    Thread.sleep(retryDelayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Interrupted while waiting to retry connection");
                    break;
                }
            }
        }
        
        log.error("Failed to connect to device at {}:{} after {} attempts", 
                ipAddress, port, maxRetries);
        return false;
    }
    
    /**
     * Check if socket is in a healthy state for communication
     * 
     * A socket is considered healthy when:
     * - It exists (not null)
     * - It's not closed
     * - It's connected
     * - Input stream is not shut down
     * - Output stream is not shut down
     * 
     * @return true if socket can be used for communication, false otherwise
     */
    private boolean isSocketHealthy() {
        return socket != null && !socket.isClosed() && socket.isConnected() 
                && !socket.isInputShutdown() && !socket.isOutputShutdown();
    }
    
    /**
     * Disconnect from the device
     */
    public void disconnect() {
        if (isSocketHealthy()) {
            try {
                // Send disconnect command only if socket is in a good state
                byte[] cmd = createCommand(CMD_EXIT, new byte[0]);
                out.write(cmd);
                out.flush();
            } catch (IOException e) {
                // Ignore errors when sending disconnect command as connection may already be broken
                log.debug("Could not send disconnect command (connection may already be closed): {}", e.getMessage());
            }
        }
        
        // Always clean up resources regardless of socket state
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                log.info("Disconnected from device at {}:{}", ipAddress, port);
            }
        } catch (IOException e) {
            log.debug("Error closing socket: {}", e.getMessage());
        } finally {
            socket = null;
            out = null;
            in = null;
            sessionId = 0;
            replyNumber = 0;
        }
    }
    
    /**
     * Check if device is connected
     */
    public boolean isConnected() {
        return socket != null && !socket.isClosed() && socket.isConnected();
    }
    
    /**
     * Disable device (prevents user interaction during sync)
     */
    public boolean disableDevice() {
        try {
            byte[] cmd = createCommand(CMD_DISABLE_DEVICE, new byte[0]);
            out.write(cmd);
            out.flush();
            
            byte[] reply = readReply();
            return reply != null;
        } catch (IOException e) {
            log.error("Error disabling device", e);
            return false;
        }
    }
    
    /**
     * Enable device (restore user interaction)
     */
    public boolean enableDevice() {
        try {
            byte[] cmd = createCommand(CMD_ENABLE_DEVICE, new byte[0]);
            out.write(cmd);
            out.flush();
            
            byte[] reply = readReply();
            return reply != null;
        } catch (IOException e) {
            log.error("Error enabling device", e);
            return false;
        }
    }
    
    /**
     * Get attendance records from device
     */
    public List<AttendanceRecord> getAttendanceRecords() {
        List<AttendanceRecord> records = new ArrayList<>();
        
        try {
            // Disable device during sync
            disableDevice();
            
            // Send command to prepare attendance data
            byte[] cmd = createCommand(CMD_GET_ATTENDANCE, new byte[0]);
            out.write(cmd);
            out.flush();
            
            // Read response
            byte[] reply = readReply();
            if (reply == null || reply.length < 8) {
                log.warn("No attendance data received from device");
                try {
                    enableDevice();
                } catch (Exception e) {
                    log.warn("Failed to re-enable device after empty response", e);
                }
                return records;
            }
            
            // Check if there's data to read
            int dataSize = ByteBuffer.wrap(reply, 8, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            if (dataSize == 0) {
                log.info("No new attendance records on device");
                try {
                    enableDevice();
                } catch (Exception e) {
                    log.warn("Failed to re-enable device after no records", e);
                }
                return records;
            }
            
            // Request the data
            cmd = createCommand(CMD_PREPARE_DATA, new byte[0]);
            out.write(cmd);
            out.flush();
            
            // Read the actual data
            byte[] data = readLargeReply();
            if (data != null && data.length > 0) {
                records = parseAttendanceRecords(data);
            }
            
            // Enable device again
            try {
                enableDevice();
            } catch (Exception e) {
                log.warn("Failed to re-enable device after successful sync", e);
            }
            
            log.info("Retrieved {} attendance records from device", records.size());
            
        } catch (IOException e) {
            log.error("Error getting attendance records", e);
            try {
                enableDevice();
            } catch (Exception ex) {
                log.warn("Failed to re-enable device after error", ex);
            }
        }
        
        return records;
    }
    
    /**
     * Set user information on device
     */
    public boolean setUser(int userId, String name, int privilege, String password, int cardNumber) {
        try {
            // Disable device during operation
            disableDevice();
            
            // Prepare user data using ByteBuffer for proper little-endian byte order
            ByteBuffer buffer = ByteBuffer.allocate(100).order(ByteOrder.LITTLE_ENDIAN);
            
            // User ID (2 bytes, little-endian)
            buffer.putShort((short) userId);
            
            // Privilege (1 byte): 0=User, 14=Admin
            buffer.put((byte) privilege);
            
            // Password (8 bytes)
            byte[] passwordBytes = new byte[8];
            if (password != null && !password.isEmpty()) {
                byte[] pwdBytes = password.getBytes();
                System.arraycopy(pwdBytes, 0, passwordBytes, 0, Math.min(pwdBytes.length, 8));
            }
            buffer.put(passwordBytes);
            
            // Name (28 bytes, null-terminated)
            byte[] nameBytes = new byte[28];
            if (name != null && !name.isEmpty()) {
                byte[] nmBytes = name.getBytes("UTF-8");
                System.arraycopy(nmBytes, 0, nameBytes, 0, Math.min(nmBytes.length, 27));
            }
            buffer.put(nameBytes);
            
            // Card number (4 bytes, little-endian)
            buffer.putInt(cardNumber);
            
            // Group (1 byte)
            buffer.put((byte) 0);
            
            // Timezone (2 bytes, little-endian)
            buffer.putShort((short) 0);
            
            // UID (4 bytes, little-endian) - User ID again
            buffer.putInt(userId);
            
            byte[] userData = new byte[buffer.position()];
            buffer.rewind();
            buffer.get(userData);
            
            // Send command
            byte[] cmd = createCommand(CMD_SET_USER, userData);
            out.write(cmd);
            out.flush();
            
            // Read response
            byte[] reply = readReply();
            
            // Enable device again
            try {
                enableDevice();
            } catch (Exception e) {
                log.warn("Failed to re-enable device after setting user", e);
            }
            
            boolean success = reply != null;
            if (success) {
                log.info("Successfully set user {} on device", userId);
            } else {
                log.error("Failed to set user {} on device", userId);
            }
            
            return success;
            
        } catch (IOException e) {
            log.error("Error setting user on device", e);
            try {
                enableDevice();
            } catch (Exception ex) {
                log.warn("Failed to re-enable device after error", ex);
            }
            return false;
        }
    }
    
    /**
     * Parse attendance records from raw data
     */
    private List<AttendanceRecord> parseAttendanceRecords(byte[] data) {
        List<AttendanceRecord> records = new ArrayList<>();
        
        try {
            // Each attendance record is typically 40 bytes
            int recordSize = 40;
            int recordCount = data.length / recordSize;
            
            for (int i = 0; i < recordCount; i++) {
                int offset = i * recordSize;
                
                if (offset + recordSize > data.length) {
                    break;
                }
                
                // Parse user ID (2 bytes at offset 0)
                int userId = ByteBuffer.wrap(data, offset, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
                
                // Parse timestamp (4 bytes at offset 4)
                long timestamp = ByteBuffer.wrap(data, offset + 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt() & 0xFFFFFFFFL;
                
                // Parse verification type (1 byte at offset 8)
                int verifyType = data[offset + 8] & 0xFF;
                
                // Parse in/out state (1 byte at offset 9)
                int inOutState = data[offset + 9] & 0xFF;
                
                // Convert timestamp to LocalDateTime
                LocalDateTime dateTime = LocalDateTime.ofInstant(
                    new Date(timestamp * 1000L).toInstant(),
                    ZoneId.systemDefault()
                );
                
                AttendanceRecord record = new AttendanceRecord();
                record.setUserId(userId);
                record.setPunchTime(dateTime);
                record.setVerifyType(verifyType);
                record.setInOutState(inOutState);
                
                records.add(record);
            }
            
        } catch (Exception e) {
            log.error("Error parsing attendance records", e);
        }
        
        return records;
    }
    
    /**
     * Create a command packet
     */
    private byte[] createCommand(int command, byte[] data) throws IOException {
        // Use ByteBuffer for consistent little-endian byte order
        int headerSize = 16;
        ByteBuffer buffer = ByteBuffer.allocate(headerSize + data.length).order(ByteOrder.LITTLE_ENDIAN);
        
        // Start marker (2 bytes) - 0x5050
        buffer.putShort((short) 0x5050);
        
        // Device ID (2 bytes) - always 0 for TCP
        buffer.putShort((short) 0);
        
        // Session ID (2 bytes)
        buffer.putShort((short) sessionId);
        
        // Reply number (2 bytes)
        buffer.putShort((short) replyNumber);
        replyNumber = (replyNumber + 1) % USHRT_MAX;
        
        // Command ID (2 bytes)
        buffer.putShort((short) command);
        
        // Checksum placeholder (2 bytes) - will be calculated and set later
        int checksumPosition = buffer.position();
        buffer.putShort((short) 0);
        
        // Data size (4 bytes)
        buffer.putInt(data.length);
        
        // Data payload
        buffer.put(data);
        
        byte[] packet = buffer.array();
        
        // Calculate checksum over entire packet except checksum field itself
        int checksum = 0;
        for (int i = 0; i < packet.length; i++) {
            if (i != checksumPosition && i != checksumPosition + 1) {
                checksum += packet[i] & 0xFF;
            }
        }
        
        // Set checksum in packet (little-endian)
        packet[checksumPosition] = (byte) (checksum & 0xFF);
        packet[checksumPosition + 1] = (byte) ((checksum >> 8) & 0xFF);
        
        return packet;
    }
    
    /**
     * Read a reply packet from device
     */
    private byte[] readReply() throws IOException {
        try {
            // Read header (16 bytes minimum)
            byte[] header = new byte[16];
            in.readFully(header);
            
            // Check start marker
            short startMarker = ByteBuffer.wrap(header, 0, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
            if (startMarker != 0x5050) {
                log.error("Invalid start marker in reply: {}", String.format("0x%04X", startMarker));
                return null;
            }
            
            // Get data size from header
            int dataSize = ByteBuffer.wrap(header, 12, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            
            // Read data if any
            byte[] data = new byte[dataSize];
            if (dataSize > 0) {
                in.readFully(data);
            }
            
            // Combine header and data
            byte[] fullReply = new byte[header.length + data.length];
            System.arraycopy(header, 0, fullReply, 0, header.length);
            System.arraycopy(data, 0, fullReply, header.length, data.length);
            
            return fullReply;
            
        } catch (SocketTimeoutException e) {
            log.error("Timeout reading reply from device");
            return null;
        } catch (EOFException e) {
            log.error("Unexpected end of stream from device");
            return null;
        }
    }
    
    /**
     * Read large data reply (for attendance records)
     */
    private byte[] readLargeReply() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try {
            while (true) {
                byte[] reply = readReply();
                if (reply == null) {
                    break;
                }
                
                // Extract command from reply
                int replyCommand = ByteBuffer.wrap(reply, 8, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
                
                // If it's a data packet, extract the data
                if (replyCommand == CMD_DATA || replyCommand == CMD_PREPARE_DATA) {
                    int dataSize = ByteBuffer.wrap(reply, 12, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
                    if (dataSize > 0 && reply.length > 16) {
                        baos.write(reply, 16, dataSize);
                    }
                    
                    // If data size is less than expected, we're done
                    if (dataSize < 1024) {
                        break;
                    }
                } else {
                    // Not a data packet, we're done
                    break;
                }
            }
        } catch (Exception e) {
            log.error("Error reading large reply", e);
        }
        
        return baos.toByteArray();
    }
    
    /**
     * Test connection to device
     */
    public static boolean testConnection(String ipAddress, int port) {
        ZKTecoDevice device = new ZKTecoDevice(ipAddress, port);
        try {
            boolean connected = device.connect();
            if (connected) {
                device.disconnect();
            }
            return connected;
        } catch (Exception e) {
            log.error("Error testing connection to {}:{}", ipAddress, port, e);
            return false;
        }
    }
}
