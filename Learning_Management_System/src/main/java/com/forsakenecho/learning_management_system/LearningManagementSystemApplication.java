package com.forsakenecho.learning_management_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class LearningManagementSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(LearningManagementSystemApplication.class, args);
    }

}
