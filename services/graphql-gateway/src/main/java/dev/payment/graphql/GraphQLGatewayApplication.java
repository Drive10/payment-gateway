package dev.payment.graphql;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GraphQLGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GraphQLGatewayApplication.class, args);
        System.out.println("===========================================");
        System.out.println("  GraphQL Gateway started!");
        System.out.println("  GraphiQL UI: http://localhost:8087/graphiql");
        System.out.println("  Endpoint: http://localhost:8087/graphql");
        System.out.println("===========================================");
    }
}
