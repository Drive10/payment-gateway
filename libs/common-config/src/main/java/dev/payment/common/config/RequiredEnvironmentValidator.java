package dev.payment.common.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class RequiredEnvironmentValidator implements EnvironmentPostProcessor {

    private static final List<String> REQUIRED_VARS = Arrays.asList(
        "JWT_SECRET",
        "DB_PASSWORD",
        "REDIS_PASSWORD"
    );

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        for (String var : REQUIRED_VARS) {
            String value = environment.getProperty(var);
            if (value == null || value.trim().isEmpty()) {
                throw new IllegalStateException("Required environment variable '" + var + "' is not set");
            }
        }
    }
}