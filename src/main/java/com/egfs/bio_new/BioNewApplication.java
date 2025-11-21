package com.egfs.bio_new;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BioNewApplication {

    public static void main(String[] args) {
        SpringApplication.run(BioNewApplication.class, args);
    }

}
