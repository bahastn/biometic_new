package com.egfs.bio_new.service;

import com.egfs.bio_new.entity.Device;
import com.egfs.bio_new.exception.DeviceNotFoundException;
import com.egfs.bio_new.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceService {
    
    private final DeviceRepository deviceRepository;
    private final ZKTecoService zkTecoService;
    
    public List<Device> getAllDevices() {
        return deviceRepository.findAll();
    }
    
    public List<Device> getActiveDevices() {
        return deviceRepository.findByActive(true);
    }
    
    public Optional<Device> getDeviceById(Long id) {
        return deviceRepository.findById(id);
    }
    
    public Optional<Device> getDeviceByIpAddress(String ipAddress) {
        return deviceRepository.findByIpAddress(ipAddress);
    }
    
    @Transactional
    public Device saveDevice(Device device) {
        log.info("Saving device: {}", device.getDeviceName());
        return deviceRepository.save(device);
    }
    
    @Transactional
    public Device createDevice(Device device) {
        log.info("Creating new device: {}", device.getDeviceName());
        // Test connection before saving
        boolean connected = zkTecoService.testConnection(device.getIpAddress(), device.getPort());
        device.setConnected(connected);
        return deviceRepository.save(device);
    }
    
    @Transactional
    public Device updateDevice(Long id, Device updatedDevice) {
        return deviceRepository.findById(id)
                .map(device -> {
                    device.setDeviceName(updatedDevice.getDeviceName());
                    device.setIpAddress(updatedDevice.getIpAddress());
                    device.setPort(updatedDevice.getPort());
                    device.setLocation(updatedDevice.getLocation());
                    device.setSerialNumber(updatedDevice.getSerialNumber());
                    device.setActive(updatedDevice.getActive());
                    log.info("Updated device: {}", device.getDeviceName());
                    return deviceRepository.save(device);
                })
                .orElseThrow(() -> new DeviceNotFoundException(id));
    }
    
    @Transactional
    public void deleteDevice(Long id) {
        deviceRepository.findById(id).ifPresent(device -> {
            if (device.getConnected()) {
                zkTecoService.disconnectDevice(device);
            }
            deviceRepository.deleteById(id);
            log.info("Deleted device: {}", device.getDeviceName());
        });
    }
    
    public boolean connectDevice(Long id) {
        return deviceRepository.findById(id)
                .map(zkTecoService::connectToDevice)
                .orElse(false);
    }
    
    public void disconnectDevice(Long id) {
        deviceRepository.findById(id).ifPresent(zkTecoService::disconnectDevice);
    }
}
