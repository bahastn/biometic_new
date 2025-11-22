package com.egfs.bio_new.service;

import com.egfs.bio_new.entity.AttendanceLog;
import com.egfs.bio_new.entity.Device;
import com.egfs.bio_new.entity.Employee;
import com.egfs.bio_new.repository.AttendanceLogRepository;
import com.egfs.bio_new.repository.DeviceRepository;
import com.egfs.bio_new.repository.EmployeeRepository;
import com.egfs.bio_new.sdk.AttendanceRecord;
import com.egfs.bio_new.sdk.ZKTecoDevice;
import com.egfs.bio_new.sdk.ZKTecoServerListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class ZKTecoService {
    
    private final DeviceRepository deviceRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceLogRepository attendanceLogRepository;
    private final ZKTecoServerListener serverListener;
    
    // Cache of connected devices - thread-safe for concurrent access
    private final Map<Long, ZKTecoDevice> deviceConnections = new ConcurrentHashMap<>();
    
    /**
     * Register all active devices with the server listener on startup
     */
    @PostConstruct
    public void registerDevices() {
        try {
            List<Device> activeDevices = deviceRepository.findByActive(true);
            for (Device device : activeDevices) {
                registerDeviceWithServerListener(device);
            }
            log.info("Registered {} active devices with server listener", activeDevices.size());
        } catch (Exception e) {
            log.error("Error registering devices with server listener", e);
        }
    }
    
    /**
     * Get or create device connection
     */
    private ZKTecoDevice getDeviceConnection(Device device) {
        ZKTecoDevice zkDevice = deviceConnections.get(device.getId());
        if (zkDevice == null) {
            zkDevice = new ZKTecoDevice(device.getIpAddress(), device.getPort());
            deviceConnections.put(device.getId(), zkDevice);
        }
        return zkDevice;
    }
    
    /**
     * Connect to a ZKTeco device
     */
    public boolean connectToDevice(Device device) {
        try {
            log.info("Attempting to connect to device: {} at {}:{}", 
                    device.getDeviceName(), device.getIpAddress(), device.getPort());
            
            // Register device with server listener for push mode support
            registerDeviceWithServerListener(device);
            log.info("Device {} registered for push mode on port 8086", device.getDeviceName());
            
            ZKTecoDevice zkDevice = getDeviceConnection(device);
            boolean connected = zkDevice.connect();
            
            if (connected) {
                device.setConnected(true);
                device.setLastSyncTime(LocalDateTime.now());
                deviceRepository.save(device);
                log.info("╔════════════════════════════════════════════════════════════════╗");
                log.info("║ PULL MODE CONNECTION SUCCESSFUL                                ║");
                log.info("╠════════════════════════════════════════════════════════════════╣");
                log.info("║ Device: {}", String.format("%-55s", device.getDeviceName()) + "║");
                log.info("║ IP Address: {}", String.format("%-51s", device.getIpAddress() + ":" + device.getPort()) + "║");
                log.info("║ Mode: Pull (Application initiated connection)                 ║");
                log.info("║ Status: Connected and ready for data sync                     ║");
                log.info("╚════════════════════════════════════════════════════════════════╝");
                return true;
            } else {
                device.setConnected(false);
                deviceRepository.save(device);
                log.warn("╔════════════════════════════════════════════════════════════════╗");
                log.warn("║ PULL MODE CONNECTION FAILED                                    ║");
                log.warn("╠════════════════════════════════════════════════════════════════╣");
                log.warn("║ Device: {}", String.format("%-55s", device.getDeviceName()) + "║");
                log.warn("║ IP Address: {}", String.format("%-51s", device.getIpAddress() + ":" + device.getPort()) + "║");
                log.warn("║                                                                ║");
                log.warn("║ PUSH MODE FALLBACK ACTIVE                                      ║");
                log.warn("║ - Device is registered to receive push data on port 8086      ║");
                log.warn("║ - If device is configured for push mode, data will sync       ║");
                log.warn("║   automatically when punches occur                             ║");
                log.warn("║ - Check PUSH_MODE_GUIDE.md for device configuration           ║");
                log.warn("╚════════════════════════════════════════════════════════════════╝");
                // Even if pull mode fails, keep device registered for push mode
                return false;
            }
        } catch (Exception e) {
            log.error("Error connecting to device: {}", device.getDeviceName(), e);
            device.setConnected(false);
            deviceRepository.save(device);
            return false;
        }
    }
    
    /**
     * Disconnect from a ZKTeco device
     */
    public void disconnectDevice(Device device) {
        try {
            log.info("Disconnecting from device: {}", device.getDeviceName());
            
            // Unregister from server listener
            unregisterDeviceFromServerListener(device);
            
            ZKTecoDevice zkDevice = deviceConnections.get(device.getId());
            if (zkDevice != null) {
                zkDevice.disconnect();
                deviceConnections.remove(device.getId());
            }
            
            device.setConnected(false);
            deviceRepository.save(device);
            log.info("Successfully disconnected from device: {}", device.getDeviceName());
        } catch (Exception e) {
            log.error("Error disconnecting from device: {}", device.getDeviceName(), e);
        }
    }
    
    /**
     * Sync attendance data from device
     */
    @Transactional
    public List<AttendanceLog> syncAttendanceData(Device device) {
        List<AttendanceLog> newLogs = new ArrayList<>();
        
        try {
            log.info("Syncing attendance data from device: {}", device.getDeviceName());
            
            ZKTecoDevice zkDevice = getDeviceConnection(device);
            
            // Connect if not already connected
            if (!zkDevice.isConnected()) {
                if (!zkDevice.connect()) {
                    log.error("Cannot sync - failed to connect to device: {}", device.getDeviceName());
                    device.setConnected(false);
                    deviceRepository.save(device);
                    return newLogs;
                }
                device.setConnected(true);
            }
            
            // Get attendance records from device
            List<AttendanceRecord> records = zkDevice.getAttendanceRecords();
            
            // Process each record
            for (AttendanceRecord record : records) {
                try {
                    // Find employee by employee ID
                    Employee employee = employeeRepository.findByEmployeeId(String.valueOf(record.getUserId()))
                            .orElse(null);
                    
                    if (employee == null) {
                        log.warn("Employee not found for user ID: {} - skipping record", record.getUserId());
                        continue;
                    }
                    
                    // Check if this attendance record already exists to avoid duplicates
                    boolean exists = attendanceLogRepository.existsByEmployeeAndPunchTimeAndDevice(
                            employee, record.getPunchTime(), device);
                    
                    if (!exists) {
                        AttendanceLog attendanceLog = new AttendanceLog();
                        attendanceLog.setEmployee(employee);
                        attendanceLog.setDevice(device);
                        attendanceLog.setPunchTime(record.getPunchTime());
                        attendanceLog.setVerifyMode(record.getVerifyTypeString());
                        attendanceLog.setPunchType(record.getInOutStateString());
                        
                        newLogs.add(attendanceLogRepository.save(attendanceLog));
                        log.debug("Saved attendance record for employee {} at {}", 
                                employee.getEmployeeId(), record.getPunchTime());
                    }
                } catch (Exception e) {
                    log.error("Error processing attendance record for user {}", record.getUserId(), e);
                }
            }
            
            device.setLastSyncTime(LocalDateTime.now());
            deviceRepository.save(device);
            
            log.info("Successfully synced {} new attendance records from device: {}", 
                    newLogs.size(), device.getDeviceName());
            
        } catch (Exception e) {
            log.error("Error syncing attendance data from device: {}", device.getDeviceName(), e);
        }
        
        return newLogs;
    }
    
    /**
     * Push employee data to device
     */
    @Transactional
    public boolean pushEmployeeToDevice(Device device, Employee employee) {
        try {
            log.info("Pushing employee {} to device: {}", employee.getEmployeeId(), device.getDeviceName());
            
            ZKTecoDevice zkDevice = getDeviceConnection(device);
            
            // Connect if not already connected
            if (!zkDevice.isConnected()) {
                if (!zkDevice.connect()) {
                    log.error("Cannot push employee - failed to connect to device: {}", device.getDeviceName());
                    device.setConnected(false);
                    deviceRepository.save(device);
                    return false;
                }
                device.setConnected(true);
            }
            
            // Parse employee ID to integer
            int userId;
            try {
                userId = Integer.parseInt(employee.getEmployeeId());
            } catch (NumberFormatException e) {
                log.error("Invalid employee ID format: {} - must be numeric", employee.getEmployeeId());
                return false;
            }
            
            // Parse card number if available
            int cardNumber = 0;
            if (employee.getCardNumber() != null && !employee.getCardNumber().isEmpty()) {
                try {
                    cardNumber = Integer.parseInt(employee.getCardNumber());
                } catch (NumberFormatException e) {
                    log.warn("Invalid card number format for employee {}: {}", 
                            employee.getEmployeeId(), employee.getCardNumber());
                }
            }
            
            // Set user on device (privilege 0 = normal user)
            boolean success = zkDevice.setUser(userId, employee.getName(), 0, "", cardNumber);
            
            if (success) {
                log.info("Successfully pushed employee {} to device: {}", 
                        employee.getEmployeeId(), device.getDeviceName());
            } else {
                log.error("Failed to push employee {} to device: {}", 
                        employee.getEmployeeId(), device.getDeviceName());
            }
            
            return success;
            
        } catch (Exception e) {
            log.error("Error pushing employee to device: {}", device.getDeviceName(), e);
            return false;
        }
    }
    
    /**
     * Get all employees from device
     */
    public List<Employee> getEmployeesFromDevice(Device device) {
        List<Employee> employees = new ArrayList<>();
        
        try {
            log.info("Getting employees from device: {}", device.getDeviceName());
            
            // Note: This functionality is not commonly used with ZKTeco devices
            // as employee data is typically managed in the application and pushed to devices
            log.warn("Get employees from device is not implemented - manage employees in application");
            
        } catch (Exception e) {
            log.error("Error getting employees from device: {}", device.getDeviceName(), e);
        }
        
        return employees;
    }
    
    /**
     * Test device connection
     */
    public boolean testConnection(String ipAddress, Integer port) {
        try {
            log.info("Testing connection to {}:{}", ipAddress, port);
            return ZKTecoDevice.testConnection(ipAddress, port);
        } catch (Exception e) {
            log.error("Error testing connection to {}:{}", ipAddress, port, e);
            return false;
        }
    }
    
    /**
     * Register a device with the server listener for push mode
     */
    private void registerDeviceWithServerListener(Device device) {
        serverListener.registerDeviceHandler(device.getIpAddress(), records -> {
            processAttendanceRecordsFromPush(device, records);
        });
        log.debug("Registered device {} ({}) with server listener", device.getDeviceName(), device.getIpAddress());
    }
    
    /**
     * Unregister a device from the server listener
     */
    private void unregisterDeviceFromServerListener(Device device) {
        serverListener.unregisterDeviceHandler(device.getIpAddress());
        log.debug("Unregistered device {} ({}) from server listener", device.getDeviceName(), device.getIpAddress());
    }
    
    /**
     * Process attendance records received from device push
     */
    @Transactional
    public void processAttendanceRecordsFromPush(Device device, List<AttendanceRecord> records) {
        try {
            log.info("Processing {} pushed attendance records from device: {}", records.size(), device.getDeviceName());
            
            int newRecordsCount = 0;
            
            for (AttendanceRecord record : records) {
                try {
                    // Find employee by employee ID
                    Employee employee = employeeRepository.findByEmployeeId(String.valueOf(record.getUserId()))
                            .orElse(null);
                    
                    if (employee == null) {
                        log.warn("Employee not found for user ID: {} - skipping record", record.getUserId());
                        continue;
                    }
                    
                    // Check if this attendance record already exists to avoid duplicates
                    boolean exists = attendanceLogRepository.existsByEmployeeAndPunchTimeAndDevice(
                            employee, record.getPunchTime(), device);
                    
                    if (!exists) {
                        AttendanceLog attendanceLog = new AttendanceLog();
                        attendanceLog.setEmployee(employee);
                        attendanceLog.setDevice(device);
                        attendanceLog.setPunchTime(record.getPunchTime());
                        attendanceLog.setVerifyMode(record.getVerifyTypeString());
                        attendanceLog.setPunchType(record.getInOutStateString());
                        
                        attendanceLogRepository.save(attendanceLog);
                        newRecordsCount++;
                        log.debug("Saved pushed attendance record for employee {} at {}", 
                                employee.getEmployeeId(), record.getPunchTime());
                    }
                } catch (Exception e) {
                    log.error("Error processing pushed attendance record for user {}", record.getUserId(), e);
                }
            }
            
            // Update device last sync time
            device.setLastSyncTime(LocalDateTime.now());
            device.setConnected(true);
            deviceRepository.save(device);
            
            log.info("Successfully processed {} new pushed attendance records from device: {}", 
                    newRecordsCount, device.getDeviceName());
            
        } catch (Exception e) {
            log.error("Error processing pushed attendance records from device: {}", device.getDeviceName(), e);
        }
    }
}
