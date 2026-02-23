package com.employmentvc.verifier;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class VerifierApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(VerifierApiApplication.class, args);
    }
}
