package com.pizzaconfig.ruleservice.client;

import com.pizzaconfig.ruleservice.dto.PriceItemDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;

// REST/JSON is used for this internal call as a deliberate scope decision for clarity
// (see CLAUDE.md §7) — gRPC is the documented upgrade path for lower internal latency.
@Component
public class PricingClient {

    private final RestClient restClient;

    public PricingClient(RestClient.Builder builder,
                          @Value("${clients.pricing-service.base-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public List<PriceItemDto> findAllPrices() {
        PriceItemDto[] prices = restClient.get()
                .uri("/v1/pricing/prices")
                .retrieve()
                .body(PriceItemDto[].class);
        return prices == null ? List.of() : Arrays.asList(prices);
    }
}
