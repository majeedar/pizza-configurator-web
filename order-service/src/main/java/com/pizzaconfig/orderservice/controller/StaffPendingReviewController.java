package com.pizzaconfig.orderservice.controller;

import com.pizzaconfig.orderservice.dto.PendingReviewResponse;
import com.pizzaconfig.orderservice.dto.ResolvePendingReviewRequest;
import com.pizzaconfig.orderservice.service.PendingReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

// Staff-facing side of the review workflow. Gateway exposes this under
// /v1/kitchen/pending-reviews/** (staff-auth, same convention as kitchen-service),
// even though it's physically served by order-service.
@RestController
@RequestMapping("/v1/orders/staff/pending-reviews")
public class StaffPendingReviewController {

    private final PendingReviewService service;

    public StaffPendingReviewController(PendingReviewService service) {
        this.service = service;
    }

    @GetMapping
    public List<PendingReviewResponse> listPending() {
        return service.findPending().stream().map(PendingReviewResponse::from).toList();
    }

    @PutMapping("/{id}/resolve")
    public ResponseEntity<?> resolve(@PathVariable UUID id, @RequestBody ResolvePendingReviewRequest request) {
        return service.resolve(id, request)
                .<ResponseEntity<?>>map(outcome -> outcome.approved()
                        ? ResponseEntity.ok(PendingReviewResponse.from(outcome.review()))
                        : ResponseEntity.unprocessableEntity().body(outcome.validation()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
