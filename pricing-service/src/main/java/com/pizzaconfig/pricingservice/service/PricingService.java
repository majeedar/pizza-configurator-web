package com.pizzaconfig.pricingservice.service;

import com.pizzaconfig.pricingservice.domain.PriceItem;
import com.pizzaconfig.pricingservice.repository.PriceItemRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PricingService {

    private final PriceItemRepository repository;

    public PricingService(PriceItemRepository repository) {
        this.repository = repository;
    }

    public List<PriceItem> findAll() {
        return repository.findAll();
    }

    public BigDecimal calculateTotal(List<String> itemIds) {
        Map<String, PriceItem> byId = repository.findAll().stream()
                .collect(Collectors.toMap(PriceItem::getItemId, Function.identity()));

        return itemIds.stream()
                .map(byId::get)
                .filter(item -> item != null)
                .map(PriceItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public Optional<PriceItem> updatePrice(String itemId, BigDecimal amount) {
        return repository.findById(itemId).map(item -> {
            item.setAmount(amount);
            return item;
        });
    }

    @Transactional
    public PriceItem create(String itemId, String itemType, BigDecimal amount) {
        if (repository.existsById(itemId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A price item with this id already exists.");
        }
        return repository.save(new PriceItem(itemId, itemType, amount));
    }

    @Transactional
    public boolean delete(String itemId) {
        if (!repository.existsById(itemId)) {
            return false;
        }
        repository.deleteById(itemId);
        return true;
    }
}
