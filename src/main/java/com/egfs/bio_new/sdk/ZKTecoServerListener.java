package com.egfs.bio_new.sdk;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Server socket listener for ZKTeco devices in push/cloud mode
 * Accepts incoming connections from devices and processes pushed data
 */
@Slf4j
@Component
public class ZKTecoServerListener {
    
    private ServerSocket serverSocket;
    private ExecutorService executorService;
    private volatile boolean running = false;
    private int port;
    
    // Protocol constants
    private static final int CMD_CONNECT = 1000;
    private static final int CMD_EXIT = 1001;
    private static final int CMD_ACK_OK = 2000;
    private static final int CMD_ACK_ERROR = 2001;
    private static final int CMD_DATA = 1501;
    private static final int CMD_ATTENDANCE_LOG = 500;
    
    private static final int PACKET_HEADER_SIZE = 16;
    private static final int MAX_DATA_SIZE = 5 * 1024 * 1024;
    private static final int USHRT_MAX = 65535;
    
    // Attendance record format constants
    private static final int ATTENDANCE_RECORD_SIZE = 40; // Size of each attendance record in bytes
    private static final int USER_ID_OFFSET = 0;
    private static final int USER_ID_SIZE = 2;
    private static final int TIMESTAMP_OFFSET = 4;
    private static final int TIMESTAMP_SIZE = 4;
    private static final int VERIFY_TYPE_OFFSET = 8;
    private static final int IN_OUT_STATE_OFFSET = 9;
    
    // Map to store device connection handlers
    private final ConcurrentHashMap<String, Consumer<List<AttendanceRecord>>> deviceHandlers = new ConcurrentHashMap<>();
    
    /**
     * Start the server listener on the specified port
     */
    public void start(int port) {
        start(port, null);
    }
    
