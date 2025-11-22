package com.egfs.bio_new.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "devices")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Device {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String deviceName;
    
    @Column(nullable = false)
    private String ipAddress;
    
    @Column(nullable = false)
    private Integer port;
    
    @Column
    private String location;
    
    @Column
    private String serialNumber;
    
    @Column
    private Boolean active = true;
    
    @Column
    private Boolean connected = false;
    
    @Column
    private LocalDateTime lastSyncTime;
    
    @Column
    private String connectionMode = "AUTO"; // AUTO, PULL, PUSH
    
    @Column
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
