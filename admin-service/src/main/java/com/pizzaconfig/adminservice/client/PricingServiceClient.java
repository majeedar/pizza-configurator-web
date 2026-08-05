package com.pizzaconfig.adminservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

// REST/JSON for this internal call as a deliberate scope decision for clarity (see
// CLAUDE.md §7) — gRPC is the documented upgrade path for lower internal latency.
// admin-service no longer stores prices itself; pricing-service (price_db) is the
// sole owner, so every read/write here is a pass-through.
@Component
public class PricingServiceClient {

    private final RestClient restClient;

    public PricingServiceClient(RestClient.Builder builder,
                                 @Value("${clients.pricing-service.base-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public List<PriceItemDto> findAll() {
        PriceItemDto[] items = restClient.get()
                .uri("/v1/pricing/prices")
                .retrieve()
                .body(PriceItemDto[].class);
        return items == null ? List.of() : Arrays.asList(items);
    }

    public Optional<PriceItemDto> updatePrice(String itemId, BigDecimal amount) {
        try {
            PriceItemDto result = restClient.put()
                    .uri("/v1/pricing/prices/{itemId}", itemId)
                    .body(new UpdatePriceRequest(amount))
                    .retrieve()
                    .body(PriceItemDto.class);
            return Optional.ofNullable(result);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }

    public PriceItemDto create(String itemId, String itemType, BigDecimal amount) {
        return restClient.post()
                .uri("/v1/pricing/prices")
                .body(new CreatePriceRequest(itemId, itemType, amount))
                .retrieve()
                .body(PriceItemDto.class);
    }

    public boolean delete(String itemId) {
        try {
            restClient.delete().uri("/v1/pricing/prices/{itemId}", itemId).retrieve().toBodilessEntity();
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        }
    }

    public record PriceItemDto(String itemId, String itemType, BigDecimal amount) {
    }

    public record UpdatePriceRequest(BigDecimal amount) {
    }

    public record CreatePriceRequest(String itemId, String itemType, BigDecimal amount) {
    }
}