    /**
     * Start the server listener on the specified port with server address
     */
    public void start(int port, String serverAddress) {
        if (running) {
            log.warn("Server listener already running on port {}", this.port);
            return;
        }
        
        this.port = port;
        
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            executorService = Executors.newCachedThreadPool();
            
            // Determine the display address
            String displayAddress = serverAddress;
            if (displayAddress == null || displayAddress.isEmpty() || "0.0.0.0".equals(displayAddress)) {
                displayAddress = "<your-server-ip>";
            }
            
            log.info("╔════════════════════════════════════════════════════════════════╗");
            log.info("║ ZKTeco PUSH MODE SERVER STARTED                                ║");
            log.info("╠════════════════════════════════════════════════════════════════╣");
            log.info("║ Listening Port: {}                                           ║", port);
            log.info("║ Status: Ready to accept device connections                    ║");
            log.info("║                                                                ║");
            log.info("║ NEXT STEPS:                                                    ║");
            log.info("║ 1. Configure your ZKTeco device cloud server settings:        ║");
            log.info("║    - Server IP: {}                                ║", String.format("%-38s", displayAddress));
            log.info("║    - Server Port: {}                                         ║", port);
            log.info("║ 2. Enable cloud/push mode on the device                       ║");
            log.info("║ 3. Reboot device to establish connection                      ║");
            log.info("║                                                                ║");
            log.info("║ See PUSH_MODE_GUIDE.md for detailed instructions              ║");
            log.info("╚════════════════════════════════════════════════════════════════╝");
            
            // Start accepting connections in a separate thread
            executorService.submit(this::acceptConnections);
            
        } catch (IOException e) {
            log.error("Failed to start server listener on port {}", port, e);
            log.error("╔════════════════════════════════════════════════════════════════╗");
            log.error("║ PUSH MODE SERVER FAILED TO START                               ║");
            log.error("╠════════════════════════════════════════════════════════════════╣");
            log.error("║ Port {} may already be in use                              ║", port);
            log.error("║ Please check:                                                  ║");
            log.error("║ 1. No other service is using port {}                       ║", port);
            log.error("║ 2. Firewall allows port {} (run: netstat -an | grep {})  ║", port, port);
            log.error("║ 3. Application has permission to bind to port                 ║");
            log.error("╚════════════════════════════════════════════════════════════════╝");
            running = false;
        }
    }
    
    /**
     * Stop the server listener
     */
    public void stop() {
        running = false;
        
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            log.debug("Error closing server socket", e);
        }
        
        if (executorService != null) {
            executorService.shutdown();
        }
        
        log.info("ZKTeco server listener stopped");
    }
    
    /**
     * Register a handler for attendance records from a specific device
     */
    public void registerDeviceHandler(String deviceIp, Consumer<List<AttendanceRecord>> handler) {
        deviceHandlers.put(deviceIp, handler);
        log.info("Registered handler for device: {}", deviceIp);
    }
    
    /**
     * Unregister a device handler
     */
    public void unregisterDeviceHandler(String deviceIp) {
        deviceHandlers.remove(deviceIp);
        log.info("Unregistered handler for device: {}", deviceIp);
    }
    
    /**
     * Accept incoming connections from devices
     */
    private void acceptConnections() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                String clientIp = clientSocket.getInetAddress().getHostAddress();
                log.info("Accepted connection from device at {}", clientIp);
                
                // Handle each device connection in a separate thread
                executorService.submit(() -> handleDeviceConnection(clientSocket, clientIp));
                
            } catch (SocketException e) {
                if (running) {
                    log.error("Socket error while accepting connections", e);
                }
                // If not running, this is expected during shutdown
            } catch (IOException e) {
                if (running) {
                    log.error("Error accepting connection", e);
                }
            }
        }
    }
    
    /**
     * Handle a device connection
     */
    private void handleDeviceConnection(Socket socket, String deviceIp) {
        DataInputStream in = null;
        DataOutputStream out = null;
        int sessionId = 0;
        int replyNumber = 0;
        
        try {
            socket.setSoTimeout(30000); // 30 second timeout
            socket.setKeepAlive(true);
            socket.setTcpNoDelay(true);
            
            // Set socket buffer sizes for better performance
            socket.setSendBufferSize(8192);
            socket.setReceiveBufferSize(8192);
            
            // Enable SO_LINGER to ensure proper connection closure
            socket.setSoLinger(true, 5);
            
            in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            
            log.debug("Device connection established from {}", deviceIp);
            
            // Keep connection alive and process commands
            while (!socket.isClosed() && running) {
                try {
                    // Read command packet
                    byte[] packet = readPacket(in);
                    if (packet == null) {
                        log.debug("No more data from device {}", deviceIp);
                        break;
                    }
                    
                    // Parse command
                    int command = ByteBuffer.wrap(packet, 8, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
                    sessionId = ByteBuffer.wrap(packet, 4, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
                    
                    log.debug("Received command {} from device {}", command, deviceIp);
                    
                    // Handle command
                    switch (command) {
                        case CMD_CONNECT:
                            // Device is connecting - send ACK
                            sendAck(out, sessionId, replyNumber++, CMD_ACK_OK);
                            log.info("╔════════════════════════════════════════════════════════════════╗");
                            log.info("║ PUSH MODE CONNECTION ESTABLISHED                               ║");
                            log.info("╠════════════════════════════════════════════════════════════════╣");
                            log.info("║ Device IP: {}                                              ║", deviceIp);
                            log.info("║ Session ID: {}                                             ║", sessionId);
                            log.info("║ Mode: Push (Device initiated connection)                      ║");
                            log.info("║ Status: Ready to receive attendance data                      ║");
                            log.info("╚════════════════════════════════════════════════════════════════╝");
                            break;
                            
                        case CMD_EXIT:
                            // Device is disconnecting
                            sendAck(out, sessionId, replyNumber++, CMD_ACK_OK);
                            log.info("Device {} disconnecting", deviceIp);
                            return;
                            
                        case CMD_DATA:
                        case CMD_ATTENDANCE_LOG:
                            // Device is sending attendance data
                            List<AttendanceRecord> records = parseAttendanceData(packet);
                            if (!records.isEmpty()) {
                                log.info("Received {} attendance records from device {}", records.size(), deviceIp);
                                
                                // Call registered handler if available
                                Consumer<List<AttendanceRecord>> handler = deviceHandlers.get(deviceIp);
                                if (handler != null) {
                                    try {
                                        handler.accept(records);
                                    } catch (Exception e) {
                                        log.error("Error in device handler for {}", deviceIp, e);
                                    }
                                }
                            }
                            sendAck(out, sessionId, replyNumber++, CMD_ACK_OK);
                            break;
                            
                        default:
                            // Unknown command - send ACK anyway
                            log.debug("Unknown command {} from device {}", command, deviceIp);
                            sendAck(out, sessionId, replyNumber++, CMD_ACK_OK);
                            break;
                    }
                    
                } catch (EOFException e) {
                    log.debug("Device {} closed connection", deviceIp);
                    break;
                } catch (Exception e) {
                    log.error("Error processing command from device {}", deviceIp, e);
                    break;
                }
            }
            
        } catch (IOException e) {
            log.error("Error handling device connection from {}", deviceIp, e);
        } finally {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                log.debug("Error closing socket for device {}", deviceIp, e);
            }
            log.info("Connection closed for device {}", deviceIp);
        }
    }
    
    /**
     * Read a packet from the input stream
     */
    private byte[] readPacket(DataInputStream in) throws IOException {
        // Read header (16 bytes)
        byte[] header = new byte[PACKET_HEADER_SIZE];
        in.readFully(header);
        
        // Check start marker
        short startMarker = ByteBuffer.wrap(header, 0, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
        if (startMarker != 0x5050) {
            log.error("Invalid start marker: {}", String.format("0x%04X", startMarker));
            return null;
        }
        
        // Get data size
        int dataSize = ByteBuffer.wrap(header, 12, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
        
        // Validate data size
        if (dataSize < 0 || dataSize > MAX_DATA_SIZE) {
            log.error("Invalid data size: {} bytes", dataSize);
            return null;
        }
        
        // Read data if any
        byte[] data = new byte[dataSize];
        if (dataSize > 0) {
            in.readFully(data);
        }
        
        // Combine header and data
        byte[] fullPacket = new byte[header.length + data.length];
        System.arraycopy(header, 0, fullPacket, 0, header.length);
        System.arraycopy(data, 0, fullPacket, header.length, data.length);
        
        return fullPacket;
    }
    
    /**
     * Send acknowledgment to device
     */
    private void sendAck(DataOutputStream out, int sessionId, int replyNumber, int command) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(PACKET_HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        
        // Start marker
        buffer.putShort((short) 0x5050);
        
        // Device ID
        buffer.putShort((short) 0);
        
        // Session ID
        buffer.putShort((short) sessionId);
        
        // Reply number
        buffer.putShort((short) (replyNumber % USHRT_MAX));
        
        // Command ID
        buffer.putShort((short) command);
        
        // Checksum placeholder
        int checksumPosition = buffer.position();
        buffer.putShort((short) 0);
        
        // Data size
        buffer.putInt(0);
        
        byte[] packet = buffer.array();
        
        // Calculate checksum
        int checksum = 0;
        for (int i = 0; i < packet.length; i++) {
            if (i != checksumPosition && i != checksumPosition + 1) {
                checksum += packet[i] & 0xFF;
            }
        }
        
        // Set checksum
        packet[checksumPosition] = (byte) (checksum & 0xFF);
        packet[checksumPosition + 1] = (byte) ((checksum >> 8) & 0xFF);
        
        out.write(packet);
        out.flush();
    }
    
    /**
     * Parse attendance data from packet
     */
    private List<AttendanceRecord> parseAttendanceData(byte[] packet) {
        List<AttendanceRecord> records = new ArrayList<>();
        
        try {
            if (packet.length <= PACKET_HEADER_SIZE) {
                return records;
            }
            
            // Extract data portion
            int dataSize = ByteBuffer.wrap(packet, 12, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            if (dataSize <= 0 || packet.length < PACKET_HEADER_SIZE + dataSize) {
                return records;
            }
            
            byte[] data = new byte[dataSize];
            System.arraycopy(packet, PACKET_HEADER_SIZE, data, 0, dataSize);
            
            // Each attendance record has a fixed size
            int recordCount = data.length / ATTENDANCE_RECORD_SIZE;
            
            for (int i = 0; i < recordCount; i++) {
                int offset = i * ATTENDANCE_RECORD_SIZE;
                
                if (offset + ATTENDANCE_RECORD_SIZE > data.length) {
                    break;
                }
                
                // Parse user ID
                int userId = ByteBuffer.wrap(data, offset + USER_ID_OFFSET, USER_ID_SIZE)
                        .order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
                
                // Parse timestamp
                long timestamp = ByteBuffer.wrap(data, offset + TIMESTAMP_OFFSET, TIMESTAMP_SIZE)
                        .order(ByteOrder.LITTLE_ENDIAN).getInt() & 0xFFFFFFFFL;
                
                // Parse verification type
                int verifyType = data[offset + VERIFY_TYPE_OFFSET] & 0xFF;
                
                // Parse in/out state
                int inOutState = data[offset + IN_OUT_STATE_OFFSET] & 0xFF;
                
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
            log.error("Error parsing attendance data", e);
        }
        
        return records;
    }
    
    /**
     * Check if the server is running
     */
    public boolean isRunning() {
        return running;
    }
    
    /**
     * Get the port the server is listening on
     */
    public int getPort() {
        return port;
    }
}
