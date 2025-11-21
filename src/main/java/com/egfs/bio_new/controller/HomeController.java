package com.egfs.bio_new.controller;

import com.egfs.bio_new.entity.AttendanceLog;
import com.egfs.bio_new.service.AttendanceService;
import com.egfs.bio_new.service.DeviceService;
import com.egfs.bio_new.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {
    
    private final DeviceService deviceService;
    private final EmployeeService employeeService;
    private final AttendanceService attendanceService;
    
    @GetMapping("/")
    public String home(Model model) {
        // Get today's attendance
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(23, 59, 59);
        List<AttendanceLog> todayAttendance = attendanceService.getAttendanceByDateRange(startOfDay, endOfDay);
        
        // Statistics
        long totalDevices = deviceService.getAllDevices().size();
        long activeDevices = deviceService.getActiveDevices().size();
        long totalEmployees = employeeService.getAllEmployees().size();
        long activeEmployees = employeeService.getActiveEmployees().size();
        
        model.addAttribute("totalDevices", totalDevices);
        model.addAttribute("activeDevices", activeDevices);
        model.addAttribute("totalEmployees", totalEmployees);
        model.addAttribute("activeEmployees", activeEmployees);
        model.addAttribute("todayAttendanceCount", todayAttendance.size());
        model.addAttribute("todayAttendance", todayAttendance);
        model.addAttribute("pageTitle", "Dashboard");
        
        return "index";
    }
    
    @GetMapping("/attendance")
    public String attendance(Model model) {
        List<AttendanceLog> allAttendance = attendanceService.getAllAttendanceLogs();
        model.addAttribute("attendanceLogs", allAttendance);
        model.addAttribute("pageTitle", "Attendance Logs");
        return "attendance/list";
    }
}
