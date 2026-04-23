package dev.payment.paymentservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
@ComponentScan(basePackages = {
    "dev.payment.paymentservice.order",
    "dev.payment.paymentservice.payment"
})
@EntityScan(basePackages = {
    "dev.payment.paymentservice.order.entity",
    "dev.payment.paymentservice.payment.domain",
    "dev.payment.paymentservice.payment.domain.ledger"
})
@EnableJpaRepositories(basePackages = {
    "dev.payment.paymentservice.order.repository",
    "dev.payment.paymentservice.payment.repository"
})
public class PaymentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
