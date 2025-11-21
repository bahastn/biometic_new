package com.egfs.bio_new.service;

import com.egfs.bio_new.entity.Device;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceSyncScheduler {
    
    private final DeviceService deviceService;
    private final ZKTecoService zkTecoService;
    
    @Value("${zkteco.sync.enabled:true}")
    private boolean syncEnabled;
    
    /**
     * Scheduled task to sync attendance data from all active devices
     * Runs every minute (configurable via zkteco.sync.interval property)
     */
    @Scheduled(fixedDelayString = "${zkteco.sync.interval:60000}")
    public void syncAttendanceFromAllDevices() {
        if (!syncEnabled) {
            return;
        }
        
        log.debug("Starting scheduled attendance sync...");
        
        try {
            List<Device> activeDevices = deviceService.getActiveDevices();
            
            if (activeDevices.isEmpty()) {
                log.debug("No active devices found for sync");
                return;
            }
            
            for (Device device : activeDevices) {
                try {
                    log.info("Syncing attendance from device: {}", device.getDeviceName());
                    zkTecoService.syncAttendanceData(device);
                } catch (Exception e) {
                    log.error("Error syncing device {}: {}", device.getDeviceName(), e.getMessage());
                }
            }
            
            log.info("Completed scheduled attendance sync for {} devices", activeDevices.size());
            
        } catch (Exception e) {
            log.error("Error in scheduled attendance sync", e);
        }
    }
}
