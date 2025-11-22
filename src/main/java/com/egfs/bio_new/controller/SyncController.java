package com.egfs.bio_new.controller;

import com.egfs.bio_new.entity.Device;
import com.egfs.bio_new.entity.Employee;
import com.egfs.bio_new.service.DeviceService;
import com.egfs.bio_new.service.EmployeeService;
import com.egfs.bio_new.service.ZKTecoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/sync")
@RequiredArgsConstructor
public class SyncController {
    
    private final DeviceService deviceService;
    private final EmployeeService employeeService;
    private final ZKTecoService zkTecoService;
    
    @GetMapping
    public String showSyncPage(Model model) {
        List<Device> devices = deviceService.getAllDevices();
        List<Employee> employees = employeeService.getAllEmployees();
        
        model.addAttribute("devices", devices);
        model.addAttribute("employees", employees);
        model.addAttribute("pageTitle", "Manual Sync");
        return "sync/index";
    }
    
    @PostMapping("/employee-to-device")
    public String syncEmployeeToDevice(@RequestParam Long employeeId, 
                                       @RequestParam Long deviceId,
                                       RedirectAttributes redirectAttributes) {
        try {
            Employee employee = employeeService.getEmployeeById(employeeId)
                    .orElseThrow(() -> new RuntimeException("Employee not found"));
            Device device = deviceService.getDeviceById(deviceId)
                    .orElseThrow(() -> new RuntimeException("Device not found"));
            
            // Check if device is in PUSH mode
            if (device.getConnectionMode() != null && device.getConnectionMode().toString().equals("PUSH")) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                        "Cannot push employee to PUSH mode device '" + device.getDeviceName() + "'. " +
                        "Employee data must be configured directly on the device. " +
                        "To enable pushing employees, change the device mode to AUTO or PULL.");
                return "redirect:/sync";
            }
            
            boolean success = zkTecoService.pushEmployeeToDevice(device, employee);
            
            if (success) {
                redirectAttributes.addFlashAttribute("successMessage", 
                        "Employee " + employee.getName() + " synced to device " + device.getDeviceName());
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", 
                        "Failed to sync employee to device");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                    "Error syncing employee: " + e.getMessage());
        }
        return "redirect:/sync";
    }
    
    @PostMapping("/all-employees-to-device")
    public String syncAllEmployeesToDevice(@RequestParam Long deviceId,
                                           RedirectAttributes redirectAttributes) {
        try {
            Device device = deviceService.getDeviceById(deviceId)
                    .orElseThrow(() -> new RuntimeException("Device not found"));
            
            // Check if device is in PUSH mode
            if (device.getConnectionMode() != null && device.getConnectionMode().toString().equals("PUSH")) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                        "Cannot push employees to PUSH mode device '" + device.getDeviceName() + "'. " +
                        "Employee data must be configured directly on the device. " +
                        "To enable pushing employees, change the device mode to AUTO or PULL.");
                return "redirect:/sync";
            }
            
            List<Employee> employees = employeeService.getActiveEmployees();
            
            int successCount = 0;
            for (Employee employee : employees) {
                if (zkTecoService.pushEmployeeToDevice(device, employee)) {
                    successCount++;
                }
            }
            
            redirectAttributes.addFlashAttribute("successMessage", 
                    successCount + " employees synced to device " + device.getDeviceName());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                    "Error syncing employees: " + e.getMessage());
        }
        return "redirect:/sync";
    }
    
    @PostMapping("/attendance-from-device")
    public String syncAttendanceFromDevice(@RequestParam Long deviceId,
                                           RedirectAttributes redirectAttributes) {
        try {
            Device device = deviceService.getDeviceById(deviceId)
                    .orElseThrow(() -> new RuntimeException("Device not found"));
            
            // Check if device is in PUSH mode
            if (device.getConnectionMode() != null && device.getConnectionMode().toString().equals("PUSH")) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                        "Cannot sync from PUSH mode device '" + device.getDeviceName() + "'. " +
                        "This device automatically pushes data to the server in real-time. " +
                        "No manual sync is needed. To enable manual sync, change the device mode to AUTO or PULL.");
                return "redirect:/sync";
            }
            
            int recordCount = zkTecoService.syncAttendanceData(device).size();
            
            redirectAttributes.addFlashAttribute("successMessage", 
                    recordCount + " attendance records synced from device " + device.getDeviceName());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                    "Error syncing attendance: " + e.getMessage());
        }
        return "redirect:/sync";
    }
}
