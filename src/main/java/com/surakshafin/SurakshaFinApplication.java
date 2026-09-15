package com.surakshafin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SuRakshaFin — unified fintech consumer platform.
 *
 * This service is a MODULAR MONOLITH: each package (identity, fraud, grievance,
 * budget, literacy) mirrors one bounded-context microservice from the target
 * architecture. Package boundaries are kept strict (no cross-package entity
 * reuse, communication only through service interfaces) so that any package
 * can be extracted into its own Spring Boot deployable later without a rewrite.
 */
@SpringBootApplication
public class SurakshaFinApplication {
    public static void main(String[] args) {
        SpringApplication.run(SurakshaFinApplication.class, args);
    }
}
