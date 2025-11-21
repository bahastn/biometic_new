package com.egfs.bio_new.service;

import com.egfs.bio_new.entity.AttendanceLog;
import com.egfs.bio_new.entity.Device;
import com.egfs.bio_new.entity.Employee;
import com.egfs.bio_new.repository.AttendanceLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceService {
    
    private final AttendanceLogRepository attendanceLogRepository;
    private final ZKTecoService zkTecoService;
    
    public List<AttendanceLog> getAllAttendanceLogs() {
        return attendanceLogRepository.findAll();
    }
    
    public List<AttendanceLog> getAttendanceByEmployee(Employee employee) {
        return attendanceLogRepository.findByEmployee(employee);
    }
    
    public List<AttendanceLog> getAttendanceByDateRange(LocalDateTime start, LocalDateTime end) {
        return attendanceLogRepository.findByPunchTimeBetween(start, end);
    }
    
    public List<AttendanceLog> getAttendanceByEmployeeAndDateRange(
            Employee employee, LocalDateTime start, LocalDateTime end) {
        return attendanceLogRepository.findByEmployeeAndPunchTimeBetween(employee, start, end);
    }
    
    public List<AttendanceLog> syncAttendanceFromDevice(Device device) {
        log.info("Syncing attendance from device: {}", device.getDeviceName());
        return zkTecoService.syncAttendanceData(device);
    }
}
