package com.egfs.bio_new.repository;

import com.egfs.bio_new.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {
    
    Optional<Device> findByIpAddress(String ipAddress);
    
    List<Device> findByActive(Boolean active);
    
    Optional<Device> findByDeviceName(String deviceName);
}
