package com.pizzaconfig.pricingservice.controller;

import com.pizzaconfig.pricingservice.domain.PriceItem;
import com.pizzaconfig.pricingservice.service.PricingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/v1/pricing")
public class PricingController {

    private final PricingService pricingService;

    public PricingController(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    @GetMapping("/prices")
    public List<PriceItem> getPrices() {
        return pricingService.findAll();
    }

    @PutMapping("/prices/{itemId}")
    public ResponseEntity<PriceItem> updatePrice(@PathVariable String itemId, @RequestBody UpdatePriceRequest request) {
        return pricingService.updatePrice(itemId, request.amount())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/calculate")
    public BigDecimal calculate(@RequestParam List<String> itemIds) {
        return pricingService.calculateTotal(itemIds);
    }

    @PostMapping("/prices")
    public ResponseEntity<PriceItem> createPrice(@RequestBody CreatePriceRequest request) {
        PriceItem created = pricingService.create(request.itemId(), request.itemType(), request.amount());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/prices/{itemId}")
    public ResponseEntity<Void> deletePrice(@PathVariable String itemId) {
        return pricingService.delete(itemId) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    public record UpdatePriceRequest(BigDecimal amount) {
    }

    public record CreatePriceRequest(String itemId, String itemType, BigDecimal amount) {
    }
}
