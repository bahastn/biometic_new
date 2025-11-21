package com.egfs.bio_new.config;

import com.egfs.bio_new.sdk.ZKTecoServerListener;
import com.egfs.bio_new.service.ZKTecoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PreDestroy;

/**
 * Configuration for ZKTeco server listener (push mode)
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class ZKTecoServerConfig implements ApplicationRunner {
    
    private final ZKTecoServerListener serverListener;
    private final ZKTecoService zkTecoService;
    
    @Value("${zkteco.server.enabled:true}")
    private boolean serverEnabled;
    
    @Value("${zkteco.server.port:8086}")
    private int serverPort;
    
    @Override
    public void run(ApplicationArguments args) {
        if (serverEnabled) {
            try {
                serverListener.start(serverPort);
                log.info("ZKTeco push mode server started on port {}", serverPort);
                log.info("Devices can now push data to this server at {}:{}", getServerAddress(), serverPort);
            } catch (Exception e) {
                log.error("Failed to start ZKTeco server listener", e);
            }
        } else {
            log.info("ZKTeco push mode server is disabled");
        }
    }
    
    @PreDestroy
    public void shutdown() {
        if (serverListener.isRunning()) {
            serverListener.stop();
            log.info("ZKTeco server listener stopped");
        }
    }
    
    private String getServerAddress() {
        // Try to get the server address from properties
        try {
            return System.getProperty("server.address", "localhost");
        } catch (Exception e) {
            return "localhost";
        }
    }
}
