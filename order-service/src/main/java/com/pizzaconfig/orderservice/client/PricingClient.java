package com.pizzaconfig.orderservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Component
public class PricingClient {

    private final RestClient restClient;

    public PricingClient(RestClient.Builder builder, @Value("${clients.pricing-service.base-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public List<PriceItemDto> findAllPrices() {
        PriceItemDto[] items = restClient.get()
                .uri("/v1/pricing/prices")
                .retrieve()
                .body(PriceItemDto[].class);
        return items == null ? List.of() : Arrays.asList(items);
    }

    public record PriceItemDto(String itemId, String itemType, BigDecimal amount) {
    }
}
