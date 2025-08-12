package dev.payment.apigateway;
import org.springframework.context.annotation.*; import org.springframework.web.reactive.function.server.*; import reactor.core.publisher.Mono;
@Configuration public class Routes {
  @Bean RouterFunction<ServerResponse> httpRoutes(){ return RouterFunctions.route().GET("/healthz", r-> ServerResponse.ok().body(Mono.just("ok"), String.class)).build(); }
}
