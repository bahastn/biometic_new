package com.egfs.bio_new.entity;

/**
 * Enum representing the connection mode for ZKTeco devices
 */
public enum ConnectionMode {
    /**
     * Automatically detect the device mode.
     * Tries pull mode first, then switches to push if connection is rejected.
     */
    AUTO,
    
    /**
     * Pull mode - Application connects to device.
     * The application initiates connections to pull data from the device.
     */
    PULL,
    
    /**
     * Push mode - Device connects to application.
     * The device initiates connections to push data to the application.
     */
    PUSH;
    
    /**
     * Get the default connection mode for new devices
     */
    public static ConnectionMode getDefault() {
        return AUTO;
    }
    
    /**
     * Parse a string to ConnectionMode, returning default if null or invalid
     */
    public static ConnectionMode fromString(String mode) {
        if (mode == null || mode.isEmpty()) {
            return getDefault();
        }
        try {
            return ConnectionMode.valueOf(mode.toUpperCase());
        } catch (IllegalArgumentException e) {
            return getDefault();
        }
    }
}
