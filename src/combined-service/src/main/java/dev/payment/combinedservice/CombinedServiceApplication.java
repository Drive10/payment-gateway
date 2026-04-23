package dev.payment.combinedservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan(basePackages = {
    "dev.payment.combinedservice.auth",
    "dev.payment.combinedservice.order",
    "dev.payment.combinedservice.payment"
})
public class CombinedServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CombinedServiceApplication.class, args);
    }
}