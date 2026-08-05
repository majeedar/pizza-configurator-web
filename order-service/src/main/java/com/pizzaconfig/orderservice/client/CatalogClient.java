package com.pizzaconfig.orderservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Component
public class CatalogClient {

    private final RestClient restClient;

    public CatalogClient(RestClient.Builder builder, @Value("${clients.catalog-service.base-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public List<CatalogPizzaDto> findAll() {
        CatalogPizzaDto[] items = restClient.get()
                .uri("/v1/catalog/pizzas")
                .retrieve()
                .body(CatalogPizzaDto[].class);
        return items == null ? List.of() : Arrays.asList(items);
    }

    public record CatalogPizzaDto(String id, String name, String description, BigDecimal basePrice) {
    }
}
