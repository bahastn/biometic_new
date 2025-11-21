package com.egfs.bio_new.repository;

import com.egfs.bio_new.entity.AttendanceLog;
import com.egfs.bio_new.entity.Device;
import com.egfs.bio_new.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AttendanceLogRepository extends JpaRepository<AttendanceLog, Long> {
    
    List<AttendanceLog> findByEmployee(Employee employee);
    
    List<AttendanceLog> findByPunchTimeBetween(LocalDateTime start, LocalDateTime end);
    
    List<AttendanceLog> findByEmployeeAndPunchTimeBetween(Employee employee, LocalDateTime start, LocalDateTime end);
    
    boolean existsByEmployeeAndPunchTimeAndDevice(Employee employee, LocalDateTime punchTime, Device device);
}
