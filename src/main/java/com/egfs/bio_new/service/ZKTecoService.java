package com.egfs.bio_new.service;

import com.egfs.bio_new.entity.AttendanceLog;
import com.egfs.bio_new.entity.Device;
import com.egfs.bio_new.entity.Employee;
import com.egfs.bio_new.repository.AttendanceLogRepository;
import com.egfs.bio_new.repository.DeviceRepository;
import com.egfs.bio_new.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ZKTecoService {
    
    private final DeviceRepository deviceRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceLogRepository attendanceLogRepository;
    
    /**
     * Connect to a ZKTeco device
     */
    public boolean connectToDevice(Device device) {
        try {
            log.info("Attempting to connect to device: {} at {}:{}", 
                    device.getDeviceName(), device.getIpAddress(), device.getPort());
            
            // TODO: Implement actual ZKTeco SDK connection
            // For now, we'll simulate a connection
            // ZKFingerReader zkReader = new ZKFingerReader();
            // boolean connected = zkReader.connect(device.getIpAddress(), device.getPort());
            
            boolean connected = true; // Simulated connection
            
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
            
            // TODO: Implement actual ZKTeco SDK disconnection
            // zkReader.disconnect();
            
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
            
            if (!device.getConnected()) {
                if (!connectToDevice(device)) {
                    log.error("Cannot sync - device not connected: {}", device.getDeviceName());
                    return newLogs;
                }
            }
            
            // TODO: Implement actual ZKTeco SDK data retrieval
            // List<AttendanceRecord> records = zkReader.getAttendanceRecords();
            // for (AttendanceRecord record : records) {
            //     Employee employee = employeeRepository.findByEmployeeId(record.getUserId())
            //             .orElse(null);
            //     if (employee != null) {
            //         AttendanceLog log = new AttendanceLog();
            //         log.setEmployee(employee);
            //         log.setDevice(device);
            //         log.setPunchTime(record.getTimestamp());
            //         log.setVerifyMode(record.getVerifyMode());
            //         newLogs.add(attendanceLogRepository.save(log));
            //     }
            // }
            
            device.setLastSyncTime(LocalDateTime.now());
            deviceRepository.save(device);
            
            log.info("Successfully synced {} attendance records from device: {}", 
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
            
            if (!device.getConnected()) {
                if (!connectToDevice(device)) {
                    log.error("Cannot push employee - device not connected: {}", device.getDeviceName());
                    return false;
                }
            }
            
            // TODO: Implement actual ZKTeco SDK user upload
            // zkReader.setUserInfo(employee.getEmployeeId(), employee.getName(), 
            //                      employee.getFingerTemplate(), employee.getCardNumber());
            
            log.info("Successfully pushed employee {} to device: {}", 
                    employee.getEmployeeId(), device.getDeviceName());
            return true;
            
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
            
            if (!device.getConnected()) {
                if (!connectToDevice(device)) {
                    log.error("Cannot get employees - device not connected: {}", device.getDeviceName());
                    return employees;
                }
            }
            
            // TODO: Implement actual ZKTeco SDK user retrieval
            // List<UserInfo> users = zkReader.getAllUserInfo();
            // for (UserInfo user : users) {
            //     Employee employee = new Employee();
            //     employee.setEmployeeId(user.getUserId());
            //     employee.setName(user.getName());
            //     employee.setCardNumber(user.getCardNumber());
            //     employees.add(employee);
            // }
            
            log.info("Retrieved {} employees from device: {}", employees.size(), device.getDeviceName());
            
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
            
            // TODO: Implement actual ZKTeco SDK connection test
            // ZKFingerReader zkReader = new ZKFingerReader();
            // boolean connected = zkReader.connect(ipAddress, port);
            // if (connected) {
            //     zkReader.disconnect();
            // }
            // return connected;
            
            return true; // Simulated successful connection
            
        } catch (Exception e) {
            log.error("Error testing connection to {}:{}", ipAddress, port, e);
            return false;
        }
    }
}
