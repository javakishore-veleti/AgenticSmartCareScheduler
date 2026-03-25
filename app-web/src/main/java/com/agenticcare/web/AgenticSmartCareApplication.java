package com.agenticcare.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = "com.agenticcare")
@EntityScan(basePackages = "com.agenticcare")
@EnableJpaRepositories(basePackages = "com.agenticcare")
public class AgenticSmartCareApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgenticSmartCareApplication.class, args);
    }
}
