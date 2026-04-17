package dev.payment.simulatorservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class SimulatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimulatorApplication.class, args);
    }
}
