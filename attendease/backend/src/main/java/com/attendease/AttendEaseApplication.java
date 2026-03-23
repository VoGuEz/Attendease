package com.attendease;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AttendEaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(AttendEaseApplication.class, args);
    }

}