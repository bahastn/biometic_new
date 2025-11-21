package com.egfs.bio_new.exception;

public class DeviceNotFoundException extends RuntimeException {
    
    public DeviceNotFoundException(String message) {
        super(message);
    }
    
    public DeviceNotFoundException(Long id) {
        super("Device not found with id: " + id);
    }
}
