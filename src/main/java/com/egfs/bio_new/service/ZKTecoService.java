package com.egfs.bio_new.service;

import com.egfs.bio_new.entity.AttendanceLog;
import com.egfs.bio_new.entity.Device;
import com.egfs.bio_new.entity.Employee;
import com.egfs.bio_new.repository.AttendanceLogRepository;
import com.egfs.bio_new.repository.DeviceRepository;
import com.egfs.bio_new.repository.EmployeeRepository;
import com.egfs.bio_new.sdk.AttendanceRecord;
import com.egfs.bio_new.sdk.ZKTecoDevice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ZKTecoService {
    
    private final DeviceRepository deviceRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceLogRepository attendanceLogRepository;
    
    // Cache of connected devices
    private final Map<Long, ZKTecoDevice> deviceConnections = new HashMap<>();
    
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
            
            ZKTecoDevice zkDevice = getDeviceConnection(device);
            boolean connected = zkDevice.connect();
            
            if (connected) {
                device.setConnected(true);
                device.setLastSyncTime(LocalDateTime.now());
                deviceRepository.save(device);
                log.info("Successfully connected to device: {}", device.getDeviceName());
                return true;
            } else {
                device.setConnected(false);
                deviceRepository.save(device);
                log.error("Failed to connect to device: {}", device.getDeviceName());
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
}
