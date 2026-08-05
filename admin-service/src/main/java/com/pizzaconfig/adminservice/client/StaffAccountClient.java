package com.pizzaconfig.adminservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// REST/JSON for this internal call as a deliberate scope decision for clarity (see
// CLAUDE.md §7) — gRPC is the documented upgrade path for lower internal latency.
// admin-service doesn't store staff accounts itself; order-service (order_db) is the
// sole owner, so every call here is a pass-through, mirroring PricingServiceClient.
@Component
public class StaffAccountClient {

    private final RestClient restClient;

    public StaffAccountClient(RestClient.Builder builder, @Value("${clients.order-service.base-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public List<StaffAccountDto> findAll() {
        StaffAccountDto[] accounts = restClient.get()
                .uri("/v1/orders/staff-accounts")
                .retrieve()
                .body(StaffAccountDto[].class);
        return accounts == null ? List.of() : Arrays.asList(accounts);
    }

    public StaffAccountCreateResultDto create(String email, String fullName, String role) {
        return restClient.post()
                .uri("/v1/orders/staff-accounts")
                .body(new CreateRequest(email, fullName, role))
                .retrieve()
                .body(StaffAccountCreateResultDto.class);
    }

    public Optional<StaffAccountDto> update(UUID id, String fullName, String role) {
        try {
            StaffAccountDto result = restClient.put()
                    .uri("/v1/orders/staff-accounts/{id}", id)
                    .body(new UpdateRequest(fullName, role))
                    .retrieve()
                    .body(StaffAccountDto.class);
            return Optional.ofNullable(result);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }

    public boolean delete(UUID id) {
        try {
            restClient.delete().uri("/v1/orders/staff-accounts/{id}", id).retrieve().toBodilessEntity();
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        }
    }

    public record StaffAccountDto(UUID id, String email, String fullName, String role,
                                   boolean mustChangePassword, Instant createdAt) {
    }

    public record StaffAccountCreateResultDto(StaffAccountDto account, String temporaryPassword) {
    }

    public record CreateRequest(String email, String fullName, String role) {
    }

    public record UpdateRequest(String fullName, String role) {
    }
}
