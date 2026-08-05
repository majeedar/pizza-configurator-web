package com.pizzaconfig.orderservice.controller;

import com.pizzaconfig.orderservice.dto.CreatePendingReviewRequest;
import com.pizzaconfig.orderservice.dto.OrderResponse;
import com.pizzaconfig.orderservice.dto.PendingReviewResponse;
import com.pizzaconfig.orderservice.service.PendingReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

// Customer-facing side of the review workflow. Gateway exposes this under
// /v1/customer/orders/pending-reviews/** (same route/rewrite as the rest of orders).
@RestController
@RequestMapping("/v1/orders/pending-reviews")
public class PendingReviewController {

    private final PendingReviewService service;

    public PendingReviewController(PendingReviewService service) {
        this.service = service;
    }

    @PostMapping
    public PendingReviewResponse create(@RequestBody CreatePendingReviewRequest request,
                                         @RequestHeader(value = "X-Customer-Id", required = false) UUID customerId) {
        return PendingReviewResponse.from(service.create(request, customerId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PendingReviewResponse> get(@PathVariable UUID id) {
        return service.findById(id)
                .map(PendingReviewResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<OrderResponse> confirm(@PathVariable UUID id) {
        return service.confirm(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
