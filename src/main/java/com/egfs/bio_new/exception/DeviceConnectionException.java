package com.egfs.bio_new.exception;

/**
 * Exception thrown when unable to connect to a ZKTeco device
 */
public class DeviceConnectionException extends RuntimeException {
    
    public DeviceConnectionException(String message) {
        super(message);
    }
    
    public DeviceConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public DeviceConnectionException(Throwable cause) {
        super(cause);
    }
}
