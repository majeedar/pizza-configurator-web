package com.pizzaconfig.adminservice.service;

import com.pizzaconfig.adminservice.client.PricingServiceClient;
import com.pizzaconfig.adminservice.client.PricingServiceClient.PriceItemDto;
import com.pizzaconfig.adminservice.domain.AuditLogEntry;
import com.pizzaconfig.adminservice.repository.AuditLogRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class AdminPriceService {

    private static final String ACTION_TYPE = "PRICE_UPDATE";
    private static final String ACTION_TYPE_CREATE = "PRICE_CREATE";
    private static final String ACTION_TYPE_DELETE = "PRICE_DELETE";

    private final PricingServiceClient pricingServiceClient;
    private final AuditLogRepository auditLogRepository;

    public AdminPriceService(PricingServiceClient pricingServiceClient, AuditLogRepository auditLogRepository) {
        this.pricingServiceClient = pricingServiceClient;
        this.auditLogRepository = auditLogRepository;
    }

    public List<PriceItemDto> findAll() {
        return pricingServiceClient.findAll();
    }

    @Transactional
    public PriceItemDto create(String itemId, String itemType, BigDecimal amount) {
        PriceItemDto created;
        try {
            created = pricingServiceClient.create(itemId, itemType, amount);
        } catch (HttpClientErrorException.Conflict e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A price item with this id already exists.");
        }
        auditLogRepository.save(new AuditLogEntry(ACTION_TYPE_CREATE, itemId, null, amount.toString(), Instant.now()));
        return created;
    }

    @Transactional
    public boolean delete(String itemId) {
        boolean deleted = pricingServiceClient.delete(itemId);
        if (deleted) {
            auditLogRepository.save(new AuditLogEntry(ACTION_TYPE_DELETE, itemId, null, "deleted", Instant.now()));
        }
        return deleted;
    }

    @Transactional
    public Optional<PriceItemDto> updatePrice(String itemId, BigDecimal amount) {
        String previousAmount = pricingServiceClient.findAll().stream()
                .filter(item -> item.itemId().equals(itemId))
                .findFirst()
                .map(item -> item.amount().toString())
                .orElse(null);

        Optional<PriceItemDto> updated = pricingServiceClient.updatePrice(itemId, amount);

        updated.ifPresent(item -> auditLogRepository.save(new AuditLogEntry(
                ACTION_TYPE, itemId, previousAmount, amount.toString(), Instant.now())));

        return updated;
    }
}
