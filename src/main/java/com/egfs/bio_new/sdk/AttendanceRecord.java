package com.egfs.bio_new.sdk;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Data transfer object for attendance records from ZKTeco device
 */
@Data
public class AttendanceRecord {
    private int userId;
    private LocalDateTime punchTime;
    private int verifyType;  // 0=Password, 1=Fingerprint, 2=Card
    private int inOutState;  // 0=Check-in, 1=Check-out, 2=Break-out, 3=Break-in, 4=Overtime-in, 5=Overtime-out
    
    /**
     * Get human-readable verification method
     */
    public String getVerifyTypeString() {
        switch (verifyType) {
            case 0: return "Password";
            case 1: return "Fingerprint";
            case 2: return "Card";
            case 3: return "Face";
            default: return "Unknown";
        }
    }
    
    /**
     * Get human-readable in/out state
     */
    public String getInOutStateString() {
        switch (inOutState) {
            case 0: return "Check-in";
            case 1: return "Check-out";
            case 2: return "Break-out";
            case 3: return "Break-in";
            case 4: return "Overtime-in";
            case 5: return "Overtime-out";
            default: return "Unknown";
        }
    }
}
