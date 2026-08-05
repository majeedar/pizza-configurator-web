package com.pizzaconfig.ruleservice.client;

import com.pizzaconfig.ruleservice.dto.CatalogPizza;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;

// REST/JSON is used for this internal call as a deliberate scope decision for clarity
// (see CLAUDE.md §7) — gRPC is the documented upgrade path for lower internal latency.
@Component
public class CatalogClient {

    private final RestClient restClient;

    public CatalogClient(RestClient.Builder builder,
                          @Value("${clients.catalog-service.base-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public List<CatalogPizza> findAll() {
        CatalogPizza[] pizzas = restClient.get()
                .uri("/v1/catalog/pizzas")
                .retrieve()
                .body(CatalogPizza[].class);
        return pizzas == null ? List.of() : Arrays.asList(pizzas);
    }
}
