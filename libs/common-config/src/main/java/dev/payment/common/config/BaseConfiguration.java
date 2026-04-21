package dev.payment.common.config;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Base configuration class that can be extended by all PayFlow services
 * to reduce duplication in Spring Boot configuration.
 */
@SpringBootApplication
@ComponentScan(basePackages = {"dev.payment"})
public class BaseConfiguration {
    // This class serves as a marker for common configuration
    // Services can extend this to inherit common Spring Boot auto-configuration
}