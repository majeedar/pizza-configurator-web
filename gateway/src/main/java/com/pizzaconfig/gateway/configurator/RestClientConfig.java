package com.pizzaconfig.gateway.configurator;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

// Gateway is a pure WebFlux app (no spring-boot-starter-web), so Spring Boot doesn't
// auto-configure a RestClient.Builder the way it does for the servlet-based services —
// only WebClient.Builder gets that treatment in a reactive app. ConfiguratorController's
// two outbound calls are low-volume, so a blocking RestClient here is a deliberate
// simplification over a fully reactive WebClient chain; revisit if this endpoint ever
// needs to handle serious concurrent throughput.
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
