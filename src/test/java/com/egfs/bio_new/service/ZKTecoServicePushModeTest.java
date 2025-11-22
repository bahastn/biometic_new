package com.egfs.bio_new.service;

import com.egfs.bio_new.entity.AttendanceLog;
import com.egfs.bio_new.entity.ConnectionMode;
import com.egfs.bio_new.entity.Device;
import com.egfs.bio_new.entity.Employee;
import com.egfs.bio_new.repository.AttendanceLogRepository;
import com.egfs.bio_new.repository.DeviceRepository;
import com.egfs.bio_new.repository.EmployeeRepository;
import com.egfs.bio_new.sdk.ZKTecoServerListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test class to verify that PUSH mode devices are handled correctly
 * and manual sync operations are prevented.
 */
@ExtendWith(MockitoExtension.class)
class ZKTecoServicePushModeTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private AttendanceLogRepository attendanceLogRepository;

    @Mock
    private ZKTecoServerListener serverListener;

    @InjectMocks
    private ZKTecoService zkTecoService;

    private Device pushModeDevice;
    private Device pullModeDevice;
    private Device autoModeDevice;
    private Employee testEmployee;

    @BeforeEach
    void setUp() {
        // Create a device configured in PUSH mode
        pushModeDevice = new Device();
        pushModeDevice.setId(1L);
        pushModeDevice.setDeviceName("TestDevice-PUSH");
        pushModeDevice.setIpAddress("192.168.1.100");
        pushModeDevice.setPort(4370);
        pushModeDevice.setConnectionMode(ConnectionMode.PUSH);
        pushModeDevice.setActive(true);
        pushModeDevice.setConnected(false);

        // Create a device configured in PULL mode
        pullModeDevice = new Device();
        pullModeDevice.setId(2L);
        pullModeDevice.setDeviceName("TestDevice-PULL");
        pullModeDevice.setIpAddress("192.168.1.101");
        pullModeDevice.setPort(4370);
        pullModeDevice.setConnectionMode(ConnectionMode.PULL);
        pullModeDevice.setActive(true);
        pullModeDevice.setConnected(false);

        // Create a device configured in AUTO mode
        autoModeDevice = new Device();
        autoModeDevice.setId(3L);
        autoModeDevice.setDeviceName("TestDevice-AUTO");
        autoModeDevice.setIpAddress("192.168.1.102");
        autoModeDevice.setPort(4370);
        autoModeDevice.setConnectionMode(ConnectionMode.AUTO);
        autoModeDevice.setActive(true);
        autoModeDevice.setConnected(false);

        // Create a test employee
        testEmployee = new Employee();
        testEmployee.setId(1L);
        testEmployee.setEmployeeId("123");
        testEmployee.setName("Test Employee");
        testEmployee.setActive(true);
    }

    @Test
    void testSyncAttendanceData_PushModeDevice_ShouldReturnEmptyList() {
        // When: Attempting to sync attendance data from a PUSH mode device
        List<AttendanceLog> result = zkTecoService.syncAttendanceData(pushModeDevice);

        // Then: Should return empty list without attempting connection
        assertNotNull(result);
        assertTrue(result.isEmpty(), "Should return empty list for PUSH mode device");

        // Verify no repository save was called (device state not modified)
        verify(deviceRepository, never()).save(any(Device.class));
    }

    @Test
    void testSyncAttendanceData_PullModeDevice_ShouldAttemptConnection() {
        // Note: This test verifies the method attempts to connect for PULL mode devices
        // Actual connection will fail without a real device, but that's expected
        
        // When: Attempting to sync attendance data from a PULL mode device
        List<AttendanceLog> result = zkTecoService.syncAttendanceData(pullModeDevice);

        // Then: Should attempt connection (will fail without real device)
        // The method should handle connection failure gracefully
        assertNotNull(result);
        // Connection will fail, but device state should be updated
        verify(deviceRepository, atLeastOnce()).save(pullModeDevice);
    }

    @Test
    void testSyncAttendanceData_AutoModeDevice_ShouldAttemptConnection() {
        // Note: AUTO mode devices should also attempt connection
        
        // When: Attempting to sync attendance data from an AUTO mode device
        List<AttendanceLog> result = zkTecoService.syncAttendanceData(autoModeDevice);

        // Then: Should attempt connection (will fail without real device)
        assertNotNull(result);
        verify(deviceRepository, atLeastOnce()).save(autoModeDevice);
    }

    @Test
    void testPushEmployeeToDevice_PushModeDevice_ShouldReturnFalse() {
        // When: Attempting to push employee to a PUSH mode device
        boolean result = zkTecoService.pushEmployeeToDevice(pushModeDevice, testEmployee);

        // Then: Should return false without attempting connection
        assertFalse(result, "Should return false for PUSH mode device");

        // Verify no repository save was called
        verify(deviceRepository, never()).save(any(Device.class));
    }

    @Test
    void testPushEmployeeToDevice_PullModeDevice_ShouldAttemptConnection() {
        // Note: This test verifies the method attempts to connect for PULL mode devices
        
        // When: Attempting to push employee to a PULL mode device
        boolean result = zkTecoService.pushEmployeeToDevice(pullModeDevice, testEmployee);

        // Then: Should attempt connection (will fail without real device)
        assertFalse(result, "Should return false when connection fails");
        verify(deviceRepository, atLeastOnce()).save(pullModeDevice);
    }

    @Test
    void testConnectionMode_NullMode_ShouldDefaultToAuto() {
        // Create a device with null connection mode
        Device deviceWithNullMode = new Device();
        deviceWithNullMode.setId(4L);
        deviceWithNullMode.setDeviceName("TestDevice-NULL");
        deviceWithNullMode.setIpAddress("192.168.1.103");
        deviceWithNullMode.setPort(4370);
        deviceWithNullMode.setConnectionMode(null);
        deviceWithNullMode.setActive(true);

        // When: Syncing with a device that has null connection mode
        List<AttendanceLog> result = zkTecoService.syncAttendanceData(deviceWithNullMode);

        // Then: Should not fail and should attempt connection (AUTO mode behavior)
        assertNotNull(result);
    }

    @Test
    void testConnectionMode_EnumValues() {
        // Verify that ConnectionMode enum has the expected values
        assertEquals("AUTO", ConnectionMode.AUTO.toString());
        assertEquals("PULL", ConnectionMode.PULL.toString());
        assertEquals("PUSH", ConnectionMode.PUSH.toString());
    }

    @Test
    void testConnectionMode_FromString() {
        // Test ConnectionMode.fromString method
        assertEquals(ConnectionMode.AUTO, ConnectionMode.fromString("AUTO"));
        assertEquals(ConnectionMode.PULL, ConnectionMode.fromString("PULL"));
        assertEquals(ConnectionMode.PUSH, ConnectionMode.fromString("PUSH"));
        assertEquals(ConnectionMode.AUTO, ConnectionMode.fromString(null));
        assertEquals(ConnectionMode.AUTO, ConnectionMode.fromString(""));
        assertEquals(ConnectionMode.AUTO, ConnectionMode.fromString("INVALID"));
    }
}
