package com.pizzaconfig.adminservice.controller;

import com.pizzaconfig.adminservice.client.PricingServiceClient.PriceItemDto;
import com.pizzaconfig.adminservice.client.RuleServiceClient.RuleThresholdDto;
import com.pizzaconfig.adminservice.service.AdminPriceService;
import com.pizzaconfig.adminservice.service.AdminRuleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/v1/admin")
public class AdminController {

    private final AdminPriceService priceService;
    private final AdminRuleService ruleService;

    public AdminController(AdminPriceService priceService, AdminRuleService ruleService) {
        this.priceService = priceService;
        this.ruleService = ruleService;
    }

    @GetMapping("/prices")
    public List<PriceItemDto> getPrices() {
        return priceService.findAll();
    }

    @PutMapping("/prices/{itemId}")
    public ResponseEntity<PriceItemDto> updatePrice(@PathVariable String itemId,
                                                     @RequestBody UpdatePriceRequest request) {
        return priceService.updatePrice(itemId, request.amount())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/prices")
    public ResponseEntity<PriceItemDto> createPrice(@RequestBody CreatePriceRequest request) {
        PriceItemDto created = priceService.create(request.itemId(), request.itemType(), request.amount());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/prices/{itemId}")
    public ResponseEntity<Void> deletePrice(@PathVariable String itemId) {
        return priceService.delete(itemId) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/rules")
    public List<RuleThresholdDto> getRules() {
        return ruleService.findAll();
    }

    @PutMapping("/rules/{ruleId}")
    public ResponseEntity<RuleThresholdDto> updateRule(@PathVariable String ruleId,
                                                         @RequestBody UpdateRuleRequest request) {
        return ruleService.updateRule(ruleId, request.value())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    public record UpdatePriceRequest(BigDecimal amount) {
    }

    public record CreatePriceRequest(String itemId, String itemType, BigDecimal amount) {
    }

    public record UpdateRuleRequest(String value) {
    }
}
