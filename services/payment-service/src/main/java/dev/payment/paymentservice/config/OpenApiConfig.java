package dev.payment.paymentservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI paymentGatewayOpenApi() {
        final String securityScheme = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Fintech Payment Platform API")
                        .version("v1")
                        .description("Enterprise-grade payment, order, and authentication APIs for a gateway-ready fintech backend.")
                        .contact(new Contact().name("Platform Engineering").email("platform@fintech.local")))
                .addSecurityItem(new SecurityRequirement().addList(securityScheme))
                .components(new Components().addSecuritySchemes(
                        securityScheme,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
                ));
    }
}
