package com.egfs.bio_new.controller;

import com.egfs.bio_new.entity.Device;
import com.egfs.bio_new.service.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/devices")
@RequiredArgsConstructor
public class DeviceController {
    
    private final DeviceService deviceService;
    
    @GetMapping
    public String listDevices(Model model) {
        List<Device> devices = deviceService.getAllDevices();
        model.addAttribute("devices", devices);
        model.addAttribute("pageTitle", "Device Management");
        return "devices/list";
    }
    
    @GetMapping("/new")
    public String showNewDeviceForm(Model model) {
        model.addAttribute("device", new Device());
        model.addAttribute("pageTitle", "Add New Device");
        return "devices/form";
    }
    
    @PostMapping("/save")
    public String saveDevice(@ModelAttribute Device device, RedirectAttributes redirectAttributes) {
        try {
            deviceService.createDevice(device);
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Device added successfully: " + device.getDeviceName());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                    "Error adding device: " + e.getMessage());
        }
        return "redirect:/devices";
    }
    
    @GetMapping("/edit/{id}")
    public String showEditDeviceForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return deviceService.getDeviceById(id)
                .map(device -> {
                    model.addAttribute("device", device);
                    model.addAttribute("pageTitle", "Edit Device");
                    return "devices/form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Device not found");
                    return "redirect:/devices";
                });
    }
    
    @PostMapping("/update/{id}")
    public String updateDevice(@PathVariable Long id, @ModelAttribute Device device, 
                               RedirectAttributes redirectAttributes) {
        try {
            deviceService.updateDevice(id, device);
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Device updated successfully: " + device.getDeviceName());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                    "Error updating device: " + e.getMessage());
        }
        return "redirect:/devices";
    }
    
    @GetMapping("/delete/{id}")
    public String deleteDevice(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            deviceService.deleteDevice(id);
            redirectAttributes.addFlashAttribute("successMessage", "Device deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                    "Error deleting device: " + e.getMessage());
        }
        return "redirect:/devices";
    }
    
    @GetMapping("/connect/{id}")
    public String connectDevice(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        boolean connected = deviceService.connectDevice(id);
        if (connected) {
            redirectAttributes.addFlashAttribute("successMessage", 
                    "✓ Device connected successfully in PULL mode. Ready for data sync.");
        } else {
            redirectAttributes.addFlashAttribute("warningMessage", 
                    "⚠ Pull mode connection failed. Device is now registered for PUSH mode. " +
                    "Please configure the device to push data to this server (port 8086). " +
                    "See PUSH_MODE_GUIDE.md for instructions.");
        }
        return "redirect:/devices";
    }
    
    @GetMapping("/disconnect/{id}")
    public String disconnectDevice(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        deviceService.disconnectDevice(id);
        redirectAttributes.addFlashAttribute("successMessage", "Device disconnected successfully");
        return "redirect:/devices";
    }
}
